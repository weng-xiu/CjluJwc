# ============================================================
# db-manifest-check.ps1 — 数据库脚本清单一致性校验（无网络、无 DB，适合 CI）
# 适用：本地提交前自查 / CI 门禁
# 用法：
#   powershell -File bin/db-manifest-check.ps1
#   powershell -File bin/db-manifest-check.ps1 -Quiet   # 只输出结论
# 校验项：
#   1) db/legacy/manifest.txt 语法与序号唯一性
#   2) 清单登记的脚本文件确实存在
#   3) sql/ 下所有 .sql 均已登记（防止新增脚本逃逸基线管理）
#   4) 清单中不存在指向已消失文件的条目
#   5) db/migration 与 db/rollback 成对（每个变更都有回滚脚本）
#   6) db/migration 文件名符合 Flyway V 命名规范，且版本号不重复
# 退出码：0=全部通过，1=存在 FAIL
# ============================================================
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [switch]$Quiet
)

$ErrorActionPreference = 'Continue'
$fail = 0
$warn = 0

function Write-Line($msg) { if (-not $Quiet) { Write-Output $msg } }
function Fail($msg) { $script:fail++; Write-Output "[FAIL] $msg" }
function Warn($msg) { $script:warn++; Write-Output "[WARN] $msg" }
function Ok($msg)   { Write-Line "[ OK ] $msg" }

Write-Line "=== db-manifest-check：仓库根 $Root ==="

# ---------- 1) 解析清单 ----------
$manifestPath = Join-Path $Root 'db/legacy/manifest.txt'
if (-not (Test-Path $manifestPath)) {
    Fail "清单不存在：$manifestPath"
    exit 1
}

$entries = @()
$lineNo = 0
foreach ($raw in Get-Content $manifestPath -Encoding UTF8) {
    $lineNo++
    $line = $raw.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { continue }
    $parts = $line -split '\|'
    if ($parts.Count -lt 3) { Fail "清单第 $lineNo 行字段不足 3 段：$line"; continue }
    $order = $parts[0].Trim()
    $rel   = ($parts[1].Trim() -replace '\\', '/')
    $exec  = $parts[2].Trim().ToUpper()
    if ($order -notmatch '^\d+\.\d{2}$') { Fail "清单第 $lineNo 行序号格式应为 n.nn：$order"; continue }
    if ($exec -notin @('Y', 'N')) { Fail "清单第 $lineNo 行 execute 只能是 Y/N：$exec"; continue }
    $entries += [pscustomobject]@{ LineNo = $lineNo; Order = $order; Path = $rel; Exec = $exec }
}
Ok "清单解析出 $($entries.Count) 条（执行 Y：$(@($entries | Where-Object Exec -eq 'Y').Count)，排除 N：$(@($entries | Where-Object Exec -eq 'N').Count)）"

# 序号唯一 + 递增
$dupOrder = $entries | Group-Object Order | Where-Object Count -gt 1
foreach ($g in $dupOrder) { Fail "序号重复：$($g.Name)（$($g.Group.LineNo -join ', ')）" }
$sorted = @($entries | ForEach-Object { [version]$_.Order })
for ($i = 1; $i -lt $sorted.Count; $i++) {
    if ($sorted[$i] -lt $sorted[$i - 1]) { Fail "清单序号非单调递增，第 $($entries[$i].LineNo) 行：$($entries[$i].Order)"; break }
}
# Y 段（基线 0.xx）内部必须严格递增，N 段（9.xx 排除项）不参与执行
$yOrders = @($entries | Where-Object Exec -eq 'Y' | ForEach-Object { [version]$_.Order })
for ($i = 1; $i -lt $yOrders.Count; $i++) {
    if ($yOrders[$i] -le $yOrders[$i - 1]) { Fail "执行序列内部序号重复或乱序：$($entries[$i].Order)"; break }
}
Ok "序号唯一性与递增性检查完成"

# ---------- 2) 登记的文件必须存在 ----------
foreach ($e in $entries) {
    $full = Join-Path $Root $e.Path
    if (-not (Test-Path $full)) { Fail "清单登记的文件不存在：$($e.Path)（第 $($e.LineNo) 行）" }
}
Ok "登记文件存在性检查完成"

# ---------- 3) sql/ 下不得有未登记脚本 ----------
# sql/备份/ 含真实数据 dump，已被 .gitignore 排除，不参与校验
$sqlDir = Join-Path $Root 'sql'
$registered = @($entries | ForEach-Object { $_.Path })
if (Test-Path $sqlDir) {
    $actual = Get-ChildItem $sqlDir -Recurse -Filter '*.sql' -File | ForEach-Object {
        ($_.FullName.Substring($Root.Length) -replace '\\', '/').TrimStart('/')
    } | Where-Object { $_ -notmatch '^sql/备份/' }
    $missing = @($actual | Where-Object { $registered -notcontains $_ })
    foreach ($m in $missing) { Fail "未登记进基线清单：$m（请在 db/legacy/manifest.txt 补行，说明顺序与是否执行）" }
    Ok "sql/ 实际 .sql 文件 $($actual.Count) 个，未登记 $($missing.Count) 个"
} else {
    Warn "sql/ 目录不存在，跳过未登记检查"
}

# ---------- 5) 回滚成对 ----------
$migDir = Join-Path $Root 'db/migration'
$rbDir  = Join-Path $Root 'db/rollback'
$mig = @()
if (Test-Path $migDir) { $mig = Get-ChildItem $migDir -Filter '*.sql' -File }
$rbNames = @()
if (Test-Path $rbDir) { $rbNames = @(Get-ChildItem $rbDir -Filter '*.sql' -File | ForEach-Object { $_.Name }) }
foreach ($f in $mig) {
    if ($rbNames -notcontains $f.Name) { Fail "变更脚本缺少同名回滚：db/migration/$($f.Name) → 应在 db/rollback/$($f.Name)" }
}
$migNames = @($mig | ForEach-Object { $_.Name })
foreach ($n in $rbNames) {
    if ($migNames -notcontains $n) { Fail "回滚脚本没有对应的变更脚本：db/rollback/$n" }
}
Ok "变更/回滚配对：$($mig.Count) 个变更，$($rbNames.Count) 个回滚"

# ---------- 6) Flyway 命名与版本唯一 ----------
$versions = @()
foreach ($f in $mig) {
    # V<版本>__<描述>.sql，版本内可含点/下划线分段
    if ($f.Name -notmatch '^V(.+)__[A-Za-z0-9_\-]+\.sql$') {
        Fail "命名不符 Flyway 规范（应为 V<版本>__<描述>.sql）：db/migration/$($f.Name)"
        continue
    }
    $v = $Matches[1]
    if ($versions -contains $v) { Fail "Flyway 版本号重复：$v" }
    $versions += $v
}
Ok "Flyway 命名与版本号唯一性检查完成"

Write-Line ""
if ($fail -gt 0) {
    Write-Output "结果：FAIL（$fail 项失败，$warn 项警告）"
    exit 1
}
Write-Output "结果：PASS（$warn 项警告）"
exit 0
