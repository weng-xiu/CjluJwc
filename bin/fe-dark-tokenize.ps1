# =============================================================
# P1 / §6.1 N7：暗色模式令牌化收敛（U1 设计令牌）
#
# 目标：把业务页/公共组件里硬编码的 Element 色值替换为 --dt-* / --el-* 令牌，
#   使 html.dark 下无需逐页覆盖即可正确换肤（tokens.scss 只重映射令牌）。
#
# 三层处理（均在内存中做正则替换后整体回写，逐字节保留其余内容）：
#   A. <style> 块内的 CSS 声明 —— 同时覆盖「独占一行」和「单行规则 { a:1;b:2 }」
#      两种写法（后者是统计卡片类页面的常见形式）。
#   B. <template> 块内的静态 style="..." 属性（不含 :style 动态绑定）。
#   C. 值中出现 gradient 时整行/整值跳过（渐变属品牌视觉层，明暗各自设计）。
#
# 不处理：<script> 块（ECharts canvas 色不解析 CSS 变量）、
#   login.vue / lock.vue（独立全屏渐变主题页，自带暗色设计）。
# 幂等：已替换为 var(...) 的值不再命中十六进制映射。
#
# 用法：
#   powershell -File bin\fe-dark-tokenize.ps1 -DryRun   # 只预览前若干条 diff
#   powershell -File bin\fe-dark-tokenize.ps1           # 实际写入
# =============================================================
param(
  [switch]$DryRun,
  [string[]]$Dirs = @('views', 'components', 'layout')
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root 'yu-ui-vue3\src'
$skip = @('login.vue', 'lock.vue')

# 只映射 tokens.scss / Element Plus 中确实存在的令牌，避免产出无效声明
# 长键在前，防止 #eee 抢先匹配 #eeeeee
$map = [ordered]@{}
@'
#303133|var(--dt-text-primary)
#606266|var(--dt-text-regular)
#909399|var(--dt-text-secondary)
#c0c4cc|var(--dt-text-placeholder)
#8492a6|var(--dt-text-placeholder)
#a0aec0|var(--dt-text-placeholder)
#dcdfe6|var(--dt-border-color)
#d8dce5|var(--dt-border-color)
#cbd5e0|var(--dt-border-color)
#e4e7ed|var(--dt-border-color-light)
#ebeef5|var(--dt-border-color-light)
#e8eaed|var(--dt-border-color-light)
#e9ecef|var(--dt-border-color-light)
#e2e8f0|var(--dt-border-color-light)
#dee2e6|var(--dt-border-color-light)
#fafbfc|var(--dt-fill-light)
#fafafa|var(--dt-fill-light)
#f5f7fa|var(--dt-fill-light)
#f7f8fa|var(--dt-fill-light)
#f7fafc|var(--dt-fill-light)
#fafcff|var(--dt-fill-light)
#eee|var(--dt-fill-light)
#f0f2f5|var(--dt-bg-page)
#edf3f9|var(--el-color-primary-light-9)
#ecf5ff|var(--el-color-primary-light-9)
#e6f0fd|var(--el-color-primary-light-9)
#f0f7ff|var(--el-color-primary-light-9)
#e6f7ff|var(--el-color-primary-light-9)
#f0f9eb|var(--el-color-success-light-9)
#e8f5e9|var(--el-color-success-light-9)
#c2e7b0|var(--el-color-success-light-7)
#fdf6ec|var(--el-color-warning-light-9)
#fff8e6|var(--el-color-warning-light-9)
#fef0f0|var(--el-color-danger-light-9)
#fbc4c4|var(--el-color-danger-light-7)
#007ab8|var(--dt-color-primary)
#00567f|var(--dt-color-primary)
#409eff|var(--el-color-primary)
#3182ce|var(--el-color-primary)
#2b6cb0|var(--el-color-primary)
#67c23a|var(--el-color-success)
#38a169|var(--el-color-success)
#e6a23c|var(--el-color-warning)
#d97706|var(--el-color-warning)
#f5a623|var(--el-color-warning)
#f56c6c|var(--el-color-danger)
#e53e3e|var(--el-color-danger)
#1a202c|var(--dt-text-primary)
#2d3748|var(--dt-text-primary)
#333|var(--dt-text-primary)
#495060|var(--dt-text-regular)
#718096|var(--dt-text-secondary)
'@ -split "`n" | ForEach-Object {
  $kv = $_.Trim() -split '\|'
  if ($kv.Count -eq 2) { $map[$kv[0]] = $kv[1] }
}

$propList = 'color|background-color|background|border-top-color|border-bottom-color|border-left-color|border-right-color|border-color|border|border-top|border-bottom|border-left|border-right|outline-color|outline|box-shadow'
# A：样式块内声明（组1=前界定符或行首，组2=属性，组3=冒号，组4=值）
$declRe = [regex]"(?m)((?:^|[{;\s])((?:$propList))(\s*:\s*))([^;{}]+)"
# B：模板内静态 style 属性（排除 :style 动态绑定）
$inlineRe = [regex]'(?<!:)(?<=\sstyle=")[^"]*'
$styleBlockRe = [regex]'(?s)<style[^>]*>.*?</style>'

function Convert-HexValue([string]$val, [string]$prop) {
  if ($val -match 'gradient') { return $val }        # C：渐变整值保留
  $out = $val
  # 已带十六进制回退值的 var() → 去掉回退值，避免产出 var(--x, var(--x)) 这类无效写法
  $out = [regex]::Replace($out, 'var\(\s*(--[\w-]+)\s*,\s*#[0-9a-fA-F]{3,8}\s*\)', 'var($1)')
  foreach ($k in $map.Keys) {
    $out = [regex]::Replace($out, ('(?<![0-9a-fA-F])' + [regex]::Escape($k) + '(?![0-9a-fA-F])'), $map[$k], 'IgnoreCase')
  }
  # background: #fff|#ffffff → 容器表面令牌；color: #fff 保留（深底白字）
  if ($prop -and $prop -match 'background') {
    $out = [regex]::Replace($out, '(?<![0-9a-fA-F])#f{3,8}(?![0-9a-fA-F])', 'var(--dt-bg-container)', 'IgnoreCase')
  }
  return $out
}

$evalDecl = {
  param($m)
  $new = Convert-HexValue $m.Groups[4].Value $m.Groups[2].Value
  if ($new -eq $m.Groups[4].Value) { return $m.Value }
  $m.Groups[1].Value + $new
}
$evalInline = {
  param($m)
  $new = Convert-HexValue $m.Value $null
  # 内联 style 里 color:#fff 之类无映射时原样返回
  if ($new -eq $m.Value) { return $m.Value }
  $new
}

$total = 0
$files = 0
$report = @()
foreach ($d in $Dirs) {
  $base = Join-Path $src $d
  if (-not (Test-Path $base)) { continue }
  Get-ChildItem -Recurse -Include *.vue -Path $base | Where-Object { $skip -notcontains $_.Name } | ForEach-Object {
    $path = $_.FullName
    $text = [System.IO.File]::ReadAllText($path)
    $new = $text
    # A：仅 <style> 块
    $new = $styleBlockRe.Replace($new, { param($sm) $declRe.Replace($sm.Value, $evalDecl) })
    # B：仅 <script> 之前（即 <template> 区）的静态 style 属性
    $cut = $new.IndexOf('<script')
    if ($cut -lt 0) { $cut = $new.Length }
    $head = $inlineRe.Replace($new.Substring(0, $cut), $evalInline)
    if ($head -ne $new.Substring(0, $cut)) { $new = $head + $new.Substring($cut) }
    if ($new -ne $text) {
      $diff = [System.Collections.Generic.List[object]]::new()
      $a = [regex]::Split($text, '\r?\n'); $b = [regex]::Split($new, '\r?\n')
      for ($i = 0; $i -lt [Math]::Min($a.Length, $b.Length); $i++) {
        if ($a[$i] -ne $b[$i]) { $diff.Add([object[]] @(($i + 1), $a[$i].Trim(), $b[$i].Trim())) }
      }
      $files++
      $total += $diff.Count
      $rel = $path.Substring($src.Length + 1)
      $report += [pscustomobject]@{ File = $rel; Lines = $diff.Count }
      if ($DryRun) {
        Write-Host "[dry] $rel ($($diff.Count) 行)"
        $show = [Math]::Min(3, $diff.Count)      # 用下标访问，避免管道把嵌套数组扁平化
        for ($j = 0; $j -lt $show; $j++) {
          $l = $diff[$j]
          Write-Host ("      L{0}- {1}" -f $l[0], $l[1])
          Write-Host ("      L{0}+ {1}" -f $l[0], $l[2])
        }
      }
      else {
        [System.IO.File]::WriteAllText($path, $new, (New-Object System.Text.UTF8Encoding($false)))
      }
    }
  }
}

if ($DryRun) { Write-Host '[dry-run] 未写盘' } else { $report | Sort-Object Lines -Descending | Format-Table -AutoSize }
Write-Host ("{0}收敛色值行数: {1}  涉及文件: {2}" -f $(if ($DryRun) { '待' } else { '已' }), $total, $report.Count)
