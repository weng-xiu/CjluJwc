# OA 接口回归验证脚本（临时）
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$redisCli = 'D:\websever\redis-6.2.6-win(not_pwd)\redis-cli.exe'

# 1. 获取验证码 uuid，从 Redis 取验证码答案
$cap = Invoke-RestMethod -Uri "$base/captchaImage" -Method Get
$uuid = $cap.uuid
$code = & $redisCli GET "captcha_codes:$uuid"
$code = $code.Trim('"')
Write-Output "captcha uuid=$uuid code=$code"

# 2. 登录
$loginBody = @{ username = 'admin'; password = 'admin123'; code = $code; uuid = $uuid } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$base/login" -Method Post -Body $loginBody -ContentType 'application/json'
if ($login.code -ne 200) { Write-Output "LOGIN FAILED: $($login | ConvertTo-Json -Depth 5)"; exit 1 }
$headers = @{ Authorization = "Bearer $($login.token)" }
Write-Output "login ok"

# 3. 流程实例条件查询
$r1 = Invoke-RestMethod -Uri "$base/oa/workflow/instance/list?pageNum=1&pageSize=5&status=finished" -Headers $headers
Write-Output "instance/list(finished): code=$($r1.code) total=$($r1.total)"
$r1b = Invoke-RestMethod -Uri "$base/oa/workflow/instance/list?pageNum=1&pageSize=5&status=running" -Headers $headers
Write-Output "instance/list(running): code=$($r1b.code) total=$($r1b.total)"

# 4. 流程实例详情（取第一条）
$rows = @($r1.rows) + @($r1b.rows)
if ($rows.Count -gt 0) {
  $pid1 = $rows[0].processInstanceId
  $r2 = Invoke-RestMethod -Uri "$base/oa/workflow/instance/detail?processInstanceId=$pid1" -Headers $headers
  Write-Output "instance/detail($pid1): code=$($r2.code) tasks=$($r2.data.tasks.Count)"
} else {
  Write-Output "instance/detail: skipped (no instances)"
}

# 5. 待办任务按名称筛选
$r3 = Invoke-RestMethod -Uri "$base/oa/workflow/task/todo?pageNum=1&pageSize=5&taskName=%E5%AE%A1" -Headers $headers
Write-Output "task/todo(taskName=审): code=$($r3.code) total=$($r3.total)"
$r3b = Invoke-RestMethod -Uri "$base/oa/workflow/task/todo?pageNum=1&pageSize=5" -Headers $headers
Write-Output "task/todo(all): code=$($r3b.code) total=$($r3b.total)"

# 6. 公文条件查询（文号/紧急程度）
$r4 = Invoke-RestMethod -Uri "$base/oa/document/list?pageNum=1&pageSize=5&urgentLevel=0" -Headers $headers
Write-Output "document/list(urgentLevel=0): code=$($r4.code) total=$($r4.total)"

# 7. 通知公告阅读上报（取第一条已发布公告）
$r5 = Invoke-RestMethod -Uri "$base/oa/notice/list?pageNum=1&pageSize=5&publishStatus=1" -Headers $headers
Write-Output "notice/list(published): code=$($r5.code) total=$($r5.total)"
if ($r5.rows.Count -gt 0) {
  $nid = $r5.rows[0].noticeId
  $before = $r5.rows[0].readCount
  $r6 = Invoke-RestMethod -Uri "$base/oa/notice/read/$nid" -Method Post -Headers $headers
  $r5b = Invoke-RestMethod -Uri "$base/oa/notice/list?pageNum=1&pageSize=5&publishStatus=1" -Headers $headers
  $after = ($r5b.rows | Where-Object { $_.noticeId -eq $nid }).readCount
  Write-Output "notice/read($nid): code=$($r6.code) readCount $before -> $after"
} else {
  Write-Output "notice/read: skipped (no published notices)"
}

# 8. 日程日期范围查询
$r7 = Invoke-RestMethod -Uri "$base/oa/schedule/list?pageNum=1&pageSize=5&params%5BbeginTime%5D=2026-01-01&params%5BendTime%5D=2026-12-31" -Headers $headers
Write-Output "schedule/list(dateRange): code=$($r7.code) total=$($r7.total)"

# 9. 会议按类型查询
$r8 = Invoke-RestMethod -Uri "$base/oa/meeting/list?pageNum=1&pageSize=5&meetingType=0" -Headers $headers
Write-Output "meeting/list(type=0): code=$($r8.code) total=$($r8.total)"

Write-Output "ALL DONE"
