#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP Platformu - Local Run Script (Bash)
# Starts PostgreSQL via Docker Compose, then launches Spring Boot on port 8080
# ==============================================================================

set -euo pipefail

SKIP_POSTGRES=0
DOCKER_ALL=0

for arg in "$@"; do
    case "$arg" in
        --skip-postgres|--skip-mysql) SKIP_POSTGRES=1 ;;
        --docker-all) DOCKER_ALL=1 ;;
        -h|--help)
            echo "Usage: $0 [--skip-postgres] [--docker-all]"
            echo "  --skip-postgres   Do not start the PostgreSQL container"
            echo "  --docker-all      Run PostgreSQL + app via Docker Compose on port 81"
            exit 0
            ;;
        *)
            echo "Unknown option: $arg" >&2
            exit 1
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker/docker-compose.yml"
POSTGRES_CONTAINER="marble-erp-postgres"
APP_PORT=8080
POSTGRES_WAIT_SECONDS=90

cd "$REPO_ROOT"

echo "======================================================================"
echo "   Özerler Mermer ERP - Local Development Runner (PostgreSQL)         "
echo "======================================================================"
echo "Repo: $REPO_ROOT"

wait_postgres_healthy() {
    local elapsed=0
    echo "[WAIT] Waiting for PostgreSQL health ($POSTGRES_CONTAINER)..."
    while [ "$elapsed" -lt "$POSTGRES_WAIT_SECONDS" ]; do
        local health
        health="$(docker inspect -f '{{.State.Health.Status}}' "$POSTGRES_CONTAINER" 2>/dev/null || true)"
        if [ "$health" = "healthy" ]; then
            echo "[OK] PostgreSQL is ready."
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        echo "  ... ${elapsed}s (status: ${health:-starting})"
    done
    echo "PostgreSQL did not become healthy within ${POSTGRES_WAIT_SECONDS}s. Check: docker logs $POSTGRES_CONTAINER" >&2
    exit 1
}

java_major_version() {
    java -version 2>&1 | awk -F '"' '/version/ { split($2, a, "."); print a[1]; exit }'
}

if [ "$DOCKER_ALL" -eq 1 ]; then
    if ! command -v docker >/dev/null 2>&1; then
        echo "Docker was not found. Install and start Docker Desktop." >&2
        exit 1
    fi
    echo "[DOCKER] Starting PostgreSQL + application (port 81)..."
    docker compose -f "$COMPOSE_FILE" up -d --build
    echo ""
    echo "App:    http://localhost:81"
    echo "Admin:  http://localhost:81/account/adminlogin/"
    echo "Email:  admin@eimece.test"
    echo "Pass:   B2u5c8JB"
    exit 0
fi

if ! command -v java >/dev/null 2>&1; then
    echo "Java was not found. Install JDK 24+ and add it to PATH." >&2
    exit 1
fi

JAVA_MAJOR="$(java_major_version)"
if [ "${JAVA_MAJOR:-0}" -lt 24 ]; then
    echo "JDK 24+ is required. Current major version: ${JAVA_MAJOR:-unknown}" >&2
    exit 1
fi
echo "[OK] Java $JAVA_MAJOR"

if [ ! -f "$REPO_ROOT/mvnw" ]; then
    echo "Maven wrapper not found: $REPO_ROOT/mvnw" >&2
    exit 1
fi
chmod +x "$REPO_ROOT/mvnw"

if [ "$SKIP_POSTGRES" -eq 0 ]; then
    if ! command -v docker >/dev/null 2>&1; then
        echo "Docker was not found. Install Docker, or re-run with --skip-postgres." >&2
        exit 1
    fi
    echo "[DOCKER] Starting PostgreSQL..."
    docker compose -f "$COMPOSE_FILE" up -d postgres
    wait_postgres_healthy
    echo "[DOCKER] Starting MinIO..."
    docker compose -f "$COMPOSE_FILE" up -d minio
else
    echo "[SKIP] PostgreSQL Docker step skipped."
fi

echo ""
echo "App:         http://localhost:$APP_PORT"
echo "Admin:       http://localhost:$APP_PORT/account/adminlogin/"
echo "MinIO API:   http://localhost:9000"
echo "MinIO UI:    http://localhost:9001"
echo "Email:       admin@eimece.test"
echo "Pass:        B2u5c8JB"
echo ""
echo "[APP] Starting Spring Boot (dev profile)..."

exec ./mvnw spring-boot:run
