# ============================================================
# verify-portal-mobile.ps1 — 门户移动端三页接口全链路验证
# 覆盖：登录(带验证码) -> 考试列表/筛选 -> 监考列表(联名字) -> 评教问卷/题目/提交/防重复
# 用法：.\verify-portal-mobile.ps1 -TeacherPwd 123456 -StudentPwd 123456
# ============================================================
param(
    [string]$BaseUrl    = 'http://localhost:8080',
    [string]$TeacherUser = '2001',
    [string]$StudentUser = 'wxl',
    [string]$TeacherPwd  = '',
    [string]$StudentPwd  = ''
)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$redisCli = Join-Path $PSScriptRoot 'redis\redis-cli.exe'

function Get-CaptchaToken([string]$user, [string]$pass) {
    $cap = Invoke-RestMethod -Uri "$BaseUrl/captchaImage" -Method Get
    $body = @{ username = $user; password = $pass }
    if ($cap.captchaEnabled) {
        if (-not $pass) { throw "验证码开启且未提供 $user 的密码参数" }
        $code = (& $redisCli -h 127.0.0.1 --no-auth-warning get "captcha_codes:$($cap.uuid)").Trim('"')
        if (-not $code) { throw "未能从 Redis 读取验证码 uuid=$($cap.uuid)" }
        $body.code = $code; $body.uuid = $cap.uuid
    }
    $r = Invoke-RestMethod -Uri "$BaseUrl/login" -Method Post -ContentType 'application/json' -Body ($body | ConvertTo-Json)
    if ($r.code -ne 200) { throw "登录失败($user): $($r.msg)" }
    return $r.token
}

function Invoke-ApiJson([string]$token, [string]$method, [string]$uri, $bodyObj) {
    # PS5.1 的 Invoke-RestMethod 会把无 charset 的 JSON 按 ISO-8859-1 解码，这里手动按 UTF-8 解析
    $headers = @{ Authorization = "Bearer $token" }
    if ($bodyObj) {
        $resp = Invoke-WebRequest -Uri "$BaseUrl$uri" -Method $method -Headers $headers -ContentType 'application/json' -Body ($bodyObj | ConvertTo-Json -Depth 6) -UseBasicParsing
    } else {
        $resp = Invoke-WebRequest -Uri "$BaseUrl$uri" -Method $method -Headers $headers -UseBasicParsing
    }
    $json = [Text.Encoding]::UTF8.GetString($resp.RawContentStream.ToArray())
    return $json | ConvertFrom-Json
}

Write-Host '===== 1. 教师登录与监考列表 ====='
$pwd2 = if ($TeacherPwd) { $TeacherPwd } else { '123456' }
$tToken = Get-CaptchaToken $TeacherUser $pwd2
Write-Host "教师 $($TeacherUser) 登录 OK"
$invig = Invoke-ApiJson $tToken Get '/portal/exam/invigilationList?pageNum=1&pageSize=10'
Write-Host "监考列表 total=$($invig.total)"
$invig.rows | Select-Object -First 3 | ForEach-Object { "  #{0} {1} duty={2} 教室={3} 时段={4}-{5}" -f $_.invigilationId, $_.examName, $_.dutyType, $_.classroomName, $_.startTime, $_.endTime }
$invigF = Invoke-ApiJson $tToken Get '/portal/exam/invigilationList?pageNum=1&pageSize=10&dutyType=0'
Write-Host "  dutyType=0 筛选 total=$($invigF.total) (应为全量中主监考条数)"

Write-Host '===== 2. 学生登录与考试列表 ====='
$pwd3 = if ($StudentPwd) { $StudentPwd } else { '123456' }
$sToken = Get-CaptchaToken $StudentUser $pwd3
Write-Host "学生 $($StudentUser) 登录 OK"
$exams = Invoke-ApiJson $sToken Get '/portal/exam/list?pageNum=1&pageSize=10'
Write-Host "考试列表 total=$($exams.total)"
$exams.rows | Select-Object -First 3 | ForEach-Object { "  #{0} {1} type={2} planStatus={3} {4} {5}-{6}" -f $_.examId, $_.examName, $_.examType, $_.planStatus, $_.examDate, $_.startTime, $_.endTime }
$examF = Invoke-ApiJson $sToken Get '/portal/exam/list?pageNum=1&pageSize=10&examType=1'
Write-Host "  examType=1(补考) 筛选 total=$($examF.total)"

Write-Host '===== 3. 评教：问卷列表 / 题目 / 提交 / 防重复 ====='
$qns = Invoke-ApiJson $sToken Get '/portal/evaluation/questionnaireList?pageNum=1&pageSize=10'
Write-Host "问卷列表 total=$($qns.total)"
$qns.rows | ForEach-Object { "  #{0} [{1}] completed={2} {3}" -f $_.questionnaireId, $_.evalStatus, $_.completed, $_.title }
$qs = Invoke-ApiJson $sToken Get '/portal/evaluation/questions/9001'
Write-Host "问卷9001题目数=$($qs.data.Count)"
# 组装提交：5 道评分题(4,5,3,4,5 -> 平均80分) + 文本题并入 comment
$answers = @(4,5,3,4,5)
$i = 0
$sum = 0; $cnt = 0
foreach ($sc in $answers) { $sum += $sc; $cnt++ }
$totalScore = [math]::Round(($sum / ($cnt * 5)) * 100, 1)
$payload = @{ questionnaireId = 9001; totalScore = $totalScore; comment = "希望增加课堂互动（自动化验证 $(Get-Random))" }
$sub1 = Invoke-ApiJson $sToken Post '/portal/evaluation/submit' $payload
Write-Host "首次提交: code=$($sub1.code) msg=$($sub1.msg)"
$sub2 = Invoke-ApiJson $sToken Post '/portal/evaluation/submit' $payload
Write-Host "重复提交: code=$($sub2.code) msg=$($sub2.msg) (应为 601 防重复提示)"
$qns2 = Invoke-ApiJson $sToken Get '/portal/evaluation/questionnaireList?pageNum=1&pageSize=10'
Write-Host "提交后问卷状态:"
$qns2.rows | ForEach-Object { "  #{0} [{1}] completed={2}" -f $_.questionnaireId, $_.evalStatus, $_.completed }

Write-Host '===== ALL DONE ====='
