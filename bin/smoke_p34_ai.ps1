$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'

function J($o, $depth) { return ($o | ConvertTo-Json -Depth $depth -Compress) }

Write-Output "==== 1. admin login ===="
$login = Invoke-RestMethod -Uri "$base/login" -Method POST -ContentType 'application/json' -Body (@{username='admin';password='admin123';code='';uuid=''} | ConvertTo-Json)
Write-Output "TOKEN code=$($login.code)"
$h = @{ Authorization = "Bearer $($login.token)" }

Write-Output "`n==== 2. GET /system/aiKnowledge/list ===="
$r = Invoke-RestMethod -Uri "$base/system/aiKnowledge/list?pageNum=1&pageSize=3" -Headers $h
Write-Output "total=$($r.total) first=$($r.rows[0].title) summaryLen=$($r.rows[0].summary.Length)"

Write-Output "`n==== 3. GET /system/aiKnowledge/engine ===="
$r = Invoke-RestMethod -Uri "$base/system/aiKnowledge/engine" -Headers $h
Write-Output (J $r.data 4)

Write-Output "`n==== 4. POST /system/aiKnowledge/ask (hit) ===="
$r = Invoke-RestMethod -Uri "$base/system/aiKnowledge/ask" -Method POST -Headers $h -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes((@{question='选课时间怎么安排，能退课吗'} | ConvertTo-Json -Compress)))
Write-Output "code=$($r.code) source=$($r.data.answerSource) conf=$($r.data.confidence) refs=$($r.data.references.Count)"
Write-Output ("ANSWER=" + ($r.data.answer -replace "`r?`n", " / "))
foreach ($ref in $r.data.references) { Write-Output ("REF  " + $ref.title + " score=" + $ref.score + " on=" + $ref.matchedOn) }

Write-Output "`n==== 5. POST /system/aiKnowledge/ask (miss) ===="
$r = Invoke-RestMethod -Uri "$base/system/aiKnowledge/ask" -Method POST -Headers $h -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes((@{question='学校食堂几点开门'} | ConvertTo-Json -Compress)))
Write-Output "source=$($r.data.answerSource) refs=$($r.data.references.Count)"
Write-Output ("ANSWER=" + ($r.data.answer -replace "`r?`n", " / "))

Write-Output "`n==== 6. GET /system/aiKnowledge/stat ===="
$r = Invoke-RestMethod -Uri "$base/system/aiKnowledge/stat" -Headers $h
Write-Output "categoryStat=$($r.data.categoryStat.Count) hotKnowledge=$($r.data.hotKnowledge.Count)"
Write-Output ("HOT1=" + ($r.data.hotKnowledge[0] | ConvertTo-Json -Compress))

Write-Output "`n==== 7. GET /system/aiChat/stat ===="
$r = Invoke-RestMethod -Uri "$base/system/aiChat/stat?days=14&unmatchedLimit=10" -Headers $h
Write-Output ("OVERVIEW=" + (J $r.data.overview 4))
Write-Output ("SOURCE=" + (J $r.data.sourceStat 4))
Write-Output ("SCENE=" + (J $r.data.sceneStat 4))
Write-Output ("TREND=" + (J $r.data.trend 4))

Write-Output "`n==== 8. GET /system/aiChat/list ===="
$r = Invoke-RestMethod -Uri "$base/system/aiChat/list?pageNum=1&pageSize=3" -Headers $h
Write-Output "total=$($r.total)"
foreach ($row in $r.rows) { Write-Output ("REC " + $row.recordId + " " + $row.userName + " " + $row.scene + " " + $row.answerSource + " conf=" + $row.confidence + " q=" + $row.question) }

Write-Output "`n==== 9. student login ===="
$slogin = Invoke-RestMethod -Uri "$base/login" -Method POST -ContentType 'application/json' -Body (@{username='wxl';password='admin123';code='';uuid=''} | ConvertTo-Json)
Write-Output "code=$($slogin.code) user=$($slogin.username)"
$sh = @{ Authorization = "Bearer $($slogin.token)" }
if (-not $slogin.token) { Write-Output "STUDENT LOGIN FAILED, abort portal checks"; exit 1 }

Write-Output "`n==== 10. GET /portal/ai/engine + suggest ===="
$r = Invoke-RestMethod -Uri "$base/portal/ai/engine" -Headers $sh
Write-Output (J $r.data 4)
$r = Invoke-RestMethod -Uri "$base/portal/ai/suggest?limit=5" -Headers $sh
Write-Output ("SUGGEST=" + (J $r.data 3))

Write-Output "`n==== 11. POST /portal/ai/ask ===="
$r = Invoke-RestMethod -Uri "$base/portal/ai/ask" -Method POST -Headers $sh -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes((@{question='成绩及格线是多少，不及格怎么办'} | ConvertTo-Json -Compress)))
Write-Output "source=$($r.data.answerSource) conf=$($r.data.confidence) refs=$($r.data.references.Count)"
Write-Output ("ANSWER=" + ($r.data.answer -replace "`r?`n", " / "))

Write-Output "`n==== 12. GET /portal/ai/recommend ===="
$r = Invoke-RestMethod -Uri "$base/portal/ai/recommend" -Headers $sh
$d = $r.data
Write-Output ("student=" + $d.student.studentName + " round=" + $d.round.roundName + " available=" + $d.available)
Write-Output ("quota=" + (J $d.quota 3))
Write-Output ("selectedCount=" + $d.selected.Count + " itemsCount=" + $d.items.Count + " excludedCount=" + $d.excluded.Count)
foreach ($it in $d.items) { Write-Output ("ITEM " + $it.rank + " " + $it.courseName + " credit=" + $it.credit + " score=" + $it.score + " signals=" + $it.signals.Count + " reason=" + ($it.reasons -join ' | ')) }
foreach ($ex in $d.excluded) { Write-Output ("EXCL " + $ex.courseName + " -> " + ($ex.reasons -join ' | ')) }
Write-Output ("SIGNALNOTE=" + $d.signalNote)

Write-Output "`n==== 13. GET /portal/ai/portrait ===="
$r = Invoke-RestMethod -Uri "$base/portal/ai/portrait" -Headers $sh
$d = $r.data
Write-Output ("available=" + $d.available + " plan=" + $d.plan.planName + " totalCredits=" + $d.plan.totalCredits)
Write-Output ("gradeSummary=" + (J $d.gradeSummary 3))
Write-Output ("peerSummary=" + (J $d.peerSummary 3))
Write-Output ("overallScore=" + $d.overallScore + " modules=" + $d.modules.Count + " grades=" + $d.grades.Count + " warnings=" + $d.warnings.Count + " categoryScores=" + $d.categoryScores.Count)
foreach ($dim in $d.dims) { Write-Output ("DIM " + $dim.code + " " + $dim.name + " = " + $dim.value + "  " + $dim.text) }
foreach ($m in $d.modules) { Write-Output ("MOD " + $m.creditTypeName + " req=" + $m.requiredCredit + " earned=" + $m.earnedCredit + " rate=" + $m.rate) }
foreach ($tip in $d.suggestions) { Write-Output ("TIP " + $tip) }
Write-Output ("NOTE=" + $d.note)
