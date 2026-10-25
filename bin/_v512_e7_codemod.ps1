# E7 dialogGuard 扩面 codemod（Vue2 主栈 yu-ui，跨域单对话框 CRUD 页）
# 幂等：每步均带已接入守卫，重复执行不叠加。
$base = 'd:\project\CjluJwc\CjluJwc\yu-ui\src\views'
$targets = @(
  'brm\campus\index.vue','brm\classroom\index.vue','brm\roomtype\index.vue','brm\semester\index.vue',
  'tpm\courseLib\index.vue','tpm\creditStruct\index.vue',
  'aem\gpaConfig\index.vue','aem\gradeWeight\index.vue',
  'sam\warningRule\index.vue','sam\degreeConfig\index.vue','sam\procedureStep\index.vue',
  'system\post\index.vue','system\config\index.vue','dis\system\index.vue'
)
foreach ($t in $targets) {
  $p = Join-Path $base $t
  if (-not (Test-Path $p)) { Write-Host "MISS $t"; continue }
  # 显式 UTF-8（无 BOM）读写，防中文乱码
  $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
  $raw = [System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)
  if ($raw -match 'dialogGuard') { Write-Host "SKIP(already) $t"; continue }

  # 1. import dialogGuard（放在 export default 之前）
  $raw = $raw -replace '(?m)^export default \{', "import dialogGuard from `"@/utils/dialogGuard`"`nexport default {"
  # 2. mixins: [dialogGuard]（插入 export default 体内首行）
  $raw = $raw -replace '(?m)^export default \{\r?\n', "export default {`n  mixins: [dialogGuard],`n"
  # 3. el-dialog 绑定 :before-close（仅编辑对话框 :visible.sync="open"，允许属性跨行）
  $raw = $raw -replace '<el-dialog([^>]*?:visible\.sync="open"[^>]*?)>', '<el-dialog$1 :before-close="guardedBeforeClose">'
  # 4. 取消按钮走 guardedCancel 包裹（字面量替换，避免 $ 被解析）
  $raw = $raw.Replace('@click="cancel"', '@click="guardedCancel(cancel)"')
  # 5. 对话框打开处采集快照（handleAdd 与 handleUpdate 回填完成后）
  $raw = $raw -replace 'this\.open = true(?!; this\.captureDialogSnapshot)', 'this.open = true; this.captureDialogSnapshot()'

  [System.IO.File]::WriteAllText($p, $raw, $utf8NoBom)
  Write-Host "OK   $t"
}
