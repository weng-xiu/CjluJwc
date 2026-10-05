# K3 MFA 端到端冒烟：绑定→二维码→确认→二次鉴别登录→错误口令拒绝→解绑复原
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$results = @()
function Step($name, $ok, $detail) {
    $script:results += [pscustomobject]@{ Step = $name; Result = ($(if($ok){'PASS'}else{'FAIL'})); Detail = $detail }
}
function TotpCode($secret, $offset) {
    return (& python "$PSScriptRoot\..\build-temp\mfa_totp.py" $secret $offset).Trim()
}
function PostJson($url, $obj, $headers) {
    $json = $obj | ConvertTo-Json -Compress
    return Invoke-RestMethod -Uri $url -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($json)) -ContentType 'application/json' -Headers $headers -TimeoutSec 15
}

# 0) 基线登录（未启用 MFA 时应成功）
$r = PostJson "$base/login" @{ username='admin'; password='admin123'; code=''; uuid='' } $null
$token = $r.token
Step 'login (no mfa)' ($r.code -eq 200 -and $token) "code=$($r.code)"
$H = @{ Authorization = "Bearer $token" }

# 1) 发起绑定
$r = PostJson "$base/mfa/bind" @{} $H
$secret = $r.secret
Step 'POST /mfa/bind' ($r.code -eq 200 -and $secret) "secret=$secret uri=$($r.otpauthUri)"

# 2) 二维码（PNG base64，魔数 iVBOR 开头）
$r = Invoke-RestMethod -Uri "$base/mfa/qrcode" -Headers $H -TimeoutSec 15
$qrOk = ($r.code -eq 200) -and $r.img -and $r.img.StartsWith('iVBOR')
Step 'GET /mfa/qrcode' $qrOk "imgLen=$($r.img.Length)"

# 3) 错误口令确认应被拒
$r = PostJson "$base/mfa/confirm" @{ code='000000' } $H
Step 'confirm wrong code rejected' ($r.code -ne 200) "msg=$($r.msg)"

# 4) 正确口令确认启用
$code = TotpCode $secret 0
$r = PostJson "$base/mfa/confirm" @{ code=$code } $H
Step 'POST /mfa/confirm' ($r.code -eq 200) "code=$code msg=$($r.msg)"

# 5) 状态查询 enabled=true
$r = Invoke-RestMethod -Uri "$base/mfa/status" -Headers $H -TimeoutSec 15
Step 'GET /mfa/status enabled' ($r.code -eq 200 -and $r.enabled -eq $true) "enabled=$($r.enabled) bindTime=$($r.bindTime)"

# 6) 启用后不带口令登录应被拒（MFA 提示）
$r = PostJson "$base/login" @{ username='admin'; password='admin123'; code=''; uuid='' } $null
Step 'login without totp rejected' ($r.code -ne 200 -and $r.msg -match 'MFA') "msg=$($r.msg)"

# 7) 错误口令登录应被拒
$r = PostJson "$base/login" @{ username='admin'; password='admin123'; code=''; uuid=''; totpCode='123456' } $null
Step 'login wrong totp rejected' ($r.code -ne 200 -and $r.msg -match 'MFA') "msg=$($r.msg)"

# 8) 正确口令登录成功
$code = TotpCode $secret 0
$r = PostJson "$base/login" @{ username='admin'; password='admin123'; code=''; uuid=''; totpCode=$code } $null
Step 'login with totp ok' ($r.code -eq 200 -and $r.token) "code=$code token=$([bool]$r.token)"
$H2 = @{ Authorization = "Bearer $($r.token)" }

# 9) 解绑（需口令）
$code = TotpCode $secret 0
$r = PostJson "$base/mfa/unbind" @{ code=$code } $H2
Step 'POST /mfa/unbind' ($r.code -eq 200) "msg=$($r.msg)"

# 10) 复原：无口令登录恢复成功
$r = PostJson "$base/login" @{ username='admin'; password='admin123'; code=''; uuid='' } $null
Step 'login restored (unbound)' ($r.code -eq 200 -and $r.token) "code=$($r.code)"

Write-Host ''
$results | Format-Table -AutoSize
$pass = ($results | Where-Object Result -eq 'PASS').Count
Write-Host ("SUMMARY: {0}/{1} PASS" -f $pass, $results.Count)
if ($pass -ne $results.Count) { exit 1 }
