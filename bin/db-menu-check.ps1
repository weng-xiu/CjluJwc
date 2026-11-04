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
#   FAIL  同一 menu_id 同名跨脚本重复，且后执行者是裸 INSERT（自身无前置 DELETE、
#         未用 INSERT IGNORE / REPLACE / ON DUPLICATE KEY）—— 名字一样也照样 1062
#   FAIL  脚本里硬编码 `USE 库名;` —— 重放时库名由 -DbName 指定，写死必然 Unknown database
#   INFO  同一 menu_id 同名跨脚本重复，但后执行者自身幂等（G4/V6.5 新增口径）
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
    @{ Min = 2300; Max = 2399;  Owner = 'SAM 学籍学位（G4/V6.5 起独占，OA 已让位到 2700）' },
    @{ Min = 2400; Max = 2499;  Owner = 'DIS 数据对接' },
    @{ Min = 2500; Max = 2599;  Owner = 'Portal 门户 / 用户管理增强' },
    @{ Min = 2600; Max = 2699;  Owner = 'Portal CMS / 门户内容' },
    @{ Min = 2700; Max = 2799;  Owner = 'OA 办公（G4/V6.5 起独占）' },
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
# G4/V6.5：正则需兼容 `INSERT IGNORE INTO`——fix_schedule_opt_menu.sql 全部是这种写法，
# 旧正则只认 insert into，导致这 6 条从未入统计，撞号被持久漏收。
$reInsert = [regex]'(?is)(?:insert\s+(?:ignore\s+)?into|replace\s+into)\s+sys_menu\b[^;]*?values\s*(.+?);'
$reTuple  = [regex]"^\(?\s*'?(\d+)'?\s*,\s*'([^']*)'"
$reDelete = [regex]'(?is)delete\s+from\s+sys_menu\b[^;]*?;'
$reNumIn  = [regex]'(\d{2,7})'
$reUse    = [regex]'(?im)^\s*use\s+[^\s;]+\s*;'

# 重放顺序与执行开关：以 db/legacy/manifest.txt 为准（未登记文件排最后、视为启用）。
# 判「撞号算不算真阻断」需要两维信息：谁先谁后（Order），以及这个脚本到底跑不跑（Exec）。
# G4/V6.5 起 ry_oa_menu.sql 降为 execute=N（被 ry_oa_menu_2700.sql 完整取代），
# 它里面的 2300 段与 SAM 冲突仍然存在，但已不阻断空库重放——这一维必须能表达出来。
$manifestPath = Join-Path $Root 'db/legacy/manifest.txt'
$manifestMap = @{}
if (Test-Path $manifestPath) {
    foreach ($raw in Get-Content $manifestPath -Encoding UTF8) {
        $line = $raw.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { continue }
        $p = $line -split '\|'
        if ($p.Count -lt 3) { continue }
        $manifestMap[(Split-Path -Leaf ($p[1].Trim() -replace '\\', '/'))] = [pscustomobject]@{
            Order = [double]$p[0].Trim()
            Exec  = $p[2].Trim().ToUpper()
        }
    }
}

$rows = @()
$useHits = @()
foreach ($f in $targets) {
    $content = Get-Content $f -Raw -Encoding UTF8
    if ([string]::IsNullOrWhiteSpace($content)) { continue }
    $leaf = Split-Path -Leaf $f
    $isMigration = $leaf -like 'V*'
    $order = if ($manifestMap.ContainsKey($leaf)) { $manifestMap[$leaf].Order } else { 9000.0 }
    $disabled = ($manifestMap.ContainsKey($leaf) -and $manifestMap[$leaf].Exec -ne 'Y')
    $um = $reUse.Match($content)
    if ($um.Success) { $useHits += [pscustomobject]@{ File = $leaf; Stmt = $um.Value.Trim() } }
    # 本文件自己删过哪些号（供「同名重复是否已被自身清理」判定）
    $deleteIds = @()
    foreach ($dm in $reDelete.Matches($content)) {
        foreach ($nm in $reNumIn.Matches($dm.Value)) { $deleteIds += [long]$nm.Groups[1].Value }
    }
    foreach ($m in $reInsert.Matches($content)) {
        $stmt = $m.Value
        # 自身幂等：INSERT IGNORE / REPLACE / ON DUPLICATE KEY
        $idem = ($stmt -match '(?i)^\s*(insert\s+ignore|replace\s+into)') -or ($stmt -match '(?i)on\s+duplicate\s+key')
        foreach ($piece in ($m.Groups[1].Value -split '\)\s*,\s*\(')) {
            $t = $reTuple.Match($piece.Trim())
            if ($t.Success) {
                $id = [long]$t.Groups[1].Value
                $rows += [pscustomobject]@{
                    Id        = $id
                    Name      = $t.Groups[2].Value
                    File      = $leaf
                    Migration = $isMigration
                    Order     = $order
                    Disabled  = $disabled
                    Idem      = $idem
                    Del       = ($deleteIds -contains $id)
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
$infoCount = 0

# ---------- 1) 同 ID 不同名称 = 真实撞号 ----------
$conflicts = @()
foreach ($g in ($rows | Group-Object Id)) {
    # 只看 execute=Y 的行：不参与重放的脚本里的撞号不可能触发 1062，
    # 但也不能悄悄消失——下面单列成 INFO，保留可见性。
    $en = @($g.Group | Where-Object { -not $_.Disabled })
    $enNames = @($en | ForEach-Object { $_.Name } | Select-Object -Unique)
    if ($enNames.Count -gt 1) {
        $files = @($en | ForEach-Object { $_.File } | Select-Object -Unique)
        $conflicts += [pscustomobject]@{
            Id     = [long]$g.Name
            Names  = ($enNames | ForEach-Object { "'$_'" }) -join ' vs '
            Files  = $files -join ', '
            IdStr  = "$($g.Name)"
        }
        continue
    }
    $allNames = @($g.Group | ForEach-Object { $_.Name } | Select-Object -Unique)
    if ($allNames.Count -gt 1) {
        $infoCount++
        $off = @($g.Group | Where-Object { $_.Disabled } | ForEach-Object { $_.File } | Select-Object -Unique)
        Write-Output "[INFO] menu_id $($g.Name)（$($allNames -join ' / ')）撞号仅存在于已停用脚本：$($off -join ', ')（execute=N，不阻断重放）"
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
# ---------- 2) 同 ID 同名跨脚本：必须真幂等，否则第二条主键必冲突 ----------
# 旧口径只判 WARN，理由写的是「delete+insert 幂等写法可接受」。但那只成立于两种情形：
# 该文件自己先 DELETE 过这个号，或用了 INSERT IGNORE / REPLACE / ON DUPLICATE KEY。
# 裸 INSERT 再插同一 menu_id，就算菜单名一模一样也会报 1062 —— 这是真实阻断：
# fix_schedule_opt_menu.sql（IGNORE）先落 2620-2625，phase1 再裸 INSERT 同号即死。
$reuses = @($rows | Group-Object Id | Where-Object {
    ($_.Group | ForEach-Object { $_.Name } | Select-Object -Unique).Count -eq 1 -and
    ($_.Group | ForEach-Object { $_.File } | Select-Object -Unique).Count -gt 1
})
foreach ($g in $reuses) {
    $seq = @($g.Group | Where-Object { -not $_.Disabled } | Sort-Object -Property Order, File)
    if ($seq.Count -lt 2) { continue }
    $head = $seq[0]
    for ($i = 1; $i -lt $seq.Count; $i++) {
        $s = $seq[$i]
        if ($s.Idem -or $s.Del) {
            $why = if ($s.Idem) { 'INSERT IGNORE / REPLACE / ON DUPLICATE KEY' } else { '自身前置 DELETE' }
            $infoCount++
            Write-Output "[INFO] menu_id $($s.Id)（$($s.Name)）$($s.File) 重复插入 $($head.File)，后执行者幂等（$why）"
            continue
        }
        if ($IgnoreLegacy -and -not $s.Migration -and -not $head.Migration) {
            Write-Output "[SKIP] menu_id $($s.Id)（$($s.Name)）同名重复（-IgnoreLegacy 历史遗留）：$($head.File) → $($s.File)"
            continue
        }
        $fail++
        Write-Output "[FAIL] menu_id $($s.Id)（$($s.Name)）重放必撞：$($head.File) 已插入，$($s.File) 再裸 INSERT"
        Write-Output "       修复：后执行者改用 INSERT IGNORE、或在同脚本内先 DELETE 该 menu_id、或整段删掉改由前序脚本提供"
    }
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

# ---------- 5) 硬编码 USE 库名 = 重放必崩 ----------
# 基线重放由 bin/db-baseline.ps1 -DbName 指定库，脚本自己 USE 一个固定库名时：
# CI 用 yu_cjlujwc、本地可能用 yu-cjljwc 或临时库，必然 Unknown database。
# （哪怕库名能对上，也会把重放写错库。G4/V6.5 已清掉 3 处）
foreach ($u in $useHits) {
    $fail++
    Write-Output "[FAIL] $($u.File) 硬编码库名：$($u.Stmt)"
    Write-Output "       修复：删掉该行，库由 mysql 命令行位置参数 / -DbName 决定"
}

Write-Output ""
if ($fail -gt 0) {
    Write-Output "结果：FAIL —— $fail 项真实冲突，$warnCount 项提示，$infoCount 项已豁免"
    Write-Output "说明：真实撞号会让基线重放（bin/db-baseline.ps1）在第二条 INSERT 处主键冲突中断。"
    Write-Output "      修复三选一：后执行者改 INSERT IGNORE / 先 DELETE 该号 / 整段删掉并交代数据由谁提供；"
    Write-Output "      存量库不受影响（早已按当时顺序落库），此项仅阻塞新库重建。"
    exit 1
}
Write-Output "结果：PASS —— 无真实冲突（$infoCount 项已验证豁免，$warnCount 项提示）"
exit 0
