# P4 API smoke test (after fixes)
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'

# 1) login admin
$login = Invoke-RestMethod -Uri "$base/login" -Method Post -ContentType 'application/json' -Body (@{username='admin';password='admin123';code='';uuid=''} | ConvertTo-Json)
$tk = $login.token
if (-not $tk) { Write-Output "LOGIN FAIL: $($login | ConvertTo-Json -Depth 4)"; exit 1 }
$H = @{ Authorization = "Bearer $tk" }
Write-Output "LOGIN ok"

# 2) template preview (TPL_GRADE_DEFAULT = templateId 1)
$pv = Invoke-RestMethod -Uri "$base/system/printTemplate/preview/1" -Headers $H
$html = [string]$pv.data
Write-Output "PREVIEW code=$($pv.code) dataLen=$($html.Length) badSilent=$($html.Contains('${!')) badVelCount=$($html.Contains('velocityCount')) hasSchool=$($html.Contains('大学'))"

# 3) render SCHEDULE (bizId=userId=1 admin)
$rd = Invoke-RestMethod -Uri "$base/system/credential/render?bizType=SCHEDULE&bizId=1&semesterId=1" -Headers $H
$html2 = [string]$rd.data
Write-Output "RENDER_SCHED code=$($rd.code) dataLen=$($html2.Length) unres=$($html2.Contains('${!'))"

# 4) issue GRADE for student 101 (wxl) then verify
$iss = Invoke-RestMethod -Uri "$base/system/credential/issue?bizType=GRADE&bizId=101&semesterId=1" -Method Post -Headers $H
$issHtml = [string]$iss.data
if ($issHtml -match 'P[A-Z]\d{10,}') { $serial = $Matches[0] } else { $serial = $issHtml }
Write-Output "ISSUE_GRADE code=$($iss.code) serial=$serial htmlLen=$($issHtml.Length)"

# find record via list to get verifyCode
Start-Sleep -Milliseconds 300
$lst = Invoke-RestMethod -Uri "$base/system/credential/list?bizType=GRADE&serialNo=$serial" -Headers $H
$row = $lst.rows | Select-Object -First 1
Write-Output "LIST total=$($lst.total) recordId=$($row.recordId) status=$($row.status)"

$vf = Invoke-RestMethod -Uri "$base/system/credential/verify?serialNo=$serial&verifyCode=$($row.verifyCode)"
Write-Output "VERIFY legit: code=$($vf.code) valid=$($vf.data.valid) name=$($vf.data.studentName) hashMatch=$($vf.data.hashMatch)"

$vb = Invoke-RestMethod -Uri "$base/system/credential/verify?serialNo=$serial&verifyCode=WRONGCODE1234567"
Write-Output "VERIFY badcode: valid=$($vb.data.valid)"

# 5) render by record (print)
$pr = Invoke-RestMethod -Uri "$base/system/credential/print/$($row.recordId)" -Headers $H
$html3 = [string]$pr.data
Write-Output "PRINT code=$($pr.code) dataLen=$($html3.Length) hasSerial=$($html3.Contains($serial))"

# 6) portal student wxl: mySeats / render EXAM_TICKET
$login2 = Invoke-RestMethod -Uri "$base/login" -Method Post -ContentType 'application/json' -Body (@{username='wxl';password='admin123';code='';uuid=''} | ConvertTo-Json)
$H2 = @{ Authorization = "Bearer $($login2.token)" }
if (-not $login2.token) { Write-Output "WXL LOGIN FAIL"; exit 1 }
$seats = Invoke-RestMethod -Uri "$base/portal/credential/mySeats" -Headers $H2
Write-Output "MYSEATS code=$($seats.code) n=$($seats.data.Count) first=$(($seats.data | Select-Object -First 1) | ConvertTo-Json -Compress)"
if ($seats.data.Count -gt 0) {
  $sid = $seats.data[0].seatId
  $rt = Invoke-RestMethod -Uri "$base/portal/credential/render?bizType=EXAM_TICKET&bizId=$sid" -Headers $H2
  $h4 = [string]$rt.data
  Write-Output "RENDER_TICKET code=$($rt.code) len=$($h4.Length) unres=$($h4.Contains('${!'))"
}
# portal own GRADE render with ownership check (wxl userId=101)
$rg = Invoke-RestMethod -Uri "$base/portal/credential/render?bizType=GRADE&bizId=101&semesterId=1" -Headers $H2
Write-Output "PORTAL_GRADE code=$($rg.code) len=$(([string]$rg.data).Length)"
# cross-owner attempt: wxl rendering bizId=102 should fail
try {
  $rx = Invoke-RestMethod -Uri "$base/portal/credential/render?bizType=GRADE&bizId=102&semesterId=1" -Headers $H2
  Write-Output "CROSS_OWNER code=$($rx.code) msg=$($rx.msg)"
} catch { Write-Output "CROSS_OWNER exc: $($_.Exception.Message)" }
Write-Output "SMOKE DONE"
