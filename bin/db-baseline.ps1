# ============================================================
# db-baseline.ps1 — 按基线清单把 sql/ 历史脚本导入全新库，并登记执行台账
# 适用：全新空库重建 / CI 冒烟库。存量库不要用本脚本（会 DROP 重建）。
# 用法：
#   .\db-baseline.ps1 -DryRun                       # 只打印执行计划与校验和，不连库
#   .\db-baseline.ps1 -DbPassword 123456            # 实际导入 yu-cjlujwc
#   .\db-baseline.ps1 -DbName yu_ci -Force          # 目标库存在时先 DROP（跳过二次确认）
#   .\db-baseline.ps1 -Resume                       # 中断后续跑：跳过台账中已成功的脚本
# 清单来源：db/legacy/manifest.txt（execute=Y 的条目，按序号升序执行）
# 台账表  ：db_legacy_baseline_log(order_no, script_path, sha256, ok, err, applied_at)
#           —— 用于回答「这台库到底导到了第几步」，Flyway 基线之上的对账依据。
# 已知阻塞：sql/ 历史脚本存在 64 处 sys_menu 撞号（见 bin/db-menu-check.ps1 输出），
#           重放会在第二条 INSERT 处主键冲突中断；先跑 db-menu-check.ps1 确认。
# ============================================================
param(
    [string]$DbHost     = "127.0.0.1",
    [int]   $DbPort     = 3306,
    [string]$DbUser     = "root",
    [string]$DbPassword = "123456",
    # 库名统一小写，避免迁移到 Linux（大小写敏感）后连不上库
    [string]$DbName     = "yu-cjlujwc",
    [string]$MysqlBin   = "",
    [string]$Root       = (Split-Path -Parent $PSScriptRoot),
    [switch]$DryRun,
    [switch]$Force,
    [switch]$Resume
)

$ErrorActionPreference = 'Continue'   # mysql 的密码告警走 stderr，Stop 会误判为终止错误

# ---------- 读清单 ----------
$manifestPath = Join-Path $Root 'db/legacy/manifest.txt'
if (-not (Test-Path $manifestPath)) { Write-Output "[错误] 清单不存在：$manifestPath"; exit 1 }

$plan = @()
foreach ($raw in Get-Content $manifestPath -Encoding UTF8) {
    $line = $raw.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { continue }
    $p = $line -split '\|'
    if ($p.Count -lt 3) { continue }
    if ($p[2].Trim().ToUpper() -ne 'Y') { continue }
    $rel = ($p[1].Trim() -replace '\\', '/')
    $full = Join-Path $Root $rel
    if (-not (Test-Path $full)) { Write-Output "[错误] 清单登记的脚本缺失：$rel"; exit 1 }
    $sha = (Get-FileHash -Path $full -Algorithm SHA256).Hash.ToLower()
    $plan += [pscustomobject]@{
        Order = $p[0].Trim(); Path = $rel; Full = (Resolve-Path $full).Path; Sha256 = $sha
    }
}
Write-Output "=== db-baseline：目标库 $DbName，计划执行 $($plan.Count) 个脚本 ==="

if ($DryRun) {
    foreach ($e in $plan) { Write-Output ("  {0,-6} {1,-46} {2}" -f $e.Order, $e.Path, $e.Sha256.Substring(0, 12)) }
    Write-Output "（-DryRun 结束，未连接数据库）"
    exit 0
}

# ---------- 定位 mysql 客户端 ----------
$mysql = $null
if (-not [string]::IsNullOrWhiteSpace($MysqlBin)) {
    foreach ($c in @("mysql.exe", "mysql")) {
        $p = Join-Path $MysqlBin $c
        if (Test-Path $p) { $mysql = $p; break }
    }
}
if (-not $mysql) {
    if (Get-Command "mysql" -ErrorAction SilentlyContinue) { $mysql = "mysql" }
    else { Write-Output "[错误] 未找到 mysql 客户端，请用 -MysqlBin 指定 bin 目录。"; exit 1 }
}
Write-Output "[0] mysql 客户端：$mysql"

$connArgs = @("--host=$DbHost", "--port=$DbPort", "--user=$DbUser", "--password=$DbPassword")

# ---------- 建库 ----------
$exists = & $mysql @connArgs "-N" "-e" "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='$DbName';" 2>$null
if ($LASTEXITCODE -ne 0) { Write-Output "[错误] 连接数据库失败，请检查 DbHost/DbUser/DbPassword 与 MySQL 服务。"; exit 1 }

if ($exists) {
    if ($Resume) {
        Write-Output "[1] 库已存在，-Resume 模式：保留现有数据，仅续跑未登记成功的脚本"
    } else {
        if (-not $Force) {
            $ans = Read-Host "目标库 '$DbName' 已存在，继续将 DROP 后重建（数据将被清空）。是否继续？(y/N)"
            if ($ans -notmatch '^[yY]$') { Write-Output "已取消。"; exit 0 }
        }
        Write-Output "[1] DROP 并重建库 $DbName ..."
        & $mysql @connArgs "-e" "DROP DATABASE ``$DbName``;" 2>$null
        if ($LASTEXITCODE -ne 0) { Write-Output "[错误] DROP 库失败。"; exit 1 }
        $exists = $null
    }
}
if (-not $exists) {
    Write-Output "[1] 创建库 $DbName (utf8mb4) ..."
    & $mysql @connArgs "-e" "CREATE DATABASE IF NOT EXISTS ``$DbName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;" 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Output "[错误] 建库失败。"; exit 1 }
}

# ---------- 台账表 ----------
$logDdl = @"
CREATE TABLE IF NOT EXISTS db_legacy_baseline_log (
  order_no    varchar(16)  NOT NULL COMMENT '基线序号，对应 db/legacy/manifest.txt',
  script_path varchar(255) NOT NULL COMMENT '脚本相对仓库根路径',
  sha256      char(64)     NOT NULL COMMENT '脚本内容校验和，用于判断是否已执行过同一版本',
  ok          tinyint      NOT NULL DEFAULT 0,
  err         varchar(500) DEFAULT NULL,
  applied_at  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史脚本基线执行台账（V4.0 数据§7.3/K1）';
"@
& $mysql @connArgs $DbName "-e" $logDdl 2>$null
if ($LASTEXITCODE -ne 0) { Write-Output "[错误] 创建台账表失败。"; exit 1 }

$done = @{}
if ($Resume) {
    & $mysql @connArgs $DbName "-N" "-e" "SELECT order_no, sha256 FROM db_legacy_baseline_log WHERE ok=1;" 2>$null |
        ForEach-Object { $parts = $_ -split "`t"; if ($parts.Count -eq 2) { $done[$parts[0]] = $parts[1] } }
    Write-Output "[2] -Resume：台账中已成功 $($done.Count) 项，将跳过"
}

# ---------- 逐个导入 ----------
function Invoke-SqlFile($file, $dbName, $extraArgs) {
    # 用 .NET Process 把文件字节流写入 mysql stdin，绕开命令行引号与编码转换问题
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $mysql
    $argsList = @("--host=$DbHost", "--port=$DbPort", "--user=$DbUser", "--password=$DbPassword",
                  "--default-character-set=utf8mb4", "--max_allowed_packet=512M") + $extraArgs + @($dbName)
    $psi.Arguments = (($argsList | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }) -join ' ')
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $proc = [System.Diagnostics.Process]::Start($psi)
    $fs = [System.IO.File]::OpenRead($file)
    try { $fs.CopyTo($proc.StandardInput.BaseStream); $proc.StandardInput.BaseStream.Flush() }
    finally { $fs.Dispose(); $proc.StandardInput.Close() }
    $err = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()
    return @{ Code = $proc.ExitCode; Err = $err }
}

$idx = 0; $okCount = 0; $skipCount = 0; $failed = @()
foreach ($e in $plan) {
    $idx++
    if ($Resume -and $done.ContainsKey($e.Order) -and $done[$e.Order] -eq $e.Sha256) {
        $skipCount++
        Write-Output ("[{0,2}/{1}] 跳过（台账已登记）{2}" -f $idx, $plan.Count, $e.Path)
        continue
    }
    Write-Output ("[{0,2}/{1}] {2}  <- {3}" -f $idx, $plan.Count, $e.Order, $e.Path)
    $r = Invoke-SqlFile -File $e.Full -DbName $DbName -ExtraArgs @("--init-command=SET FOREIGN_KEY_CHECKS=0;")
    $okFlag = if ($r.Code -eq 0) { 1 } else { 0 }
    $errTxt = if ($r.Code -eq 0) { "" } else { ($r.Err -replace '[\r\n]+', ' | ') }
    if ($errTxt.Length -gt 480) { $errTxt = $errTxt.Substring(0, 480) }
    $escErr = $errTxt -replace "'", "''"
    $logSql = "INSERT INTO db_legacy_baseline_log(order_no,script_path,sha256,ok,err,applied_at) VALUES('$($e.Order)','$($e.Path)','$($e.Sha256)',$okFlag,'$escErr',NOW()) ON DUPLICATE KEY UPDATE sha256=VALUES(sha256), ok=VALUES(ok), err=VALUES(err), applied_at=NOW();"
    & $mysql @connArgs $DbName "-e" $logSql 2>$null
    if ($r.Code -ne 0) {
        $failed += [pscustomobject]@{ Order = $e.Order; Path = $e.Path; Err = $errTxt }
        Write-Output "     [失败] $errTxt"
        Write-Output "     基线重放中断：后续脚本依赖前置结构，不再继续。修复后加 -Resume 续跑。"
        break
    }
    $okCount++
}

# ---------- 汇总 ----------
$tableCount = & $mysql @connArgs $DbName "-N" "-e" "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='$DbName';" 2>$null
Write-Output ""
Write-Output "执行完成：成功 $okCount，跳过 $skipCount，失败 $($failed.Count)，目标库表数量 $tableCount（参考值 163+）"
if ($failed.Count -gt 0) {
    Write-Output "首个失败点：$($failed[0].Order) $($failed[0].Path)"
    Write-Output "提示：sys_menu 撞号类失败请用 bin/db-menu-check.ps1 定位，改派 3000+ 号段后 -Resume 续跑。"
    exit 1
}
Write-Output "下一步：以 FLYWAY_ENABLED=true 启动后端，Flyway 将以版本 0 打基线并执行 db/migration/V1 之后的变更。"
exit 0
