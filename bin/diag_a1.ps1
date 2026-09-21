$ErrorActionPreference='Continue'
$base='http://localhost:8080'
$login=Invoke-RestMethod -Uri "$base/login" -Method POST -ContentType 'application/json' -Body (@{username='admin';password='admin123';code='';uuid=''} | ConvertTo-Json)
$h=@{Authorization="Bearer $($login.token)"}
Write-Output "classroom list total:"
$cl=Invoke-RestMethod -Uri "$base/brm/classroom/list?pageSize=100&pageNum=1" -Headers $h
Write-Output ("total="+$cl.total)
$cl.rows | ForEach-Object { Write-Output ("  id="+$_.classroomId+" cap="+$_.capacity+" status="+$_.status+" name="+$_.classroomName) }
Write-Output "`nautoArrange 9001:"
(Invoke-RestMethod -Uri "$base/aem/examPlan/autoArrange/9001" -Method POST -Headers $h) | ConvertTo-Json -Compress -Depth 6
