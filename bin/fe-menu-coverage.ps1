# ============================================================================
# bin/fe-menu-coverage.ps1  （V4.0 §6.1 N7 / §7.3 快速可交付清单）
#
# 目的：核对「后端 sys_menu 下发的组件路径」与「Vue3 管理端实际存在的 .vue 页面」，
#       量化菜单落地率，找出会命中 src/views/Unmigrated.vue 占位页的菜单。
#
# 判定规则与运行时代码同源（store/modules/permission.js 的 loadView）：
#   loadView 遍历 import.meta.glob('../../views/**/*.vue')，
#   以「相对 views/ 去掉 .vue 的路径 == component 字符串」精确匹配，
#   匹配不到即返回 Unmigrated.vue 占位组件。本脚本用同一条规则静态校验，无需数据库。
#
# 数据来源：sql/**（历史手工脚本，只读）+ db/migration/**（Flyway 增量）。
#   component 为空的记录（M 型目录、F 型按钮）不构成页面需求，自动排除；
#   Layout / ParentView / InnerLink 由框架内置组件承接，同样排除；
#   被 Flyway 脚本置 status='1'（停用）的菜单不再下发路由，亦排除
#   （仅当该组件的所有菜单引用都已停用才排除）。
#
# 用法：
#   powershell -File bin/fe-menu-coverage.ps1                # 全量核对，有缺失则退出码 1
#   powershell -File bin/fe-menu-coverage.ps1 -Detailed      # 逐条列出缺失项及来源脚本
#   powershell -File bin/fe-menu-coverage.ps1 -Baseline 116  # 断言落地数不低于 116（棘轮）
# ============================================================================
[CmdletBinding()]
param(
  # 默认空串而非 (Split-Path -Parent $PSScriptRoot)：PowerShell 5.1 在 -File 调用下
  # param 默认值中 $PSScriptRoot 仍为空，会直接报绑定错误，故在脚本体内解析
  [string] $Root = '',
  [string] $UiDir = 'yu-ui-vue3',
  [int] $Baseline = 0,
  [switch] $Detailed
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($Root)) {
  $here = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Definition }
  $Root = Split-Path -Parent $here
}

# 与 loadView() 一致：框架内置组件不需要业务页文件
$builtin = @('Layout', 'ParentView', 'InnerLink', 'IFrame', 'iframe')

function Split-SqlTuple([string]$text) {
  # 按顶层逗号切分 VALUES 元组：正确处理单引号字符串、'' 与 \' 转义、嵌套括号（如 sysdate()）
  $items = New-Object System.Collections.Generic.List[string]
  $sb = New-Object System.Text.StringBuilder
  $inStr = $false
  $depth = 0
  for ($i = 0; $i -lt $text.Length; $i++) {
    $c = $text[$i]
    if ($inStr) {
      [void]$sb.Append($c)
      if ($c -eq '\' -and ($i + 1) -lt $text.Length) { [void]$sb.Append($text[++$i]); continue }
      if ($c -eq "'") {
        if (($i + 1) -lt $text.Length -and $text[$i + 1] -eq "'") { [void]$sb.Append("'"); $i++; continue }
        $inStr = $false
      }
      continue
    }
    if ($c -eq "'") { $inStr = $true; [void]$sb.Append($c); continue }
    if ($c -eq '(') { $depth++; [void]$sb.Append($c); continue }
    if ($c -eq ')') { $depth--; [void]$sb.Append($c); continue }
    if ($c -eq ',' -and $depth -eq 0) { $items.Add($sb.ToString().Trim()); [void]$sb.Clear(); continue }
    [void]$sb.Append($c)
  }
  if ($sb.Length -gt 0) { $items.Add($sb.ToString().Trim()) }
  return ,$items
}

function Unquote([string]$v) {
  if ([string]::IsNullOrWhiteSpace($v)) { return '' }
  $t = $v.Trim()
  if ($t -eq 'NULL' -or $t -eq 'null') { return '' }
  if ($t.Length -ge 2 -and $t.StartsWith("'") -and $t.EndsWith("'")) {
    return $t.Substring(1, $t.Length - 2).Replace("''", "'")
  }
  return $t
}

function Split-SqlValues([string]$body) {
  # 从 values 后的文本中按顶层括号提取每条元组内容（不含外层括号）
  # 不能用 \((.*?)\) 正则：sysdate() 等函数会让非贪心匹配提前终止
  $tuples = New-Object System.Collections.Generic.List[string]
  $sb = New-Object System.Text.StringBuilder
  $inStr = $false
  $depth = 0
  $capturing = $false
  for ($i = 0; $i -lt $body.Length; $i++) {
    $c = $body[$i]
    if ($inStr) {
      if ($capturing) { [void]$sb.Append($c) }
      if ($c -eq '\' -and ($i + 1) -lt $body.Length) { if ($capturing) { [void]$sb.Append($body[$i + 1]) }; $i++; continue }
      if ($c -eq "'") {
        if (($i + 1) -lt $body.Length -and $body[$i + 1] -eq "'") { if ($capturing) { [void]$sb.Append("''") }; $i++; continue }
        $inStr = $false
      }
      continue
    }
    if ($c -eq "'") { $inStr = $true; if ($capturing) { [void]$sb.Append($c) }; continue }
    if ($c -eq '(') {
      $depth++
      if ($depth -eq 1) { $capturing = $true; [void]$sb.Clear() } elseif ($capturing) { [void]$sb.Append($c) }
      continue
    }
    if ($c -eq ')') {
      $depth--
      if ($depth -eq 0) { $capturing = $false; $tuples.Add($sb.ToString()) } elseif ($capturing) { [void]$sb.Append($c) }
      continue
    }
    if ($capturing) { [void]$sb.Append($c) }
  }
  return ,$tuples
}

$uiPath = Join-Path $Root $UiDir
if (-not (Test-Path $uiPath)) { throw "找不到前端工程目录：$uiPath" }
$viewsPath = Join-Path $uiPath 'src\views'

# ---------- 收集 SQL 文本（回滚脚本不产生菜单需求，排除） ----------
$sqlFiles = @()
foreach ($sub in @('sql', 'db\migration')) {
  $p = Join-Path $Root $sub
  if (Test-Path $p) {
    $sqlFiles += Get-ChildItem -Path $p -Recurse -Filter '*.sql' -File |
      Where-Object { $_.FullName -notmatch '[\\/](rollback|备份|backup)[\\/]' }
  }
}
if ($sqlFiles.Count -eq 0) { throw '未找到任何 SQL 脚本' }

$reInsert = [regex]'(?is)insert\s+into\s+`?sys_menu`?\s*(\([^()]*\))?\s*values\s*(.+?);'
# guarded 写法：INSERT INTO sys_menu (cols) SELECT <literal-tuple> FROM DUAL WHERE NOT EXISTS(...)
#   phase 脚本大量采用此写法（幂等插入），旧版正则只认 VALUES，会整体漏计 → 落地率虚高。
$reInsertSelect = [regex]'(?is)insert\s+into\s+`?sys_menu`?\s*(\([^()]*\))\s*select\s+(.+?)\s+from\s+'
# UPDATE sys_menu SET ... status = '1' ... WHERE menu_id = N  → 该菜单已从路由下发中摘除
$reRetire = [regex]"(?is)update\s+sys_menu\s+set\b[^;]*?status\s*=\s*'1'[^;]*?where\b[^;]*?menu_id\s*=\s*(\d+)"

# component -> @{ refs = @("menuId@相对路径"); menus = @{id=...} }
$map = @{}
$retired = New-Object System.Collections.Generic.HashSet[string]
$stmtCount = 0

function Resolve-CompColIdx([string]$colsRaw) {
  # 返回 component 列下标：显式列名按名定位（剔除反引号），否则用 RuoYi 默认列序（第 6 位）
  if (-not $colsRaw) { return 5 }
  $cols = (Split-SqlTuple ($colsRaw.Trim(' ', '(', ')')))
  for ($k = 0; $k -lt $cols.Count; $k++) {
    if (((Unquote $cols[$k]).Trim('`')).ToLower() -eq 'component') { return $k }
  }
  return -1
}

foreach ($f in $sqlFiles) {
  $text = [IO.File]::ReadAllText($f.FullName, [Text.Encoding]::UTF8)
  $rel = $f.FullName.Substring($Root.Length + 1).Replace('\', '/')

  foreach ($mm in $reRetire.Matches($text)) { [void]$retired.Add($mm.Groups[1].Value) }

  foreach ($m in $reInsert.Matches($text)) {
    $stmtCount++
    # 元组级拆分：把 values 后到语句末尾的内容按 (...) 逐条取出（支持多行 VALUES (...),(...)）
    $body = $m.Groups[2].Value
    $colIdx = Resolve-CompColIdx $m.Groups[1].Value
    if ($colIdx -lt 0) { continue }

    foreach ($tuple in (Split-SqlValues $body)) {
      $items = Split-SqlTuple $tuple
      if ($items.Count -le $colIdx) { continue }
      $menuId = Unquote $items[0]
      $comp = (Unquote $items[$colIdx]).Trim('/')
      if ([string]::IsNullOrWhiteSpace($comp)) { continue }
      if ($builtin -contains $comp) { continue }
      if (-not $map.ContainsKey($comp)) { $map[$comp] = New-Object System.Collections.Generic.List[string] }
      $map[$comp].Add("$menuId@$rel")
    }
  }

  # guarded INSERT...SELECT：单条字面量元组，必带显式列名
  foreach ($m in $reInsertSelect.Matches($text)) {
    $stmtCount++
    $colIdx = Resolve-CompColIdx $m.Groups[1].Value
    if ($colIdx -lt 0) { continue }
    $items = Split-SqlTuple $m.Groups[2].Value
    if ($items.Count -le $colIdx) { continue }
    $menuId = Unquote $items[0]
    $comp = (Unquote $items[$colIdx]).Trim('/')
    if ([string]::IsNullOrWhiteSpace($comp)) { continue }
    if ($builtin -contains $comp) { continue }
    if (-not $map.ContainsKey($comp)) { $map[$comp] = New-Object System.Collections.Generic.List[string] }
    $map[$comp].Add("$menuId@$rel")
  }
}

# ---------- 落地判定（与 loadView 同源：views/<component>.vue 是否存在） ----------
$landed = @()
$missing = @()
$shapeIssue = @()
foreach ($comp in ($map.Keys | Sort-Object)) {
  $refs = $map[$comp]
  # 所有引用该组件的菜单都已停用 → 不再下发路由
  $alive = @($refs | Where-Object { -not $retired.Contains(($_ -split '@')[0]) })
  if ($alive.Count -eq 0) { continue }

  $exact = Join-Path $viewsPath ($comp.Replace('/', [IO.Path]::DirectorySeparatorChar) + '.vue')
  if (Test-Path $exact) { $landed += $comp; continue }

  $asIndex = Join-Path $viewsPath ($comp.Replace('/', [IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar + 'index.vue')
  if (Test-Path $asIndex) { $shapeIssue += $comp; continue }

  $missing += [pscustomobject]@{ Component = $comp; Refs = $alive }
}

$total = $landed.Count + $shapeIssue.Count + $missing.Count
Write-Output "SQL 脚本：$($sqlFiles.Count) 个 / insert into sys_menu 语句：$stmtCount 条"
Write-Output "需页面的菜单组件（去重、已排除目录/按钮/内置/停用）：$total"
Write-Output "已落地：$($landed.Count)　列形待补(存在 <comp>/index.vue)：$($shapeIssue.Count)　缺失：$($missing.Count)"
if ($total -gt 0) {
  $hit = $landed.Count + $shapeIssue.Count
  Write-Output ("菜单落地率：{0:N1}%" -f ($hit * 100.0 / $total))
}

if ($shapeIssue.Count -gt 0) {
  Write-Warning "以下 component 需写成 '<path>/index' 才能被 loadView 命中（当前为 '$comp' 形式）： $($shapeIssue -join ', ')"
}

if ($missing.Count -gt 0) {
  Write-Output ''
  Write-Output '>>> 未落地菜单（运行时会命中 src/views/Unmigrated.vue 占位页）：'
  foreach ($x in $missing) {
    if ($Detailed) {
      Write-Output ("  - {0}  ← {1}" -f $x.Component, ($x.Refs -join '; '))
    } else {
      Write-Output ("  - {0}  ({1} 个菜单引用)" -f $x.Component, $x.Refs.Count)
    }
  }
  if (-not $Detailed) { Write-Output '  （加 -Detailed 可展开每条引用来源脚本明细）' }
}

if ($Baseline -gt 0 -and $landed.Count -lt $Baseline) {
  Write-Output "结果：FAIL —— 已落地 $($landed.Count) 低于基线 $Baseline"
  exit 1
}
if ($missing.Count -gt 0) {
  Write-Output '结果：FAIL —— 存在未落地菜单'
  exit 1
}
Write-Output '结果：PASS'
exit 0
