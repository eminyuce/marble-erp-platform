# ==============================================================================
# Özerler Mermer ERP Platformu - Dummy Data Loader Script (PowerShell)
# Loads seed_dummy_data.sql into local PostgreSQL or running Docker container
# ==============================================================================

param (
    [string]$DbUser = "marbleuser",
    [string]$DbPass = "marblepass",
    [string]$DbName = "marble_erp",
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 5432,
    [string]$SqlFile = "scripts/seed_dummy_data.sql"
)

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   Özerler Mermer ERP - Test & Demo Veri Yükleme Aracı (PostgreSQL)   " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

if (-not (Test-Path $SqlFile)) {
    Write-Error "SQL dosyası bulunamadı: $SqlFile"
    exit 1
}

# 1. Check if Docker container is running
$dockerContainer = "marble-erp-postgres"
$isDockerRunning = $false
try {
    $containerStatus = docker inspect -f '{{.State.Running}}' $dockerContainer 2>$null
    if ($containerStatus -ne "true") {
        $dockerContainer = "marble-postgres"
        $containerStatus = docker inspect -f '{{.State.Running}}' $dockerContainer 2>$null
    }
    if ($containerStatus -eq "true") {
        $isDockerRunning = $true
    }
} catch {
    # Docker not available or container not created
}

if ($isDockerRunning) {
    Write-Host "[DOCKER DETECTED] Aktif PostgreSQL konteyneri bulundu ($dockerContainer)." -ForegroundColor Green
    Write-Host "[IMPORTING] Veriler Docker konteynerine aktarılıyor..." -ForegroundColor Yellow
    
    Get-Content $SqlFile -Raw -Encoding UTF8 | docker exec -i -e PGPASSWORD=$DbPass $dockerContainer psql -U $DbUser -d $DbName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✔ Başarılı! Veriler $dockerContainer içerisindeki '$DbName' veritabanına aktarıldı." -ForegroundColor Green
        exit 0
    } else {
        Write-Warning "Docker üzerinden aktarım başarısız oldu. Yerel PostgreSQL denenecek..."
    }
}

# 2. Try Local PostgreSQL CLI (psql)
$psqlCmd = Get-Command psql -ErrorAction SilentlyContinue
if ($psqlCmd) {
    Write-Host "[LOCAL POSTGRESQL] Yerel psql CLI tespit edildi: $DbHost Port: $DbPort" -ForegroundColor Cyan
    Write-Host "[IMPORTING] Veriler aktarılıyor..." -ForegroundColor Yellow
    
    $env:PGPASSWORD = $DbPass
    Get-Content $SqlFile -Raw -Encoding UTF8 | psql --host=$DbHost --port=$DbPort -U $DbUser -d $DbName
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✔ Başarılı! Veriler $DbHost / $DbPort üzerindeki '$DbName' veritabanına aktarıldı." -ForegroundColor Green
        exit 0
    } else {
        Write-Error "Yerel PostgreSQL aktarımı başarısız oldu."
        exit 1
    }
}

Write-Warning "Ne aktif Docker PostgreSQL konteyneri ne de 'psql' komut satırı aracı bulunamadı."
Write-Host "SQL dosyasını DBeaver, pgAdmin veya IntelliJ Database panelinden çalıştırabilirsiniz:" -ForegroundColor Yellow
Write-Host "  -> $SqlFile" -ForegroundColor White
