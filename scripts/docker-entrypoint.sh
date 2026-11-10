#!/usr/bin/env sh
# ==============================================================================
# Özerler Mermer ERP — Container Entrypoint
#
# Supports:
#   --print-compose       Print standalone docker-compose.yml (Postgres + RustFS + App)
#   --print-prod-compose  Print production docker-compose.prod.yml
#   --version             Print image version and Git commit SHA
# ==============================================================================

set -eu

if [ "${1:-}" = "--print-compose" ]; then
    cat /stack/docker-compose.yml
    exit 0
fi

if [ "${1:-}" = "--print-prod-compose" ]; then
    cat /stack/docker-compose.prod.yml
    exit 0
fi

if [ "${1:-}" = "--version" ]; then
    echo "version=${APP_VERSION:-1.0.0} commit=${APP_GIT_SHA:-unknown}"
    exit 0
fi

# When someone runs "docker run guvenulu/marble-erp-platform:latest" outside
# Docker Compose (so service DNS names "postgres" and "rustfs" do not resolve),
# automatically fall back to host.docker.internal if available on Docker Desktop.
if [ "${MARBLE_IN_CONTAINER:-}" != "true" ] && [ -z "${DB_URL:-}" ]; then
    target_db_host="${DB_HOST:-postgres}"
    if ! getent hosts "$target_db_host" >/dev/null 2>&1; then
        if getent hosts host.docker.internal >/dev/null 2>&1; then
            export DB_HOST="host.docker.internal"
            export DB_URL="jdbc:postgresql://host.docker.internal:${DB_PORT:-5432}/${DB_NAME:-marble_erp}"
            if [ "${RUSTFS_ENDPOINT:-http://rustfs:9000}" = "http://rustfs:9000" ]; then
                export RUSTFS_ENDPOINT="http://host.docker.internal:${RUSTFS_API_PORT:-9000}"
            fi
            echo "[INFO] '${target_db_host}' DNS çözümlenemedi; Docker Desktop host.docker.internal kullanılıyor."
        else
            echo "[WARN] '${target_db_host}' servisine ulaşılamıyor. Tüm bağımlılıklarla (PostgreSQL + RustFS + ERP) çalıştırmak için:" >&2
            echo "       docker run --rm guvenulu/marble-erp-platform:latest --print-compose > docker-compose.yml && docker compose up -d" >&2
        fi
    fi
fi

exec java ${JAVA_OPTS:-} -Dserver.shutdown=graceful -jar /app/app.jar "$@"
