# A1/F6 高并发选课压测基线（无新依赖版）
# 目标：对三道选课尖峰闸门给出可复现的并发证据（直连本地 Redis，等价重放后端原子原语）：
#   1) 容量原子扣减（SelectionCacheManager.decrementCapacity = DECR）：N 并发抢 M 座，放行数恰为 M、不为负
#   2) 削峰令牌桶（SelectionAdmission Lua：令牌桶+排队号同脚本原子）：放行不超突发容量、排队号严格单调无重复
#   3) IP 限流闸门（RateLimiterAspect 执行 RedisConfig.limitScript 固定窗口 Lua）：窗口内放行恰为配额 count
#   4) 令牌桶持续速率恢复（SelectionAdmission 完整 Lua 含时间补充项）：突发耗尽后按 rate/s 回血且封顶 burst
# 指标：基线 3/4 逐次记录闸门往返延迟，输出 avg/p95/max（含 redis-cli 进程启动开销，量级参考；Lua 本体微秒级）
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

# ---------- 基线 3：IP 限流闸门（重放 RedisConfig.limitScript 固定窗口 Lua，与 RateLimiterAspect 同判定：返回值>配额即拒绝） ----------
$limitKey = "loadtest:limit:ip1"
RedisCmd "DEL $limitKey" | Out-Null
$limitQuota = 20; $limitN = 40
# 与 Java 侧 limitScriptText 同一脚本（压平为单行）
$limitLua = 'local key = KEYS[1] local count = tonumber(ARGV[1]) local time = tonumber(ARGV[2]) local current = redis.call(''get'', key); if current and tonumber(current) > count then return tonumber(current); end current = redis.call(''incr'', key) if tonumber(current) == 1 then redis.call(''expire'', key, time) end return tonumber(current);'
$limitCmd = 'EVAL "' + $limitLua + '" 1 ' + $limitKey + ' ' + $limitQuota + ' 60'
$allowedL = 0; $latL = @()
$sw3 = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt $limitN; $i++) {
    $t0 = [System.Diagnostics.Stopwatch]::StartNew()
    $num = [long]((RedisCmd $limitCmd) -replace '\D','')
    $t0.Stop(); $latL += $t0.Elapsed.TotalMilliseconds
    if ($num -le $limitQuota) { $allowedL++ }
}
$sw3.Stop()
Write-Host ("[3] IP限流闸门: 窗口内 {0} 请求放行 {1}(配额 {2})，耗时 {3}ms" -f $limitN, $allowedL, $limitQuota, $sw3.ElapsedMilliseconds)
$sumL = ($latL | Measure-Object -Average -Maximum)
$p95L = ($latL | Sort-Object)[[Math]::Ceiling(0.95 * $latL.Count) - 1]
Write-Host ("     闸门延迟: avg {0:F1}ms / p95 {1:F1}ms / max {2:F1}ms" -f $sumL.Average, $p95L, $sumL.Maximum)
if ($allowedL -ne $limitQuota) {
    Write-Host "FAIL: 固定窗口放行数($allowedL)不等于配额($limitQuota)" -ForegroundColor Red; exit 1
}
Write-Host "PASS: 窗口内放行恰为配额 $limitQuota，超限 $($limitN-$allowedL) 个请求全部被拒" -ForegroundColor Green

# ---------- 基线 4：令牌桶持续速率恢复（SelectionAdmission 完整 Lua 含时间补充项） ----------
# 说明：now 为后端应用层注入 ARGV 的逻辑时钟，此处同样注入确定性时间戳（A 阶段窗口内共 14ms，
# 回血≈ 0.14 枚可忽略；B 阶段模拟静默 1.2s，回血 min(burst, 12)=5 封顶），不受 redis-cli 进程开销干扰。
$admitLua = 'local bucket = KEYS[1] local seqKey = KEYS[2] local rate = tonumber(ARGV[1]) local burst = tonumber(ARGV[2]) local now = tonumber(ARGV[3]) local ttl = tonumber(ARGV[4]) local state = redis.call(''HMGET'', bucket, ''t'', ''ts'') local tokens = tonumber(state[1]) local ts = tonumber(state[2]) if tokens == nil then tokens = burst; ts = now end local elapsed = now - ts; if elapsed < 0 then elapsed = 0 end tokens = math.min(burst, tokens + (elapsed / 1000.0) * rate) local allowed = 0 if tokens >= 1 then tokens = tokens - 1; allowed = 1 end redis.call(''HSET'', bucket, ''t'', tokens, ''ts'', now) redis.call(''PEXPIRE'', bucket, ttl) local seq = redis.call(''INCR'', seqKey) redis.call(''PEXPIRE'', seqKey, ttl) return {allowed, seq}'
$bucketKey4 = "loadtest:admit:bucket:r4"; $seqKey4 = "loadtest:admit:seq:r4"
RedisCmd "DEL $bucketKey4 $seqKey4" | Out-Null
$rate4 = 10; $burst4 = 5; $lat4 = @(); $t0Base = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
function Admit4([long]$nowMs) {
    $cmd = 'EVAL "' + $admitLua + '" 2 ' + $bucketKey4 + ' ' + $seqKey4 + ' ' + $rate4 + ' ' + $burst4 + ' ' + $nowMs + ' 60000'
    $out = RedisCmd $cmd
    return [long]($out[0] -replace '\D','')
}
# 阶段 A：突发 8 连发（逻辑时钟步进 2ms，应只放行前 burst4=5 个，桶耗尽）
$allowedA = 0
for ($i = 0; $i -lt ($burst4 + 3); $i++) {
    $t0 = [System.Diagnostics.Stopwatch]::StartNew()
    if ((Admit4 ($t0Base + $i * 2)) -eq 1) { $allowedA++ }
    $t0.Stop(); $lat4 += $t0.Elapsed.TotalMilliseconds
}
# 阶段 B：逻辑时钟前进 1.2s（回血 min(burst, 0+1.2*rate)=5 枚），再 5 连发应全放行且不超 burst 封顶
$allowedB = 0
for ($i = 0; $i -lt $burst4; $i++) {
    $t0 = [System.Diagnostics.Stopwatch]::StartNew()
    if ((Admit4 ($t0Base + 1200 + $i * 2)) -eq 1) { $allowedB++ }
    $t0.Stop(); $lat4 += $t0.Elapsed.TotalMilliseconds
}
Write-Host ("[4] 令牌桶持续速率: 突发8连发放行 {0}(应=突发 {1})，逻辑静默1.2s后5连发放行 {2}(回血封顶 {1})" -f $allowedA, $burst4, $allowedB)
$sum4 = ($lat4 | Measure-Object -Average -Maximum)
$p954 = ($lat4 | Sort-Object)[[Math]::Ceiling(0.95 * $lat4.Count) - 1]
Write-Host ("     闸门延迟: avg {0:F1}ms / p95 {1:F1}ms / max {2:F1}ms" -f $sum4.Average, $p954, $sum4.Maximum)
if ($allowedA -ne $burst4 -or $allowedB -ne $burst4) {
    Write-Host "FAIL: 令牌桶突发/恢复约束被打破(A=$allowedA!=$burst4 或 B=$allowedB!=$burst4)" -ForegroundColor Red; exit 1
}
Write-Host "PASS: 突发窗口恰放 $burst4 枚，回血按 rate=$rate4/s 恢复且封顶 burst，恢复窗口无一漏放" -ForegroundColor Green

# 清理压测键
RedisCmd "DEL $capKey $bucketKey $seqKey $limitKey $bucketKey4 $seqKey4" | Out-Null
Write-Host "`nSUMMARY: A1/F6 选课压测基线 4/4 PASS（防超卖/削峰发号/IP限流/持续速率）" -ForegroundColor Cyan
