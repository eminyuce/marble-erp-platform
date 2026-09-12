#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP Platformu - Local Run Script (Bash)
# Starts MySQL via Docker Compose, then launches Spring Boot on port 8080
# ==============================================================================

set -euo pipefail

SKIP_MYSQL=0
DOCKER_ALL=0

for arg in "$@"; do
    case "$arg" in
        --skip-mysql) SKIP_MYSQL=1 ;;
        --docker-all) DOCKER_ALL=1 ;;
        -h|--help)
            echo "Usage: $0 [--skip-mysql] [--docker-all]"
            echo "  --skip-mysql   Do not start the MySQL container"
            echo "  --docker-all   Run MySQL + app via Docker Compose on port 81"
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
MYSQL_CONTAINER="marble-erp-mysql"
APP_PORT=8080
MYSQL_WAIT_SECONDS=90

cd "$REPO_ROOT"

echo "======================================================================"
echo "   Özerler Mermer ERP - Local Development Runner                     "
echo "======================================================================"
echo "Repo: $REPO_ROOT"

wait_mysql_healthy() {
    local elapsed=0
    echo "[WAIT] Waiting for MySQL health ($MYSQL_CONTAINER)..."
    while [ "$elapsed" -lt "$MYSQL_WAIT_SECONDS" ]; do
        local health
        health="$(docker inspect -f '{{.State.Health.Status}}' "$MYSQL_CONTAINER" 2>/dev/null || true)"
        if [ "$health" = "healthy" ]; then
            echo "[OK] MySQL is ready."
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        echo "  ... ${elapsed}s (status: ${health:-starting})"
    done
    echo "MySQL did not become healthy within ${MYSQL_WAIT_SECONDS}s. Check: docker logs $MYSQL_CONTAINER" >&2
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
    echo "[DOCKER] Starting MySQL + application (port 81)..."
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

if [ "$SKIP_MYSQL" -eq 0 ]; then
    if ! command -v docker >/dev/null 2>&1; then
        echo "Docker was not found. Install Docker, or re-run with --skip-mysql." >&2
        exit 1
    fi
    echo "[DOCKER] Starting MySQL..."
    docker compose -f "$COMPOSE_FILE" up -d mysql
    wait_mysql_healthy
else
    echo "[SKIP] MySQL Docker step skipped."
fi

echo ""
echo "App:    http://localhost:$APP_PORT"
echo "Admin:  http://localhost:$APP_PORT/account/adminlogin/"
echo "Email:  admin@eimece.test"
echo "Pass:   B2u5c8JB"
echo ""
echo "[APP] Starting Spring Boot (dev profile)..."

exec ./mvnw spring-boot:run
