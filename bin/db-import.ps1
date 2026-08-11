# ============================================================
# db-import.ps1 — 在新环境建库并导入 yu-CjluJwc 全量 SQL
# 适用：目标（新）机器执行。需先准备好由 db-export.ps1 生成的 .sql 文件。
# 用法：
#   .\db-import.ps1 -SqlFile D:\backup\yu-CjluJwc-20260806-120000.sql
#   .\db-import.ps1 -SqlFile .\db-backup\yu-CjluJwc.sql -DbHost 127.0.0.1 -DbUser root -DbPassword 123456
# 注意：
#   - 库名含连字符，脚本内部用反引号 `yu-CjluJwc` 包裹。
#   - 导入前会创建空库（utf8mb4）。若库已存在，先 DROP 再建，确保干净迁移（会二次确认）。
# ============================================================
param(
    [Parameter(Mandatory=$true)][string]$SqlFile,
    [string]$DbHost     = "127.0.0.1",
    [int]   $DbPort     = 3306,
    [string]$DbUser     = "root",
    [string]$DbPassword = "123456",
    # 注意：库名统一使用全小写，避免迁移到 Linux（大小写敏感）后连不上库
    [string]$DbName     = "yu-cjlujwc",
    # mysql 客户端目录，留空则从 PATH 查找。
    # 若新机为 phpstudy 等绿色版（未注册服务/未加 PATH），需显式指定，例如：
    #   -MysqlBin 'F:\phpstudy_pro\Extensions\MySQL8.0.12\bin'
    [string]$MysqlBin   = "",
    [switch]$Force       # 跳过“库已存在将 DROP”的二次确认
)

# 说明：mysql/mysqldump 会把密码告警写入 stderr。若设为 'Stop'，
# PowerShell 会把原生命令的 stderr 当成终止性错误，导致操作明明成功却中断。
# 因此全程使用 'Continue'，并以 $LASTEXITCODE 作为唯一成功判据。
$ErrorActionPreference = 'Continue'

if (-not (Test-Path $SqlFile)) { Write-Output "[错误] SQL 文件不存在：$SqlFile"; exit 1 }
# 统一为绝对路径，避免相对路径在重定向时解析异常
$SqlFile = (Resolve-Path $SqlFile).Path

# 定位 mysql 客户端：优先 -MysqlBin，找不到则回退 PATH
$mysql = $null
if (-not [string]::IsNullOrWhiteSpace($MysqlBin)) {
    foreach ($candidate in @("mysql.exe", "mysql")) {
        $p = Join-Path $MysqlBin $candidate
        if (Test-Path $p) { $mysql = $p; break }
    }
    if (-not $mysql) { Write-Output "[提示] 指定目录下未找到 mysql：$MysqlBin，尝试从 PATH 查找。" }
}
if (-not $mysql) {
    if (Get-Command "mysql" -ErrorAction SilentlyContinue) {
        $mysql = "mysql"
    } else {
        Write-Output "[错误] 未找到 mysql 客户端。请用 -MysqlBin 指定 MySQL 的 bin 目录，例如："
        Write-Output "        .\db-import.ps1 -SqlFile xxx.sql -MysqlBin 'D:\MySQL\bin'"
        exit 1
    }
}
Write-Output "[0/4] 使用 mysql 客户端：$mysql"

# 公共连接参数
$connArgs = @("--host=$DbHost", "--port=$DbPort", "--user=$DbUser", "--password=$DbPassword")

# 构造建库 SQL（含反引号包裹）
$createSql = "CREATE DATABASE IF NOT EXISTS ``$DbName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 1) 检测库是否已存在
Write-Output "[1/4] 检测目标库是否存在 ..."
$checkSql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '$DbName';"
$exists = & $mysql @connArgs "-N" "-e" $checkSql 2>$null
if ($LASTEXITCODE -ne 0) { Write-Output "[错误] 连接数据库失败，请检查 DbHost/DbUser/DbPassword 与 MySQL 服务。"; exit 1 }

if ($exists) {
    if (-not $Force) {
        $ans = Read-Host "目标库 '$DbName' 已存在，继续将 DROP 后重建（数据将被清空）。是否继续？(y/N)"
        if ($ans -notmatch '^[yY]$') { Write-Output "已取消。"; exit 0 }
    }
    Write-Output "       库已存在，先 DROP 再重建 ..."
    & $mysql @connArgs "-e" "DROP DATABASE ``$DbName``;" 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Output "[错误] DROP 库失败。"; exit 1 }
}

# 2) 建库
Write-Output "[2/4] 创建库 ${DbName} (utf8mb4) ..."
& $mysql @connArgs "-e" $createSql 2>$null
if ($LASTEXITCODE -ne 0) { Write-Output "[错误] 建库失败。"; exit 1 }

# 3) 导入
Write-Output "[3/4] 导入数据（可能需要数分钟，取决于数据量）..."
# 用 stdin 喂入 SQL，而不是 "-e source xxx" 或 cmd 的 "<" 重定向：
#   1) source 是 mysql 客户端内建命令，路径含空格时无法正确解析
#   2) 经 cmd.exe 拼接命令行时，--init-command 的值含空格与分号，
#      多层引号嵌套极易被拆错，导致导入静默失败（库建好但一张表都没有）
#   3) 这里直接用 .NET Process 把文件字节流写入 stdin，完全绕开命令行引号问题
$importArgs = @(
    "--host=$DbHost",
    "--port=$DbPort",
    "--user=$DbUser",
    "--password=$DbPassword",
    "--default-character-set=utf8mb4",
    "--max_allowed_packet=512M",
    "--init-command=SET FOREIGN_KEY_CHECKS=0;",
    $DbName
)

# PowerShell 5.1 的 ProcessStartInfo 没有 ArgumentList，只能用 Arguments 字符串。
# 对含空格的参数值加双引号（--init-command 的值即含空格与分号）。
$quotedArgs = $importArgs | ForEach-Object {
    if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
}

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName  = $mysql
$psi.Arguments = ($quotedArgs -join ' ')
$psi.RedirectStandardInput  = $true
$psi.RedirectStandardError  = $true
$psi.UseShellExecute        = $false

$proc = [System.Diagnostics.Process]::Start($psi)

# 以字节流原样透传，避免任何编码转换破坏 utf8mb4 内容
$fs = [System.IO.File]::OpenRead($SqlFile)
try {
    $fs.CopyTo($proc.StandardInput.BaseStream)
    $proc.StandardInput.BaseStream.Flush()
}
finally {
    $fs.Dispose()
    $proc.StandardInput.Close()
}

$errText = $proc.StandardError.ReadToEnd()
$proc.WaitForExit()

if ($proc.ExitCode -ne 0) {
    Write-Output "[错误] 导入失败，退出码：$($proc.ExitCode)"
    if (-not [string]::IsNullOrWhiteSpace($errText)) { Write-Output $errText }
    exit 1
}

# 4) 核对表数量
Write-Output "[4/4] 核对导入结果 ..."
$countSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '$DbName';"
$tableCount = & $mysql @connArgs "-N" "-e" $countSql 2>$null
Write-Output "       目标库 '$DbName' 表数量：$tableCount"

# 与源库对照提醒
Write-Output "       （源库导出时为 163 张表，数量一致即表示结构导入完整）"

Write-Output ""
Write-Output "导入完成。请将 yu-admin 的 application-druid.yml / application.yml 中连接信息指向本机，"
Write-Output "或设置环境变量 DB_HOST / DB_PORT / DB_USER / DB_PASSWORD / REDIS_HOST 后启动应用。"
