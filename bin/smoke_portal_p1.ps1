# P1 portal smoke: verify read-only contracts of rewritten portal pages
# usage: powershell -File bin\smoke_portal_p1.ps1
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$results = @()

function Step($name, $ok, $detail) {
    $script:results += [pscustomobject]@{ Step = $name; Result = ($(if ($ok) { 'PASS' } else { 'FAIL' })); Detail = $detail }
}

# check returned row actually carries the given property names
function Test-Keys($obj, $keys) {
    if ($null -eq $obj) { return 'no-row' }
    $missing = @()
    foreach ($k in $keys) {
        $props = @($obj.PSObject.Properties.Name)
        if ($props -notcontains $k) { $missing += $k }
    }
    if ($missing.Count -eq 0) { return 'ok' }
    return 'missing=' + ($missing -join ',')
}

$login = Invoke-RestMethod -Uri "$base/login" -Method Post -ContentType 'application/json' -Body (@{ username = 'admin'; password = 'admin123'; code = ''; uuid = '' } | ConvertTo-Json)
if ($login.code -ne 200) { Write-Host ('LOGIN FAIL: ' + $login.msg); exit 1 }
$H = @{ Authorization = "Bearer $($login.token)" }
Step 'POST /login (admin)' $true 'token issued'

# ---- adjustment (rewritten: source picker + list + cancel endpoint exists) ----
try {
    $r = Invoke-RestMethod -Uri "$base/portal/adjustment/list?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/adjustment/list' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('adjustId', 'adjustType', 'courseName', 'approveStatus', 'createTime')))
} catch { Step 'GET /portal/adjustment/list' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/adjustment/mySchedules?pageSize=200" -Headers $H
    $row = $r.rows | Select-Object -First 1
    # tpm_schedule rows carry weekDay/startPeriod/endPeriod/classroomName (no original* prefix)
    Step 'GET /portal/adjustment/mySchedules' ($r.code -eq 200) ("rows=$(@($r.rows).Count) keys=" + (Test-Keys $row @('scheduleId', 'courseName', 'weekDay', 'startPeriod', 'classroomName')))
} catch { Step 'GET /portal/adjustment/mySchedules' $false $_.Exception.Message }

# ---- evaluation / evalResult (teacher aggregate + comments) ----
try {
    $r = Invoke-RestMethod -Uri "$base/portal/evaluation/questionnaireList?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/evaluation/questionnaireList' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('title', 'questionCount', 'fullScore', 'evalStatus', 'isAnonymous')))
} catch { Step 'GET /portal/evaluation/questionnaireList' $false $_.Exception.Message }
$courseId = $null
try {
    $r = Invoke-RestMethod -Uri "$base/portal/evaluation/teacherResults" -Headers $H
    $first = @($r.data)[0]
    $courseId = $first.courseId
    Step 'GET /portal/evaluation/teacherResults' ($r.code -eq 200) ("rows=$( @($r.data).Count) keys=" + (Test-Keys $first @('courseId', 'courseName', 'semesterName', 'totalCount', 'avgScore', 'satisfactionRate')))
} catch { Step 'GET /portal/evaluation/teacherResults' $false $_.Exception.Message }
if (-not $courseId) {
    # admin has no brm_teacher profile -> exercise the teacher branch instead
    try {
        $tl = Invoke-RestMethod -Uri "$base/login" -Method Post -ContentType 'application/json' -Body (@{ username = '2001'; password = 'admin123'; code = ''; uuid = '' } | ConvertTo-Json)
        $TH = @{ Authorization = "Bearer $($tl.token)" }
        Step 'POST /login (teacher 2001)' ($tl.code -eq 200) 'token issued'
        $r = Invoke-RestMethod -Uri "$base/portal/evaluation/teacherResults" -Headers $TH
        $first = @($r.data)[0]
        $courseId = $first.courseId
        Step 'GET /portal/evaluation/teacherResults (teacher)' ($r.code -eq 200) ("rows=$( @($r.data).Count) keys=" + (Test-Keys $first @('courseName', 'totalCount', 'avgScore', 'satisfactionRate')))
        $r2 = Invoke-RestMethod -Uri "$base/portal/evaluation/myReport" -Headers $TH
        Step 'GET /portal/evaluation/myReport (teacher)' ($r2.code -eq 200) ("dataNull=$(($r2.data -eq $null))")
    } catch { Step 'GET /portal/evaluation/teacherResults (teacher)' $false $_.Exception.Message }
}
try {
    if (-not $courseId) {
        # resolve a real courseId from the admin-visible result list so comments endpoint is always covered
        $rl = Invoke-RestMethod -Uri "$base/portal/evaluation/resultList?pageSize=50" -Headers $H
        $courseId = @($rl.rows | Where-Object { $_.courseId } | Select-Object -First 1).courseId
    }
    $cid = if ($courseId) { $courseId } else { 0 }   # no seeded courseId -> still assert endpoint contract (empty list)
    $r = Invoke-RestMethod -Uri "$base/portal/evaluation/comments/$cid" -Headers $H
    Step 'GET /portal/evaluation/comments/{courseId}' ($r.code -eq 200) ("code=$($r.code) courseId=$cid rows=$( @($r.data).Count )")
} catch { Step 'GET /portal/evaluation/comments/{courseId}' $false $_.Exception.Message }

# ---- studentStatus (self info + my change list) ----
try {
    $r = Invoke-RestMethod -Uri "$base/portal/studentStatus/info" -Headers $H
    $u = $r.data
    Step 'GET /portal/studentStatus/info' ($r.code -eq 200) ("keys=" + (Test-Keys $u @('enrollmentYear', 'studentStatus', 'deptName', 'majorName', 'className')))
} catch { Step 'GET /portal/studentStatus/info' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/studentStatus/changeList?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/studentStatus/changeList' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('changeType', 'reason', 'changeDate', 'approveStatus', 'approveOpinion')))
} catch { Step 'GET /portal/studentStatus/changeList' $false $_.Exception.Message }

# ---- exam / grade / selection / teachingTask / schedule ----
try {
    $r = Invoke-RestMethod -Uri "$base/aem/examPlan/list?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /aem/examPlan/list (new courseName join)' ($r.code -eq 200 -and ($null -eq $row -or $row.PSObject.Properties.Name -contains 'courseName')) ("total=$($r.total) keys=" + (Test-Keys $row @('courseName', 'examDate', 'examType', 'duration', 'planStatus')))
} catch { Step 'GET /aem/examPlan/list (new courseName join)' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/exam/list?pageSize=5" -Headers $H
    Step 'GET /portal/exam/list' ($r.code -eq 200) "total=$($r.total)"
} catch { Step 'GET /portal/exam/list' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/exam/invigilationList?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/exam/invigilationList' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('dutyType', 'examName', 'classroomName')))
} catch { Step 'GET /portal/exam/invigilationList' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/grade/list?pageSize=5" -Headers $H
    Step 'GET /portal/grade/list' ($r.code -eq 200) "total=$($r.total)"
} catch { Step 'GET /portal/grade/list' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/grade/entryList?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/grade/entryList' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('courseName', 'examType', 'examScore', 'totalScore', 'gradePoint')))
} catch { Step 'GET /portal/grade/entryList' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/selection/courseList?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/selection/courseList' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('courseName', 'credit', 'teacherName', 'maxStudents', 'enrolledCount', 'offeringStatus')))
} catch { Step 'GET /portal/selection/courseList' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/teachingTask/list?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/teachingTask/list' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('courseName', 'classCount', 'semesterName')))
} catch { Step 'GET /portal/teachingTask/list' $false $_.Exception.Message }
try {
    $r = Invoke-RestMethod -Uri "$base/portal/schedule/teacherList?pageSize=5" -Headers $H
    $row = $r.rows | Select-Object -First 1
    Step 'GET /portal/schedule/teacherList' ($r.code -eq 200) ("total=$($r.total) keys=" + (Test-Keys $row @('weekDay', 'startPeriod', 'endPeriod', 'startWeek', 'endWeek')))
} catch { Step 'GET /portal/schedule/teacherList' $false $_.Exception.Message }

$results | Format-Table -AutoSize -Wrap
$fail = @($results | Where-Object { $_.Result -eq 'FAIL' }).Count
Write-Host ("SUMMARY: {0}/{1} PASS" -f ($results.Count - $fail), $results.Count)
if ($fail -gt 0) { exit 1 } else { exit 0 }
