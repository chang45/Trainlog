# TrainLog 接口冒烟测试：需要后端以 local 配置启动（验证码会写入 app.log）
param(
    [string]$BaseUrl = 'http://localhost:8081',
    [string]$LogPath = (Join-Path (Split-Path -Parent $PSScriptRoot) 'backend\app.log')
)

$ErrorActionPreference = 'Stop'
$script:passed = 0
$script:failed = 0

function Check([string]$name, [bool]$ok, [string]$detail) {
    if ($ok) {
        $script:passed++
        Write-Host "[PASS] $name" -ForegroundColor Green
    } else {
        $script:failed++
        Write-Host "[FAIL] $name -> $detail" -ForegroundColor Red
    }
}

function New-Captcha {
    $res = Invoke-RestMethod -Uri "$BaseUrl/api/captcha" -TimeoutSec 10
    Start-Sleep -Milliseconds 250
    $line = (Select-String -Path $LogPath -Pattern $res.data.captchaId -ErrorAction SilentlyContinue | Select-Object -Last 1).Line
    $code = 'AAAA'
    if ($line -match 'code=([A-Za-z0-9]{4})') { $code = $matches[1] }
    return [pscustomobject]@{ id = $res.data.captchaId; code = $code; image = $res.data.image }
}

function Invoke-Json([string]$uri, [string]$method, $body, $headers) {
    $params = @{ Uri = $uri; Method = $method; TimeoutSec = 15 }
    if ($headers) { $params.Headers = $headers }
    if ($null -ne $body) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = [Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Compress))
    }
    try {
        $res = Invoke-RestMethod @params
        return [pscustomobject]@{ status = 200; body = $res }
    } catch {
        $resp = $_.Exception.Response
        if (-not $resp) { return [pscustomobject]@{ status = -1; body = $null } }
        $reader = New-Object IO.StreamReader($resp.GetResponseStream(), [Text.Encoding]::UTF8)
        $text = $reader.ReadToEnd()
        $parsed = $null
        try { $parsed = $text | ConvertFrom-Json } catch { }
        return [pscustomobject]@{ status = [int]$resp.StatusCode; body = $parsed }
    }
}

Write-Host "== TrainLog 冒烟测试 $BaseUrl ==" -ForegroundColor Cyan

$captcha = New-Captcha
Check '获取图片验证码' ($captcha.image -like 'data:image/png;base64,*' -and $captcha.code -ne 'AAAA') '验证码图片或日志中的答案缺失'

$login = Invoke-Json "$BaseUrl/api/auth/login" 'Post' @{
    username = 'admin'; password = '123456'; captchaId = $captcha.id; captchaCode = $captcha.code
} $null
Check '管理员登录成功' ($login.status -eq 200 -and $login.body.data.token) "返回 $($login.status)"

if ($login.status -ne 200) {
    Write-Host "== 通过 $script:passed 项，失败 $script:failed 项 ==" -ForegroundColor Cyan
    exit 1
}

$headers = @{ Authorization = "Bearer $($login.body.data.token)" }

$me = Invoke-Json "$BaseUrl/api/auth/me" 'Get' $null $headers
Check '读取当前用户' ($me.status -eq 200 -and $me.body.data.username -eq 'admin') "返回 $($me.status)"

$types = Invoke-Json "$BaseUrl/api/training/types" 'Get' $null $headers
Check '训练类型列表' ($types.status -eq 200 -and $types.body.data.Count -ge 8) "返回 $($types.status)"

$list = Invoke-Json "$BaseUrl/api/training/records?page=1&size=5" 'Get' $null $headers
Check '分页读取训练记录' ($list.status -eq 200 -and $null -ne $list.body.data.total) "返回 $($list.status)"

$today = Get-Date -Format 'yyyy-MM-dd'
$created = Invoke-Json "$BaseUrl/api/training/records" 'Post' @{
    title = '冒烟测试训练'; type = 'STRENGTH'; trainingDate = $today; durationMinutes = 30;
    distanceKm = $null; calories = 200; intensity = 7; notes = '自动化测试'
} $headers
Check '新增训练记录' ($created.status -eq 200 -and $created.body.data.id) "返回 $($created.status) $($created.body.message)"

$recordId = $created.body.data.id
if ($recordId) {
    $updated = Invoke-Json "$BaseUrl/api/training/records/$recordId" 'Put' @{
        title = '冒烟测试训练（已更新）'; type = 'STRENGTH'; trainingDate = $today; durationMinutes = 45;
        distanceKm = $null; calories = 260; intensity = 8; notes = '自动化测试'
    } $headers
    Check '更新训练记录' ($updated.status -eq 200 -and $updated.body.data.durationMinutes -eq 45) "返回 $($updated.status)"

    $stats = Invoke-Json "$BaseUrl/api/training/stats?days=30" 'Get' $null $headers
    Check '读取统计汇总' ($stats.status -eq 200 -and $stats.body.data.sessions -ge 1) "返回 $($stats.status)"

    $deleted = Invoke-Json "$BaseUrl/api/training/records/$recordId" 'Delete' $null $headers
    Check '删除训练记录' ($deleted.status -eq 200) "返回 $($deleted.status)"
}

$unauth = Invoke-Json "$BaseUrl/api/training/records" 'Get' $null $null
Check '未登录访问被拒绝' ($unauth.status -eq 401) "返回 $($unauth.status)"

$newUser = 'smoke' + (Get-Random -Minimum 1000 -Maximum 99999)
$regCaptcha = New-Captcha
$reg = Invoke-Json "$BaseUrl/api/auth/register" 'Post' @{
    username = $newUser; nickname = '冒烟用户'; password = 'Passw0rd123'; inviteCode = '';
    captchaId = $regCaptcha.id; captchaCode = $regCaptcha.code
} $null
Check '注册新账号并自动登录' ($reg.status -eq 200 -and $reg.body.data.token) "返回 $($reg.status) $($reg.body.message)"

if ($reg.status -eq 200) {
    $regHeaders = @{ Authorization = "Bearer $($reg.body.data.token)" }
    $own = Invoke-Json "$BaseUrl/api/training/records" 'Get' $null $regHeaders
    Check '新账号数据隔离' ($own.status -eq 200 -and $own.body.data.total -eq 0) "返回 $($own.status)"
}

Write-Host "== 通过 $script:passed 项，失败 $script:failed 项 ==" -ForegroundColor Cyan
if ($script:failed -gt 0) { exit 1 }
