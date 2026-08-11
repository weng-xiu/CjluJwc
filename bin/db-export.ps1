# ============================================================
# db-export.ps1 — 全量导出 yu-CjluJwc 数据库（含结构与数据）
# 适用：当前开发机执行，生成单一 SQL 文件用于迁移。
# 用法：
#   .\db-export.ps1                       # 使用默认值（localhost / root / 123456）
#   .\db-export.ps1 -DbHost 127.0.0.1 -DbUser root -DbPassword 123456
#   .\db-export.ps1 -OutFile D:\backup\yu-CjluJwc.sql
# 注意：库名含连字符，脚本内部已用反引号 `yu-CjluJwc` 包裹。
# ============================================================
param(
    [string]$DbHost    = "localhost",
    [int]   $DbPort    = 3306,
    [string]$DbUser    = "root",
    [string]$DbPassword = "123456",
    # 注意：本机 MySQL 中实际库名为全小写 yu-cjlujwc
    # （Windows 下 lower_case_table_names=1，大小写不敏感；迁到 Linux 时务必用小写建库）
    [string]$DbName    = "yu-cjlujwc",
    [string]$OutFile   = "",
    # mysqldump 所在目录，留空则依赖 PATH。
    # 本机 MySQL 为 phpstudy 绿色版（未加入 PATH、未注册服务），故给出默认路径。
    [string]$MysqlBin  = "F:\phpstudy_pro\Extensions\MySQL8.0.12\bin"
)

$ErrorActionPreference = 'Stop'

# 若未指定输出路径，默认导出到脚本同级目录的 db-backup 子目录，按时间戳命名
if ([string]::IsNullOrWhiteSpace($OutFile)) {
    $backupDir = Join-Path $PSScriptRoot "db-backup"
    if (-not (Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir | Out-Null }
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutFile = Join-Path $backupDir ("{0}-{1}.sql" -f $DbName, $stamp)
}

# 定位 mysqldump：优先用 -MysqlBin 指定目录，找不到则回退到 PATH
$mysqldump = $null
if (-not [string]::IsNullOrWhiteSpace($MysqlBin)) {
    foreach ($candidate in @("mysqldump.exe", "mysqldump")) {
        $p = Join-Path $MysqlBin $candidate
        if (Test-Path $p) { $mysqldump = $p; break }
    }
    if (-not $mysqldump) {
        Write-Output "[提示] 指定目录下未找到 mysqldump：$MysqlBin，尝试从 PATH 查找。"
    }
}
if (-not $mysqldump) {
    if (Get-Command "mysqldump" -ErrorAction SilentlyContinue) {
        $mysqldump = "mysqldump"
    } else {
        Write-Output "[错误] 未找到 mysqldump。请用 -MysqlBin 指定 MySQL 的 bin 目录，例如："
        Write-Output "        .\db-export.ps1 -MysqlBin 'D:\MySQL\bin'"
        exit 1
    }
}
Write-Output "[0/3] 使用 mysqldump：$mysqldump"

Write-Output "[1/3] 使用 mysqldump 从 ${DbHost}:${DbPort} 导出库 ${DbName} ..."
# --set-gtid-purged=OFF : 避免目标库开启 GTID 时导入报错
# --default-character-set=utf8mb4 : 保持中文正常
# --routines/--triggers : 本库无 routines，但保留以通用
# --single-transaction : 不锁表热备（InnoDB）
# --hex-blob : 二进制字段安全导出
$argList = @(
    "--host=$DbHost",
    "--port=$DbPort",
    "--user=$DbUser",
    "--password=$DbPassword",
    "--default-character-set=utf8mb4",
    "--single-transaction",
    "--routines",
    "--triggers",
    "--events",
    "--hex-blob",
    "--set-gtid-purged=OFF",
    "--skip-lock-tables",
    "--result-file=$OutFile",
    $DbName
)

# 注意：mysqldump 会把 "Using a password on the command line interface can be insecure"
# 这类警告写入 stderr。在 $ErrorActionPreference='Stop' 下，PowerShell 会把原生命令的
# stderr 输出当成终止性错误，导致导出明明成功却被判为失败。
# 因此这里临时放宽 ErrorActionPreference，并以 $LASTEXITCODE 作为唯一成功判据。
$prevEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
$stderrText = & $mysqldump @argList 2>&1 | Out-String
$exitCode = $LASTEXITCODE
$ErrorActionPreference = $prevEap

if ($exitCode -ne 0) {
    Write-Output "[错误] 导出失败，mysqldump 退出码: $exitCode"
    if (-not [string]::IsNullOrWhiteSpace($stderrText)) { Write-Output $stderrText }
    exit 1
}

# mysqldump 成功时仍会向 stderr 写入密码告警，属正常噪音，不予展示。
# 仅当退出码非 0（上面已处理）才视为失败，这里保持静默。

# 等待文件落盘
Start-Sleep -Seconds 1
if (-not (Test-Path $OutFile)) { Write-Output "[错误] 未生成导出文件：$OutFile"; exit 1 }

$sizeMB = [math]::Round((Get-Item $OutFile).Length / 1MB, 2)
Write-Output "[2/3] 导出完成："
Write-Output "       文件：$OutFile"
Write-Output "       大小：${sizeMB} MB"

# 校验完整性：头部 mysqldump 标记 + 尾部 "Dump completed" + 表数量
$hasHeader    = @(Select-String -Path $OutFile -Pattern 'MySQL dump' -Encoding utf8 -List).Count -gt 0
$hasCompleted = @(Select-String -Path $OutFile -Pattern 'Dump completed' -Encoding utf8 -List).Count -gt 0
$tableCount   = @(Select-String -Path $OutFile -Pattern '^CREATE TABLE' -Encoding utf8).Count

$headerText = '缺失'
if ($hasHeader) { $headerText = '通过' }
$completedText = '缺失（导出可能被中断）'
if ($hasCompleted) { $completedText = '通过' }

Write-Output "[3/3] 完整性校验："
Write-Output "       头部标记：$headerText"
Write-Output "       结束标记：$completedText"
Write-Output "       建表语句：$tableCount 张表"

if (-not $hasCompleted) {
    Write-Output "[警告] 未发现 'Dump completed' 标记，导出可能不完整，请勿用于迁移。"
    exit 1
}

Write-Output ""
Write-Output "下一步：将本文件拷贝到新环境，运行 bin/db-import.ps1 完成导入。"
