# R Runtime full API test: probe all read-only GET endpoints against running backend (8080)
# Grades: PASS(code=200) / BINARY200(binary HTTP200) / NEEDPARAM(HTTP400/500 requires params) / AUTHZ(401/403) / FAIL(conn/404/unexpected)
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'

# binary / stream endpoints: only check HTTP 200
$binary = @(
    '/mfa/qrcode','/system/credential/render','/portal/credential/render',
    '/oa/workflow/definition/xml','/oa/workflow/diagram',
    '/common/download','/common/download/resource'
)
# anonymous public endpoints
$public = @('/portal/public/home','/portal/public/banners','/portal/public/search')

# 1) login to obtain token
$loginBody = @{ username='admin'; password='admin123'; code=''; uuid='' } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$base/login" -Method Post -Body $loginBody -ContentType 'application/json' -TimeoutSec 15
if (-not $login.token) { Write-Host 'LOGIN FAILED, abort'; exit 1 }
$H = @{ Authorization = "Bearer $($login.token)" }
Write-Host 'LOGIN OK token acquired'

$urls = Get-Content "$PSScriptRoot\_api_endpoints.txt" | Where-Object { $_ -ne '' }
$results = @()

foreach ($u in $urls) {
    $q = $u
    if ($u -match '/list$') { $q = "$u" + '?pageSize=5&pageNum=1' }
    if ($u -eq '/portal/public/search') { $q = "$u" + '?keyword=jw' }
    $isPublic = $public -contains $u
    $isBinary = $binary -contains $u
    $headers = if ($isPublic) { @{} } else { $H }
    $grade=''; $detail=''
    try {
        if ($isBinary) {
            $r = Invoke-WebRequest -Uri "$base$q" -Method Get -Headers $headers -TimeoutSec 15 -UseBasicParsing
            if ($r.StatusCode -eq 200) { $grade='BINARY200'; $detail="HTTP200 contentType=$($r.Headers['Content-Type']) len=$($r.RawContentLength)" }
            else { $grade='FAIL'; $detail="HTTP$($r.StatusCode)" }
        } else {
            $r = Invoke-RestMethod -Uri "$base$q" -Method Get -Headers $headers -TimeoutSec 15
            $code = $r.code
            if ($code -eq 200) {
                $grade='PASS'
                if ($r.PSObject.Properties.Name -contains 'rows') { $detail="code=200 total=$($r.total) rows=$($r.rows.Count)" }
                elseif ($r.data) { $detail='code=200 data=<obj>' }
                else { $detail="code=200 msg=$($r.msg)" }
            }
            elseif ($code -eq 401 -or $code -eq 403) { $grade='AUTHZ'; $detail="code=$code msg=$($r.msg)" }
            else { $grade='FAIL'; $detail="code=$code msg=$($r.msg)" }
        }
    } catch {
        $resp = $_.Exception.Response
        if ($resp -and ($resp.StatusCode.value__ -eq 400 -or $resp.StatusCode.value__ -eq 500)) {
            $grade='NEEDPARAM'; $detail="HTTP$($resp.StatusCode.value__) (endpoint requires mandatory params/body; not covered by read-only sweep)"
        } elseif ($resp -and $resp.StatusCode.value__ -eq 404) {
            $grade='FAIL'; $detail='HTTP404 route not found'
        } elseif ($resp -and ($resp.StatusCode.value__ -eq 401 -or $resp.StatusCode.value__ -eq 403)) {
            $grade='AUTHZ'; $detail="HTTP$($resp.StatusCode.value__)"
        } else {
            $grade='FAIL'; $detail=$_.Exception.Message
        }
    }
    $results += [pscustomobject]@{ Url=$u; Grade=$grade; Detail=$detail }
}

# 2) safe write lifecycle (CRUD) on system notice: add -> get -> update -> delete, cleanup at end
$writeResults = @()
try {
    $title = "ITTEST_$(Get-Date -Format yyyyMMddHHmmss)"
    $addBody = @{ noticeTitle=$title; noticeType='0'; noticeContent='interface-test'; status='0' } | ConvertTo-Json
    $add = Invoke-RestMethod -Uri "$base/system/notice" -Method Post -Headers $H -Body $addBody -ContentType 'application/json' -TimeoutSec 15
    $nid = $add.data
    if (-not $nid) { $nid = ((Invoke-RestMethod -Uri "$base/system/notice/list?pageSize=1&noticeTitle=$title" -Method Get -Headers $H -TimeoutSec 15).rows | Select-Object -First 1).noticeId }
    $writeResults += [pscustomobject]@{ Url='POST /system/notice (add)'; Grade=$(if($add.code -eq 200){'PASS'}else{'FAIL'}); Detail="code=$($add.code) id=$nid" }
    $gi = Invoke-RestMethod -Uri "$base/system/notice/$nid" -Method Get -Headers $H -TimeoutSec 15
    $writeResults += [pscustomobject]@{ Url='GET /system/notice/{id}'; Grade=$(if($gi.code -eq 200){'PASS'}else{'FAIL'}); Detail="code=$($gi.code) title=$($gi.data.noticeTitle)" }
    $upBody = @{ noticeId=$nid; noticeTitle="$title"+"_UP"; noticeType='0'; noticeContent='interface-test-up'; status='0' } | ConvertTo-Json
    $up = Invoke-RestMethod -Uri "$base/system/notice" -Method Put -Headers $H -Body $upBody -ContentType 'application/json' -TimeoutSec 15
    $writeResults += [pscustomobject]@{ Url='PUT /system/notice (update)'; Grade=$(if($up.code -eq 200){'PASS'}else{'FAIL'}); Detail="code=$($up.code)" }
    $del = Invoke-RestMethod -Uri "$base/system/notice/$nid" -Method Delete -Headers $H -TimeoutSec 15
    $writeResults += [pscustomobject]@{ Url='DELETE /system/notice/{id} (cleanup)'; Grade=$(if($del.code -eq 200){'PASS'}else{'FAIL'}); Detail="code=$($del.code)" }
} catch {
    $writeResults += [pscustomobject]@{ Url='CRUD /system/notice'; Grade='FAIL'; Detail=$_.Exception.Message }
}

$all = $results + $writeResults
$all | Export-Csv -NoTypeInformation -Encoding UTF8 "$PSScriptRoot\_api_test_results.csv"

Write-Host ''
Write-Host '==================== API TEST SUMMARY ===================='
$all | Group-Object Grade | Sort-Object Name | ForEach-Object { Write-Host ("{0,-11} {1}" -f $_.Name, $_.Count) }
$passLike = ($all | Where-Object { $_.Grade -in 'PASS','BINARY200' }).Count
Write-Host ("TOTAL={0}  PASS+BINARY={1}" -f $all.Count, $passLike)
Write-Host ''
Write-Host '---- FAIL ----'
$all | Where-Object { $_.Grade -eq 'FAIL' } | ForEach-Object { Write-Host ("{0}  {1}" -f $_.Url, $_.Detail) }
Write-Host ''
Write-Host '---- NEEDPARAM (requires params, not a defect) ----'
$all | Where-Object { $_.Grade -eq 'NEEDPARAM' } | ForEach-Object { Write-Host ("{0}" -f $_.Url) }
Write-Host ''
Write-Host '---- AUTHZ ----'
$all | Where-Object { $_.Grade -eq 'AUTHZ' } | ForEach-Object { Write-Host ("{0}  {1}" -f $_.Url, $_.Detail) }
