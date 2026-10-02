$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8080'

function Login($u, $p) {
  $body = @{ username = $u; password = $p } | ConvertTo-Json
  try {
    $r = Invoke-RestMethod -Uri "$base/login" -Method Post -Body $body -ContentType 'application/json;charset=utf-8'
    if ($r.code -eq 200) { return $r.token } else { Write-Host "  LOGIN FAIL $u => code=$($r.code) msg=$($r.msg)"; return $null }
  } catch { Write-Host "  LOGIN ERR $u => $_"; return $null }
}

function Hit($token, $name, $method, $url, $bodyObj) {
  $headers = @{ Authorization = "Bearer $token" }
  try {
    if ($method -eq 'get') {
      $r = Invoke-RestMethod -Uri "$base$url" -Method Get -Headers $headers
    } else {
      $json = $bodyObj | ConvertTo-Json
      $r = Invoke-RestMethod -Uri "$base$url" -Method Post -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($json)) -ContentType 'application/json;charset=utf-8'
    }
    $preview = ''
    if ($r.data) {
      if ($r.data.available -ne $null) { $preview = "available=$($r.data.available)" }
      elseif ($r.data.items) { $preview = "items=$($r.data.items.Count)" }
      elseif ($r.data.items) { $preview = "items=$($r.data.items.Count)" }
      elseif ($r.data -is [Array]) { $preview = "len=$($r.data.Count)" }
      elseif ($r.data.summary) { $preview = "willGraduate=$($r.data.willGraduate)" }
      else { $preview = 'obj' }
    } elseif ($r.total -ne $null) { $preview = "total=$($r.total)" }
    Write-Host ("  {0,-16} {1,-4} {2,-42} => code={3} {4}" -f $name, $method, $url, $r.code, $preview)
    return $r
  } catch {
    Write-Host ("  {0,-16} {1,-4} {2,-42} => EXCEPTION $_" -f $name, $method, $url)
    return $null
  }
}

Write-Host "`n===== STUDENT (wxl) ====="
$st = Login 'wxl' 'admin123'
if (-not $st) { $st = Login 'wxl' '123456' }
if ($st) {
  Hit $st 'graduation' 'get' '/portal/graduation/preReview' $null
  Hit $st 'ai:engine'  'get' '/portal/ai/engine' $null
  Hit $st 'ai:suggest' 'get' '/portal/ai/suggest?limit=6' $null
  Hit $st 'ai:ask'     'post' '/portal/ai/ask' @{ question = 'what is the course selection time' }
  Hit $st 'ai:recommend' 'get' '/portal/ai/recommend' $null
  Hit $st 'ai:portrait'  'get' '/portal/ai/portrait' $null
  Hit $st 'thesis:my'    'get' '/portal/thesis/my' $null
  Hit $st 'thesis:topics' 'get' '/portal/thesis/topics' $null
  Hit $st 'thesis:degree' 'get' '/portal/thesis/degreePreview' $null
} else { Write-Host '  (student login failed, skip)' }

Write-Host "`n===== TEACHER (2001) ====="
$te = Login '2001' 'admin123'
if ($te) {
  Hit $te 'borrow:classrooms' 'get' '/portal/borrow/classrooms?pageNum=1&pageSize=5' $null
  Hit $te 'borrow:myList'     'get' '/portal/borrow/myList?pageNum=1&pageSize=5' $null
  Hit $te 'thesis:advisorList' 'get' '/portal/thesis/advisorList?pageNum=1&pageSize=5' $null
} else { Write-Host '  (teacher login failed, skip)' }

Write-Host "`n===== ADMIN ====="
$ad = Login 'admin' 'admin123'
if ($ad) {
  Hit $ad 'borrow:myList' 'get' '/portal/borrow/myList?pageNum=1&pageSize=5' $null
  Hit $ad 'thesis:advisorList' 'get' '/portal/thesis/advisorList?pageNum=1&pageSize=5' $null
  Hit $ad 'ai:ask' 'post' '/portal/ai/ask' @{ question = 'graduation credit requirement' }
}
Write-Host "`nDONE"
