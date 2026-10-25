# E5 校验失败滚动定位扩面 codemod（Vue2 主栈 yu-ui，复用 E7 的同一批 14 页）
# 幂等：已含 scrollToError 则跳过。
$base = 'd:\project\CjluJwc\CjluJwc\yu-ui\src\views'
$targets = @(
  'brm\campus\index.vue','brm\classroom\index.vue','brm\roomtype\index.vue','brm\semester\index.vue',
  'tpm\courseLib\index.vue','tpm\creditStruct\index.vue',
  'aem\gpaConfig\index.vue','aem\gradeWeight\index.vue',
  'sam\warningRule\index.vue','sam\degreeConfig\index.vue','sam\procedureStep\index.vue',
  'system\post\index.vue','system\config\index.vue','dis\system\index.vue'
)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
foreach ($t in $targets) {
  $p = Join-Path $base $t
  if (-not (Test-Path $p)) { Write-Host "MISS $t"; continue }
  $raw = [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)
  if ($raw -match 'scrollToError') { Write-Host "SKIP(already) $t"; continue }

  # 1. import（紧邻 dialogGuard import 之后）
  $raw = $raw.Replace(
    'import dialogGuard from "@/utils/dialogGuard"',
    "import dialogGuard from `"@/utils/dialogGuard`"`nimport scrollToError from `"@/utils/scrollToError`""
  )
  # 2. mixins 追加 scrollToError
  $raw = $raw.Replace('mixins: [dialogGuard],', 'mixins: [dialogGuard, scrollToError],')
  # 3. submitForm 校验失败分支（在回调开头早退式提示，兼容单行/多行）
  $raw = [regex]::Replace($raw, '(\.validate\((?:valid)? => \{)', '$1 if (!valid) { this.scrollToError() }')

  [System.IO.File]::WriteAllText($p, $raw, $utf8NoBom)
  Write-Host "OK   $t"
}
