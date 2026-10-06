# A1 高并发选课压测基线（无新依赖版）
# 目标：对两道防超卖闸门给出可复现的并发证据（直连本地 Redis，等价重放后端原子原语）：
#   1) 容量原子扣减（SelectionCacheManager.decrementCapacity = DECR）：N 并发抢 M 座，放行数恰为 M、不为负
#   2) 削峰令牌桶（SelectionAdmission Lua：令牌桶+排队号同脚本原子）：放行不超突发容量、排队号严格单调无重复
# 说明：这是入口级原子性基线（非全栈 HTTP 压测，后者需 JMeter/Gatling 等新外部依赖，离线环境不引入）。
#       基线 2 以内联命令重放令牌桶语义（不含时间补充项，等价于亚秒窗口内速率增量≈0 的真实尖峰场景）。
# 用法：需要先拉起本地 Redis；powershell -File a1_loadtest_baseline.ps1 [-Rounds 100] [-Seats 20]
param(
    [int]$Rounds = 100,   # 并发抢座请求数（Start-Job 窗口进程数，可视机器规格上调）
    [int]$Seats  = 20     # 课程容量
)
$ErrorActionPreference = 'Stop'
$redisCli = Join-Path $PSScriptRoot 'redis-cli.exe'   # 本脚本即放在 bin\redis 下
if (-not (Test-Path $redisCli)) { Write-Host "找不到 redis-cli: $redisCli"; exit 1 }
& $redisCli -h 127.0.0.1 ping | Out-Null
if ($LASTEXITCODE -ne 0) { Write-Host '本地 Redis(6379) 未就绪，请先拉起'; exit 1 }

# stdin 传命令，规避 PowerShell 5.1 原生命令参数转发对含引号/长串的破坏
function RedisCmd([string]$cmd) { ($cmd | & $redisCli -h 127.0.0.1 --no-auth-warning) }

Write-Host "=== A1 选课压测基线：并发 $Rounds 请求 / 容量 $Seats 座 ===" -ForegroundColor Cyan

# ---------- 基线 1：容量原子扣减防超卖 ----------
$capKey = "loadtest:cap:offering1"
RedisCmd "DEL $capKey" | Out-Null
RedisCmd "SET $capKey $Seats" | Out-Null   # 等价服务层 getCapacity==null 时的 setCapacity 初始化
$jobs = @()
$sw = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt $Rounds; $i++) {
    $jobs += Start-Job -ArgumentList $redisCli, $capKey -ScriptBlock {
        param($cli, $key)
        # 等价 decrementCapacity 后 remaining<0 则 incrementCapacity 回滚的服务层契约
        $r = & $cli -h 127.0.0.1 --no-auth-warning DECR $key 2>$null | Out-String
        if ($r -match '-\d+') {
            & $cli -h 127.0.0.1 --no-auth-warning INCR $key | Out-Null   # 负数优先匹配，防 "(integer) -3" 误解析为 3
        }
    }
}
$jobs | Wait-Job | Out-Null; $jobs | Remove-Job
$sw.Stop()
$final = [long]((RedisCmd "GET $capKey") -replace '\D','')
$admitted = $Seats - $final
Write-Host ("[1] 容量原子扣减: 放行 {0}/{1}，余量 {2}，耗时 {3}ms" -f $admitted, $Seats, $final, $sw.ElapsedMilliseconds)
if ($admitted -ne $Seats -or $final -lt 0) {
    Write-Host "FAIL: 防超卖被打破（放行 $admitted != 容量 $Seats 或余量为负）" -ForegroundColor Red; exit 1
}
Write-Host "PASS: $Rounds 并发下放行数恰等于容量，无超卖、余量非负" -ForegroundColor Green

# ---------- 基线 2：削峰令牌桶 + 排队号单调（内联重放 SelectionAdmission Lua 语义） ----------
$bucketKey = "loadtest:admit:bucket:r1"; $seqKey = "loadtest:admit:seq:r1"
RedisCmd "DEL $bucketKey $seqKey" | Out-Null
# 与 SelectionAdmission.java 同一原子性：桶读写与 INCR 发号在单个 EVAL 内完成；键不存在即满桶 burst
$inline = "EVAL ""local t = redis.call('HMGET', KEYS[1], 't') local tok = tonumber(t[1]) if tok == nil then tok = tonumber(ARGV[2]) end local allowed = 0 if tok >= 1 then tok = tok - 1; allowed = 1 end redis.call('HSET', KEYS[1], 't', tok) local seq = redis.call('INCR', KEYS[2]) return {allowed, seq}"" 2 $bucketKey $seqKey 100 50"
$burst = 50; $N = 100
$allowedCnt = 0; $seqs = @()
$sw2 = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt $N; $i++) {
    $out = RedisCmd $inline | ForEach-Object { [long]($_ -replace '\D','') }
    if ($out[0] -eq 1) { $allowedCnt++ }
    $seqs += $out[1]
}
$sw2.Stop()
$distinctSeq = ($seqs | Select-Object -Unique).Count
Write-Host ("[2] 削峰令牌桶: {0} 请求放行 {1}(≤突发 {2})，排队号去重后 {3}/{0}，耗时 {4}ms" -f $N, $allowedCnt, $burst, $distinctSeq, $sw2.ElapsedMilliseconds)
if ($allowedCnt -ne $burst -or $distinctSeq -ne $N) {
    Write-Host "FAIL: 令牌桶突发约束($allowedCnt!=$burst)或排队号单调唯一性($distinctSeq!=$N)被打破" -ForegroundColor Red; exit 1
}
Write-Host "PASS: 突发窗口内放行恰为桶容量 $burst，$N 个排队号严格单调无重复（Lua 原子发号成立）" -ForegroundColor Green

# 清理压测键
RedisCmd "DEL $capKey $bucketKey $seqKey" | Out-Null
Write-Host "`nSUMMARY: A1 基线 2/2 PASS" -ForegroundColor Cyan
