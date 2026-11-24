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

# Production: make sure the TOTP key file exists before the JVM starts.
# The key is not a compose secret (a missing compose secret file stops "docker compose up"
# before the container exists, and the in-app deploy has no other way to reach the host).
# A one-shot helper container creates it on the host when missing (atomic, never
# overwrites, aborts on empty files) and its content is copied into a private file.
# The value never reaches stdout, stderr or the logs.
ensure_totp_key() {
    host_dir="${MARBLE_HOST_SECRETS_DIR:-}"
    target="${TOTP_ENCRYPTION_KEY_FILE:-}"
    if [ -z "$host_dir" ] || [ -z "$target" ]; then
        return 0
    fi
    if [ -s "$target" ]; then
        return 0
    fi
    helper="${MARBLE_SECRETS_HELPER_IMAGE:-alpine:3.24.1}"
    if ! command -v docker >/dev/null 2>&1; then
        echo "[HATA] TOTP anahtarı hazırlanamadı: imajda docker istemcisi yok." >&2
        exit 1
    fi
    echo "[INFO] TOTP anahtar dosyası kontrol ediliyor (totp_encryption_key.txt)."
    if ! docker run --rm -i --network none --user 0:0 \
            -e APP_UID="$(id -u)" -e APP_GID="$(id -g)" \
            -v "${host_dir}:/s" "$helper" \
            sh -s -- --dir /s --host-dir "$host_dir" --name totp_encryption_key.txt \
            < /stack/scripts/ensure_secrets.sh; then
        echo "[HATA] TOTP anahtarı hazırlanamadı. Ayrıntı yukarıda; uygulama başlatılmadı." >&2
        exit 1
    fi
    old_umask=$(umask)
    umask 077
    mkdir -p "$(dirname "$target")"
    if ! docker run --rm --network none --user 0:0 -v "${host_dir}:/s:ro" "$helper" \
            cat /s/totp_encryption_key.txt > "$target.tmp" 2>/dev/null || [ ! -s "$target.tmp" ]; then
        rm -f "$target.tmp"
        umask "$old_umask"
        echo "[HATA] TOTP anahtar dosyası okunamadı (totp_encryption_key.txt)." >&2
        exit 1
    fi
    mv -f "$target.tmp" "$target"
    chmod 400 "$target"
    umask "$old_umask"
    echo "[OK] TOTP anahtar dosyası hazır (totp_encryption_key.txt)."
}

if [ "${MARBLE_IN_CONTAINER:-}" = "true" ]; then
    ensure_totp_key
fi

exec java ${JAVA_OPTS:-} -Dserver.shutdown=graceful -jar /app/app.jar "$@"
