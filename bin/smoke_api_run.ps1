# Q 接口冒烟测试：验证运行态后端关键 API 契约
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$results = @()

function Step($name, $ok, $detail) {
    $script:results += [pscustomobject]@{ Step = $name; Result = ($(if($ok){'PASS'}else{'FAIL'})); Detail = $detail }
}

# 1) 验证码配置（匿名）
try {
    $cap = Invoke-RestMethod -Uri "$base/captchaImage" -Method Get -TimeoutSec 10
    $captchaOn = $true
    if ($cap.PSObject.Properties.Name -contains 'captchaEnabled') { $captchaOn = $cap.captchaEnabled }
    Step 'GET /captchaImage' ($cap.code -eq 200) "code=$($cap.code) captchaEnabled=$captchaOn uuid=$([bool]$cap.uuid)"
} catch { Step 'GET /captchaImage' $false $_.Exception.Message; $captchaOn = $true }

# 2) 登录（admin）
$token = $null
$loginBody = @{ username = 'admin'; password = 'admin123'; code = ''; uuid = '' } | ConvertTo-Json
if ($captchaOn) { $loginBody = @{ username = 'admin'; password = 'admin123'; code = '0'; uuid = '' } | ConvertTo-Json }
try {
    $login = Invoke-RestMethod -Uri "$base/login" -Method Post -Body $loginBody -ContentType 'application/json' -TimeoutSec 10
    $token = $login.token
    Step 'POST /login (admin)' ($login.code -eq 200 -and $token) "code=$($login.code) token=$([bool]$token) msg=$($login.msg)"
} catch { Step 'POST /login (admin)' $false $_.Exception.Message }

$H = @{ Authorization = "Bearer $token" }

# 3) getInfo（登录用户信息，鉴权）
try {
    $gi = Invoke-RestMethod -Uri "$base/getInfo" -Method Get -Headers $H -TimeoutSec 10
    Step 'GET /getInfo' ($gi.code -eq 200) "code=$($gi.code) user=$($gi.user.userName) roles=$($gi.roles -join ',') perms=$($gi.permissions.Count)"
} catch { Step 'GET /getInfo' $false $_.Exception.Message }

# 4) getRouters（菜单路由，鉴权）
try {
    $rt = Invoke-RestMethod -Uri "$base/getRouters" -Method Get -Headers $H -TimeoutSec 10
    Step 'GET /getRouters' ($rt.code -eq 200) "code=$($rt.code) topRoutes=$($rt.data.Count)"
} catch { Step 'GET /getRouters' $false $_.Exception.Message }

# 5) 系统管理高频列表接口（鉴权，验证分页 TableDataInfo 契约）
$apis = @(
    '/system/user/list?pageSize=5',
    '/system/role/list?pageSize=5',
    '/system/menu/list',
    '/system/dept/list',
    '/system/dict/data/list?dictType=sys_user_sex',
    '/system/config/list?pageSize=5',
    '/system/notice/list?pageSize=5',
    '/system/printTemplate/list?pageSize=5',
    '/system/credential/list?pageSize=5',
    '/system/msgCenter/message/unreadCount'
)
foreach ($api in $apis) {
    try {
        $r = Invoke-RestMethod -Uri "$base$api" -Method Get -Headers $H -TimeoutSec 10
        $isTable = $r.PSObject.Properties.Name -contains 'rows'
        $okCode = ($r.code -eq 200)
        $d = if ($isTable) { "code=$($r.code) total=$($r.total) rows=$($r.rows.Count)" } else { "code=$($r.code) data=$($r.data)" }
        Step "GET $api" $okCode $d
    } catch { Step "GET $api" $false $_.Exception.Message }
}

# 6) 门户公开接口（匿名）
foreach ($api in @('/portal/public/home','/portal/public/banners')) {
    try {
        $r = Invoke-RestMethod -Uri "$base$api" -Method Get -TimeoutSec 10
        Step "GET $api (anon)" ($r.code -eq 200) "code=$($r.code)"
    } catch { Step "GET $api (anon)" $false $_.Exception.Message }
}

Write-Host ''
$results | Format-Table -AutoSize
$pass = ($results | Where-Object Result -eq 'PASS').Count
Write-Host ("SUMMARY: {0}/{1} PASS" -f $pass, $results.Count)
