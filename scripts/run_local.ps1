# ==============================================================================
# Ozerler Mermer ERP Platform - Local Run Script (PowerShell)
# Starts PostgreSQL via Docker Compose, then launches Spring Boot on port 8080
# ==============================================================================

param (
    [switch]$SkipPostgres,
    [switch]$SkipMysql,
    [switch]$DockerAll
)

$ErrorActionPreference = "Stop"

if ($SkipMysql) {
    $SkipPostgres = $true
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
$ComposeFile = Join-Path $RepoRoot "docker\docker-compose.yml"
$PostgresContainer = "marble-erp-postgres"
$AppPort = 8080
$PostgresWaitSeconds = 90

Set-Location $RepoRoot

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   Ozerler Mermer ERP - Local Development Runner (PostgreSQL)         " -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "Repo: $RepoRoot" -ForegroundColor DarkGray

function Test-CommandExists {
    param ([string]$Name)
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-JavaMajorVersion {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        return 0
    }

    $previousEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        # java --version writes to stdout; java -version writes to stderr.
        # Windows PowerShell treats stderr as errors when ErrorActionPreference is Stop.
        $output = & java --version 2>&1 | ForEach-Object { $_.ToString() }
        $text = $output -join "`n"
        if ($text -match '(?i)(?:openjdk|java)(?:\s+version)?[^\d]*(\d+)') {
            return [int]$Matches[1]
        }
        if ($text -match 'version "?(\d+)') {
            return [int]$Matches[1]
        }
    } catch {
        return 0
    } finally {
        $ErrorActionPreference = $previousEap
    }
    return 0
}

function Wait-PostgresHealthy {
    param ([int]$TimeoutSeconds)

    $elapsed = 0
    Write-Host "[WAIT] Waiting for PostgreSQL health ($PostgresContainer)..." -ForegroundColor Yellow
    while ($elapsed -lt $TimeoutSeconds) {
        $previousEap = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $health = docker inspect -f "{{.State.Health.Status}}" $PostgresContainer 2>$null
        $ErrorActionPreference = $previousEap
        if ($health -eq "healthy") {
            Write-Host "[OK] PostgreSQL is ready." -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
        $status = if ($health) { $health } else { "starting" }
        Write-Host "  ... ${elapsed}s (status: $status)" -ForegroundColor DarkGray
    }

    throw "PostgreSQL did not become healthy within $TimeoutSeconds seconds. Check: docker logs $PostgresContainer"
}

if ($DockerAll) {
    if (-not (Test-CommandExists "docker")) {
        throw "Docker was not found. Install and start Docker Desktop."
    }
    Write-Host "[DOCKER] Starting PostgreSQL + application (port 81)..." -ForegroundColor Yellow
    docker compose -f $ComposeFile up -d --build
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose up failed."
    }
    Write-Host ""
    Write-Host "App:    http://localhost:81" -ForegroundColor Green
    Write-Host "Admin:  http://localhost:81/account/adminlogin/" -ForegroundColor Green
    Write-Host "Email:  admin@eimece.test" -ForegroundColor White
    Write-Host "Pass:   B2u5c8JB" -ForegroundColor White
    exit 0
}

$javaMajor = Get-JavaMajorVersion
if ($javaMajor -eq 0) {
    throw "Java was not found. Install JDK 24+ and add it to PATH."
}
if ($javaMajor -lt 24) {
    throw "JDK 24+ is required. Current major version: $javaMajor"
}
Write-Host "[OK] Java $javaMajor" -ForegroundColor Green

$mvnw = Join-Path $RepoRoot "mvnw.cmd"
if (-not (Test-Path $mvnw)) {
    throw "Maven wrapper not found: $mvnw"
}

if (-not $SkipPostgres) {
    if (-not (Test-CommandExists "docker")) {
        throw "Docker was not found. Install Docker, or re-run with -SkipPostgres."
    }

    Write-Host "[DOCKER] Starting PostgreSQL..." -ForegroundColor Yellow
    docker compose -f $ComposeFile up -d postgres
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start the PostgreSQL container."
    }
    Wait-PostgresHealthy -TimeoutSeconds $PostgresWaitSeconds
    Write-Host "[DOCKER] Starting MinIO..." -ForegroundColor Yellow
    docker compose -f $ComposeFile up -d minio
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start the MinIO container."
    }
} else {
    Write-Host "[SKIP] PostgreSQL Docker step skipped." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "App:         http://localhost:$AppPort" -ForegroundColor Green
Write-Host "Admin:       http://localhost:$AppPort/account/adminlogin/" -ForegroundColor Green
Write-Host "MinIO API:   http://localhost:9000" -ForegroundColor Green
Write-Host "MinIO UI:    http://localhost:9001" -ForegroundColor Green
Write-Host "Email:       admin@eimece.test" -ForegroundColor White
Write-Host "Pass:        B2u5c8JB" -ForegroundColor White
Write-Host ""
Write-Host "[APP] Starting Spring Boot (dev profile)..." -ForegroundColor Yellow

& $mvnw spring-boot:run
exit $LASTEXITCODE
