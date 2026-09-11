# ==============================================================================
# Özerler Mermer ERP Platformu - Dummy Data Loader Script (PowerShell)
# Loads seed_dummy_data.sql into local MySQL or running Docker container
# ==============================================================================

param (
    [string]$DbUser = "root",
    [string]$DbPass = "rootpassword",
    [string]$DbName = "marble_erp",
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$SqlFile = "scripts/seed_dummy_data.sql"
)

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   Özerler Mermer ERP - Test & Demo Veri Yükleme Aracı (Seed Script)  " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

if (-not (Test-Path $SqlFile)) {
    Write-Error "SQL dosyası bulunamadı: $SqlFile"
    exit 1
}

# 1. Check if Docker container is running
$dockerContainer = "marble-erp-mysql"
$isDockerRunning = $false
try {
    $containerStatus = docker inspect -f '{{.State.Running}}' $dockerContainer 2>$null
    if ($containerStatus -ne "true") {
        $dockerContainer = "marble-mysql"
        $containerStatus = docker inspect -f '{{.State.Running}}' $dockerContainer 2>$null
    }
    if ($containerStatus -eq "true") {
        $isDockerRunning = $true
    }
} catch {
    # Docker not available or container not created
}

if ($isDockerRunning) {
    Write-Host "[DOCKER DETECTED] Aktif MySQL konteyneri bulundu ($dockerContainer)." -ForegroundColor Green
    Write-Host "[IMPORTING] Veriler Docker konteynerine aktarılıyor..." -ForegroundColor Yellow
    
    Get-Content $SqlFile -Raw -Encoding UTF8 | docker exec -i $dockerContainer mysql -u $DbUser "-p$DbPass" $DbName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✔ Başarılı! Veriler $dockerContainer içerisindeki '$DbName' veritabanına aktarıldı." -ForegroundColor Green
        exit 0
    } else {
        Write-Warning "Docker üzerinden aktarım başarısız oldu. Yerel MySQL denenecek..."
    }
}

# 2. Try Local MySQL CLI
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysqlCmd) {
    Write-Host "[LOCAL MYSQL] Yerel MySQL CLI tespit edildi: $DbHost Port: $DbPort" -ForegroundColor Cyan
    Write-Host "[IMPORTING] Veriler aktarılıyor..." -ForegroundColor Yellow
    
    Get-Content $SqlFile -Raw -Encoding UTF8 | mysql --host=$DbHost --port=$DbPort -u$DbUser "-p$DbPass" $DbName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✔ Başarılı! Veriler $DbHost / $DbPort üzerindeki '$DbName' veritabanına aktarıldı." -ForegroundColor Green
        exit 0
    } else {
        Write-Error "Yerel MySQL aktarımı başarısız oldu."
        exit 1
    }
}

Write-Warning "Ne aktif Docker MySQL konteyneri ne de 'mysql' komut satırı aracı bulunamadı."
Write-Host "SQL dosyasını DBeaver, MySQL Workbench veya IntelliJ Database panelinden çalıştırabilirsiniz:" -ForegroundColor Yellow
Write-Host "  -> $SqlFile" -ForegroundColor White
