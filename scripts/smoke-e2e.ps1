param([string]$ApiBaseUrl = 'http://localhost:8080/api')

$ErrorActionPreference = 'Stop'
$username = "e2e_$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$password = 'TravelMind-E2E-2026!'
$newPassword = 'TravelMind-E2E-Changed-2026!'
$headers = @{ 'Content-Type' = 'application/json' }

$session = Invoke-RestMethod -Method Post -Uri "$ApiBaseUrl/user/auth/register" -Headers $headers -Body (@{
    username = $username; nickname = '端到端测试'; password = $password
} | ConvertTo-Json)
$token = $session.data.tokenValue
if (!$token) { throw '注册未返回登录令牌。' }
$auth = @{ Authorization = $token }
$me = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/user/auth/me" -Headers $auth
if ($me.data.roles -notcontains 'user') { throw '注册账号角色异常。' }
$export = Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/user/account/export" -Headers $auth
if ($export.data.profile.user.username -ne $username) { throw '账号数据导出异常。' }

Invoke-RestMethod -Method Put -Uri "$ApiBaseUrl/user/account/password" -Headers ($auth + $headers) -Body (@{
    currentPassword = $password; newPassword = $newPassword
} | ConvertTo-Json) | Out-Null
try {
    Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/user/auth/me" -Headers $auth | Out-Null
    throw '修改密码后旧令牌仍然有效。'
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) { throw }
}

$session = Invoke-RestMethod -Method Post -Uri "$ApiBaseUrl/user/auth/login" -Headers $headers -Body (@{
    username = $username; password = $newPassword
} | ConvertTo-Json)
$auth = @{ Authorization = $session.data.tokenValue }
Invoke-RestMethod -Method Delete -Uri "$ApiBaseUrl/user/account" -Headers $auth | Out-Null
try {
    Invoke-RestMethod -Method Get -Uri "$ApiBaseUrl/user/auth/me" -Headers $auth | Out-Null
    throw '停用账号后旧令牌仍然有效。'
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 401) { throw }
}

Write-Host "端到端冒烟测试通过：$username"
