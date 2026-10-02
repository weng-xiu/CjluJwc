# ============================================================
# db-menu-check.ps1 — sys_menu 主键占用与分段冲突扫描（无网络、无 DB，适合 CI）
# 适用：新增菜单脚本前自查 / CI 门禁
# 用法：
#   powershell -File bin/db-menu-check.ps1                          # 全量扫描 sql/ 与 db/migration
#   powershell -File bin/db-menu-check.ps1 -Files sql\phase36.sql   # 只看指定文件（配合 git diff 使用）
#   powershell -File bin/db-menu-check.ps1 -IgnoreLegacy            # 只判定 db/migration 引入的新冲突
# 判定口径：
#   FAIL  同一 menu_id 在不同脚本里对应不同的 menu_name —— 真实撞号，后执行者主键冲突报错
#   FAIL  同一脚本内同一 menu_id 出现多次且名称不同
#   FAIL  menu_id 落在 db/README.md 分段表未登记的空洞区间
#   WARN  同一 menu_id 同名跨脚本重复 —— 多为 delete+insert 幂等写法
#   WARN  db/migration 新增菜单未使用 3000+ 保留段
# 背景：V4.0 §7.3/K1 —— 历史 phase 脚本各自挑号，缺乏统一登记；本脚本用于把
#       「口头规矩」变成可执行的门禁。分段规则见 db/README.md。
# ============================================================
param(
    [string]$Root = (Split-Path -Parent $PSScriptRoot),
    [string[]]$Files = @(),
    [switch]$IgnoreLegacy
)

$ErrorActionPreference = 'Continue'

# 分段表：menu_id 归属（与 db/README.md 的表保持一致，两处需同步修改）
# 区间为「实际观测占用」而非「设计意图」，来源见 db/legacy/MANIFEST.md 附录
$segments = @(
    @{ Min = 1;    Max = 1199;  Owner = 'RuoYi 框架原生（含菜单 115 表单构建）' },
    @{ Min = 2000; Max = 2099;  Owner = 'BRM 基础资源' },
    @{ Min = 2100; Max = 2199;  Owner = 'TPM 培养过程' },
    @{ Min = 2200; Max = 2299;  Owner = 'AEM 考核评价' },
    @{ Min = 2300; Max = 2399;  Owner = 'OA 办公 / SAM 学籍（两段共用，历史撞号区）' },
    @{ Min = 2400; Max = 2499;  Owner = 'DIS 数据对接' },
    @{ Min = 2500; Max = 2599;  Owner = 'Portal 门户 / 用户管理增强' },
    @{ Min = 2600; Max = 2699;  Owner = 'Portal CMS / phase1 增量' },
    @{ Min = 2700; Max = 2799;  Owner = 'OA 补充 / phase4 增量' },
    @{ Min = 2800; Max = 2899;  Owner = '预留' },
    @{ Min = 2900; Max = 2999;  Owner = '菜单修正补丁（fix_*_menu）' },
    @{ Min = 3000; Max = 999999999; Owner = '基线之后的新增变更专用段（db/migration）' }
)

function Get-Owner([long]$id) {
    foreach ($s in $segments) { if ($id -ge $s.Min -and $id -le $s.Max) { return $s.Owner } }
    return '!!未登记分段!!'
}

# ---------- 收集待扫描文件 ----------
if ($Files.Count -gt 0) {
    $targets = $Files | ForEach-Object {
        if ([IO.Path]::IsPathRooted($_)) { $_ } else { Join-Path $Root $_ }
    } | Where-Object { Test-Path $_ }
} else {
    $targets = @()
    foreach ($d in @('sql', 'db/migration')) {
        $full = Join-Path $Root $d
        if (Test-Path $full) {
            # sql/备份/ 为真实数据 dump，已出库并 gitignore，不参与口径统计
            $targets += Get-ChildItem $full -Recurse -Filter '*.sql' -File |
                Where-Object { $_.FullName -notmatch '\\sql\\备份\\' } | ForEach-Object { $_.FullName }
        }
    }
}
Write-Output "=== db-menu-check：扫描 $($targets.Count) 个脚本 ==="

# ---------- 提取 (menu_id, menu_name) ----------
# 只匹配「元组首个字段为纯数字、第二个字段为字符串」的显式主键插入，
# 天然排除 (menu_id, menu_name) 列清单与 (select ...) 子查询形式。
$reInsert = [regex]'(?is)insert\s+into\s+sys_menu\b[^;]*?values\s*(.+?);'
$reTuple  = [regex]"^\(?\s*'?(\d+)'?\s*,\s*'([^']*)'"

$rows = @()
foreach ($f in $targets) {
    $content = Get-Content $f -Raw -Encoding UTF8
    if ([string]::IsNullOrWhiteSpace($content)) { continue }
    $leaf = Split-Path -Leaf $f
    $isMigration = $leaf -like 'V*'
    foreach ($m in $reInsert.Matches($content)) {
        foreach ($piece in ($m.Groups[1].Value -split '\)\s*,\s*\(')) {
            $t = $reTuple.Match($piece.Trim())
            if ($t.Success) {
                $rows += [pscustomobject]@{
                    Id        = [long]$t.Groups[1].Value
                    Name      = $t.Groups[2].Value
                    File      = $leaf
                    Migration = $isMigration
                }
            }
        }
    }
}

if ($rows.Count -eq 0) {
    Write-Output "[WARN] 未提取到任何 sys_menu 显式主键插入"
    exit 0
}
$ids = @($rows | ForEach-Object { $_.Id } | Sort-Object -Unique)
Write-Output "[ OK ] 提取到显式 menu_id 去重后 $($ids.Count) 个（插入元组 $($rows.Count) 条）"

$fail = 0
$warnCount = 0

# ---------- 1) 同 ID 不同名称 = 真实撞号 ----------
$conflicts = @()
foreach ($g in ($rows | Group-Object Id)) {
    $names = @($g.Group | ForEach-Object { $_.Name } | Select-Object -Unique)
    if ($names.Count -gt 1) {
        $files = @($g.Group | ForEach-Object { $_.File } | Select-Object -Unique)
        $conflicts += [pscustomobject]@{
            Id     = [long]$g.Name
            Names  = ($names | ForEach-Object { "'$_'" }) -join ' vs '
            Files  = $files -join ', '
            IdStr  = "$($g.Name)"
        }
    }
}
# 同一脚本内的重复（名称不同）单列，便于定位
foreach ($c in ($conflicts | Sort-Object Id)) {
    if ($IgnoreLegacy -and $c.Files -notmatch '^V') {
        Write-Output "[SKIP] menu_id $($c.Id) 属历史遗留冲突（-IgnoreLegacy）：$($c.Names)"
        continue
    }
    $fail++
    Write-Output "[FAIL] 菜单撞号 menu_id=$($c.Id) → $($c.Names)"
    Write-Output "       涉及脚本：$($c.Files)"
}
# ---------- 2) 同 ID 同名跨脚本 = 幂等写法，仅提示 ----------
$reuses = @($rows | Group-Object Id | Where-Object {
    ($_.Group | ForEach-Object { $_.Name } | Select-Object -Unique).Count -eq 1 -and
    ($_.Group | ForEach-Object { $_.File } | Select-Object -Unique).Count -gt 1
})
foreach ($g in $reuses) {
    $warnCount++
    $files = @($g.Group | ForEach-Object { $_.File } | Select-Object -Unique)
    Write-Output "[WARN] menu_id $($g.Name)（$($g.Group[0].Name)）跨脚本重复插入：$($files -join ', ')（delete+insert 幂等写法可接受，但会静默覆盖前值）"
}

# ---------- 3) 分段归属 ----------
Write-Output ""
Write-Output "--- menu_id 占用分布 ---"
$grouped = $ids | Group-Object { Get-Owner $_ }
foreach ($g in ($grouped | Sort-Object { $_.Name })) {
    $arr = @($g.Group | ForEach-Object { [long]$_ } | Sort-Object)
    $range = if ($arr.Count -gt 1) { "$($arr[0])–$($arr[-1])" } else { "$($arr[0])" }
    Write-Output ("[INFO] {0,-42} {1,4} 个  区间 {2}" -f $g.Name, $g.Count, $range)
}
foreach ($g in @($grouped | Where-Object { $_.Name -like '!!*' })) {
    $fail++
    Write-Output "[FAIL] menu_id 落在未登记分段：$(($g.Group | Sort-Object {[long]$_}) -join ', ')（请在 db/README.md 与本脚本分段表同步补充）"
}

# ---------- 4) 新增变更须用 3000+ 保留段 ----------
$migRows = @($rows | Where-Object { $_.Migration })
if ($migRows.Count -gt 0) {
    foreach ($r in ($migRows | Sort-Object Id -Unique)) {
        if ($r.Id -lt 3000) {
            $warnCount++
            Write-Output "[WARN] db/migration/$($r.File) 插入的 menu_id=$($r.Id) 低于 3000 保留段，易与历史脚本撞号"
        }
    }
}

Write-Output ""
if ($fail -gt 0) {
    Write-Output "结果：FAIL —— $fail 项真实冲突，$warnCount 项提示"
    Write-Output "说明：真实撞号会让基线重放（bin/db-baseline.ps1）在第二条 INSERT 处主键冲突中断。"
    Write-Output "      修复方式：为后执行的脚本改派 3000+ 号段，并在 db/migration/ 提交变更+回滚成对脚本；"
    Write-Output "      存量库不受影响（早已按当时顺序落库），此项仅阻塞新库重建。"
    exit 1
}
Write-Output "结果：PASS —— $warnCount 项提示，无真实冲突"
exit 0
