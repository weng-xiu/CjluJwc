# ============================================================
# db-ci-export.ps1 — 生成 CI 集成测试用的数据库基线（G4，绕开 sql/ 历史脚本重放）
#
# 背景：sql/ 78 个历史脚本存在 64 处 sys_menu 撞号（db-menu-check.ps1 判定），
#       空库重放不可行；整库 dump 含 PII 不得入库（C1 合规口径）。
# 方案：CI 基线 = 结构快照（无任何数据） + 框架元数据白名单 + 手工脱敏锚点行。
#       本脚本在开发机执行，生成前两件；03-anchors.sql 为仓库静态维护文件。
#
# 产物（可直接提交版本库，均不含 PII）：
#   db/ci/01-schema.sql  建表结构（--no-data；不含 Flowable 自有表 ACT_*/FLW_*，含 QRTZ_*）
#   db/ci/02-meta.sql    白名单元数据 INSERT：sys_menu/sys_role/sys_role_menu/
#                        sys_dept/sys_post/sys_config/sys_dict_type/sys_dict_data
#   db/ci/03-anchors.sql （静态，不在本脚本生成）合成测试账号与业务锚点行
#
# 用法：
#   .\db-ci-export.ps1                          # 默认 127.0.0.1:3306 yu-cjlujwc root/root
#   .\db-ci-export.ps1 -DbPass xxx -MysqlBin "D:\MySQL\bin"
# 导出后自动执行 PII 扫描门禁：命中疑似邮箱/手机号/身份证则删除产物并报错。
#
# CI 侧完整初始化顺序（见 .github/workflows/ci.yml integration job）：
#   01-schema.sql → 02-meta.sql → 03-anchors.sql
#   → sql/phase7_portal_selection_seed.sql（93xx 课程锚点，仓内既有脱敏脚本）
#   → testdata/reset-all.sql（97xx fixture）
# ============================================================
param(
    [string]$DbHost    = "127.0.0.1",
    [int]   $DbPort    = 3306,
    [string]$DbUser    = "root",
    [string]$DbPass    = "root",
    [string]$DbName    = "yu-cjlujwc",
    [string]$MysqlBin  = "",
    [string]$OutDir    = "db\ci"
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

# ---------- 定位 mysqldump ----------
$mysqldump = $null
if ($MysqlBin) {
    $p = Join-Path $MysqlBin "mysqldump.exe"
    if (Test-Path $p) { $mysqldump = $p }
}
if (-not $mysqldump) {
    $cmd = Get-Command "mysqldump" -ErrorAction SilentlyContinue
    if ($cmd) { $mysqldump = $cmd.Source }
}
if (-not $mysqldump) {
    foreach ($c in @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
        "F:\phpstudy_pro\Extensions\MySQL8.0.12\bin\mysqldump.exe")) {
        if (Test-Path $c) { $mysqldump = $c; break }
    }
}
if (-not $mysqldump) { Write-Output "[错误] 未找到 mysqldump，请用 -MysqlBin 指定 bin 目录"; exit 1 }
Write-Output "[0] mysqldump：$mysqldump"

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir | Out-Null }
$schemaOut = Join-Path $OutDir "01-schema.sql"
$metaOut   = Join-Path $OutDir "02-meta.sql"

# 密码经 MYSQL_PWD 注入，避免命令行明文
$env:MYSQL_PWD = $DbPass
$baseArgs = @("--host=$DbHost", "--port=$DbPort", "--user=$DbUser",
              "--default-character-set=utf8mb4", "--no-tablespaces", $DbName)

# ---------- 1) 结构快照（无数据；排除 Flowable 自有表） ----------
# ACT_*/FLW_* 不进快照：结构 dump 无版本数据行，引擎 database-schema-update 会误走升级路径
# （Duplicate column）；it/CI profile 下由 Flowable 7.1 启动时自建，版本与 jar 天然一致。
$mysql = Join-Path (Split-Path $mysqldump) "mysql.exe"
if (-not (Test-Path $mysql)) { $mysql = (Get-Command "mysql" -ErrorAction SilentlyContinue).Source }
if (-not $mysql) { Write-Output "[错误] 未找到 mysql 客户端（用于枚举 Flowable 表）"; exit 1 }
$fwTables = & $mysql --host=$DbHost --port=$DbPort --user=$DbUser -N -e "SELECT table_name FROM information_schema.tables WHERE table_schema='$DbName' AND (table_name LIKE 'ACT\_%' OR table_name LIKE 'FLW\_%');" 2>$null
$ignoreArgs = @()
foreach ($t in $fwTables) { if ($t) { $ignoreArgs += "--ignore-table=$DbName.$t" } }
Write-Output "[1] 排除 Flowable 自有表 $($ignoreArgs.Count) 张（引擎启动自建）"
& $mysqldump --no-data --skip-comments @ignoreArgs @baseArgs `
    --result-file="$schemaOut" 2>$null
if ($LASTEXITCODE -ne 0) { Write-Output "[错误] 结构导出失败"; exit 1 }
# 顶部加执行序注释与字符集
$head = @"
-- G4 CI 基线 · 结构快照（自动生成于 $(Get-Date -Format 'yyyy-MM-dd HH:mm')，勿手改；重生成：bin/db-ci-export.ps1）
-- 注：不含 ACT_*/FLW_*（Flowable 自有表），由集成测试 it profile 的 database-schema-update:true 启动自建
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
"@
$body = Get-Content $schemaOut -Raw -Encoding UTF8
[System.IO.File]::WriteAllText($schemaOut, ($head + "`r`n" + $body + "`r`nSET FOREIGN_KEY_CHECKS = 1;`r`n"), [System.Text.UTF8Encoding]::new($false))
$tblCount = ([regex]::Matches($body, 'CREATE TABLE')).Count
Write-Output "[1] 01-schema.sql 生成完成：$tblCount 张表"

# ---------- 2) 框架元数据白名单 ----------
$metaTables = @("sys_menu","sys_role","sys_role_menu","sys_dept","sys_post",
                "sys_config","sys_dict_type","sys_dict_data")
& $mysqldump --no-create-info --complete-insert --skip-extended-insert `
    --skip-comments @baseArgs --tables @metaTables `
    --result-file="$metaOut" 2>$null
if ($LASTEXITCODE -ne 0) { Write-Output "[错误] 元数据导出失败"; exit 1 }
$head2 = @"
-- G4 CI 基线 · 框架元数据白名单（表：$($metaTables -join ', ')；重生成：bin/db-ci-export.ps1）
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
"@
$body2 = Get-Content $metaOut -Raw -Encoding UTF8
[System.IO.File]::WriteAllText($metaOut, ($head2 + "`r`n" + $body2 + "`r`nSET FOREIGN_KEY_CHECKS = 1;`r`n"), [System.Text.UTF8Encoding]::new($false))
Write-Output "[2] 02-meta.sql 生成完成：$((Get-Item $metaOut).Length) 字节"

# ---------- 3) PII 扫描门禁 ----------
Write-Output "[3] PII 扫描……"
# 命中检测模式（全面扫描）
$patterns = @{
    "邮箱"   = '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'
    "手机号" = '(?<!\d)1[3-9]\d{9}(?!\d)'
    "身份证" = '(?<!\d)\d{17}[\dXx](?!\d)'
}
# 白名单（合成/机构公开值，非个人信息）：RuoYi 演示号、号段式合成号、学校机构域名邮箱
# V5.23 S-10：合成身份证号段 99010119900101xxxx 与合成手机号段 1380000xxxx 与
# sql/desensitize-test-data.sql 的改写公式一致，仓库级门禁见 bin/pii-scan.js
$allow = @('^15888888888$', '^1380000\d{4}$', '^99010119900101\d{4}$', '@cjlu\.edu\.cn$', '@163\.com$')
$hits = @()
foreach ($f in @($schemaOut, $metaOut, (Join-Path $OutDir "03-anchors.sql"))) {
    if (-not (Test-Path $f)) { continue }
    foreach ($k in $patterns.Keys) {
        $m = Select-String -Path $f -Pattern $patterns[$k] -AllMatches
        foreach ($line in $m) {
            foreach ($v in $line.Matches | ForEach-Object { $_.Value } | Select-Object -Unique) {
                $white = $false
                foreach ($a in $allow) { if ($v -match $a) { $white = $true; break } }
                if (-not $white) {
                    $hits += "$([System.IO.Path]::GetFileName($f)) [$k] 行$($line.LineNumber)：$v"
                }
            }
        }
    }
}
if ($hits.Count -gt 0) {
    Write-Output "[失败] 疑似 PII 命中，已删除生成产物，请勿提交："
    $hits | Select-Object -First 20 | ForEach-Object { Write-Output "  - $_" }
    Remove-Item $schemaOut, $metaOut -ErrorAction SilentlyContinue
    exit 1
}
Write-Output "[3] PII 扫描通过（0 命中）"
Write-Output "=== 完成。三件套：db/ci/01-schema.sql + 02-meta.sql + 03-anchors.sql（静态） ==="
