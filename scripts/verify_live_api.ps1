# Verify live server on port 8080
$loginPage = Invoke-WebRequest -Uri "http://localhost:8080/account/adminlogin/" -SessionVariable session
$csrf = ""
if ($loginPage.Content -match 'name="_csrf" value="([^"]+)"') {
    $csrf = $matches[1]
}

Write-Host "CSRF Token resolved: $csrf" -ForegroundColor Cyan

$loginBody = @{
    username = "admin@eimece.test"
    password = "B2u5c8JB"
    _csrf = $csrf
}

$loginResponse = Invoke-WebRequest -Uri "http://localhost:8080/login" -Method Post -Body $loginBody -WebSession $session -MaximumRedirection 0 -ErrorAction SilentlyContinue

Write-Host "Login response status: $($loginResponse.StatusCode)" -ForegroundColor Green

# Fetch blocks data
$blocksResponse = Invoke-RestMethod -Uri "http://localhost:8080/blocks/api/data?page=1`&size=10" -WebSession $session
Write-Host "Blocks found: $($blocksResponse.total)" -ForegroundColor Green
Write-Host "Sample Block: $($blocksResponse.data[0].block_code) - $($blocksResponse.data[0].stone_type) - $($blocksResponse.data[0].quality_grade)" -ForegroundColor Yellow

# Fetch dashboard
$dashboard = Invoke-WebRequest -Uri "http://localhost:8080/admin/dashboard" -WebSession $session
Write-Host "Dashboard HTTP Status: $($dashboard.StatusCode)" -ForegroundColor Green
