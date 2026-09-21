$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$login = Invoke-RestMethod -Uri "$base/login" -Method POST -ContentType 'application/json' -Body (@{username='admin';password='admin123';code='';uuid=''} | ConvertTo-Json)
$h = @{ Authorization = "Bearer $($login.token)" }
Write-Output "TOKEN_OK=$($login.code)"

Write-Output "`n==== S3 degreeConfig/effective ===="
(Invoke-RestMethod -Uri "$base/sam/degreeConfig/effective" -Headers $h) | ConvertTo-Json -Compress

Write-Output "`n==== A5 gradeRecord/entryWindow ===="
(Invoke-RestMethod -Uri "$base/aem/gradeRecord/entryWindow" -Headers $h) | ConvertTo-Json -Compress

Write-Output "`n==== A2 examPlan/detectConflicts?semesterId=1 ===="
(Invoke-RestMethod -Uri "$base/aem/examPlan/detectConflicts?semesterId=1" -Headers $h) | ConvertTo-Json -Compress -Depth 6

Write-Output "`n==== A1 examPlan/autoArrange/9001 ===="
(Invoke-RestMethod -Uri "$base/aem/examPlan/autoArrange/9001" -Method POST -Headers $h) | ConvertTo-Json -Compress -Depth 6

Write-Output "`n==== A5 grade submit->audit->edit-guard ===="
# pick a gradeId
$dbg = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --user=root --host=127.0.0.1 -N -B yu-cjlujwc -e "SELECT grade_id FROM aem_grade_record WHERE submit_status IS NULL OR submit_status IN ('0','3') LIMIT 1;" 2>$null
$gid = ($dbg | Select-Object -First 1)
Write-Output "PICK gradeId=$gid"
if ($gid) {
  (Invoke-RestMethod -Uri "$base/aem/gradeRecord/submit/$gid" -Method PUT -Headers $h) | ConvertTo-Json -Compress
  (Invoke-RestMethod -Uri "$base/aem/gradeRecord/audit/$gid`?approved=true" -Method PUT -Headers $h) | ConvertTo-Json -Compress
  # now attempt to edit a locked record directly -> should be rejected by service guard
  $cur = (Invoke-RestMethod -Uri "$base/aem/gradeRecord/$gid" -Headers $h).data
  $cur.totalScore = 55
  try {
    $r = Invoke-RestMethod -Uri "$base/aem/gradeRecord" -Method PUT -Headers $h -ContentType 'application/json' -Body ($cur | ConvertTo-Json -Depth 6)
    Write-Output "EDIT_RESULT code=$($r.code) msg=$($r.msg)"
  } catch { Write-Output "EDIT_ERR $($_.Exception.Message)" }
  # restore status to uncommitted so demo not left locked
  (Invoke-RestMethod -Uri "$base/aem/gradeRecord/unlock/$gid" -Method PUT -Headers $h) | ConvertTo-Json -Compress
}
