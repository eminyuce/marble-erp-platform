#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — Production deploy (Linux host)
#
# Usage:
#   ./scripts/deploy_production.sh        # prompts [Y/n] in terminal
#   ./scripts/deploy_production.sh -y     # deploys without prompting
#   ./scripts/deploy_production.sh --yes  # deploys without prompting
#
# See docs/production-deploy.md and DEPLOYMENT.md
#
# What this does:
#   1. Pre-flight checks: Linux host, JDK 25+, Docker DB container running
#   2. Prepares /opt/marble-erp and verifies /opt/marble-erp/marble-erp.env
#   3. Build frontend (npm install && npm run build) unless --skip-frontend
#   4. ./mvnw clean package -DskipTests (unless --skip-build)
#   5. Stop marble-erp via systemctl (or live JVM)
#   6. Backup current JAR -> /opt/marble-erp/app.jar.bak.<timestamp>
#   7. Copy built JAR -> /opt/marble-erp/app.jar
#   8. Sync /etc/systemd/system/marble-erp.service and start service
#   9. Verify local health (port 8080) and public domain (https://ozerler.naklink.com)
# ==============================================================================

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: ./scripts/deploy_production.sh [options]

  -y, --yes          Apply deployment without interactive prompt.
  --dry-run          Preview deployment steps and exit.
  --skip-frontend    Skip npm install && npm run build
  --skip-build       Skip frontend and Maven; restart/replace with existing JAR
  --print-unit       Print the default systemd unit and exit
  -h, --help         Show this help

Environment (optional):
  DEPLOY_DIR         Default: /opt/marble-erp
  SERVICE_NAME       Default: marble-erp
  SERVICE_USER       Default: current user (eyuce on this host)
  SERVER_PORT        Default: 8080 (must match marble-erp.env)
  HEALTH_PATH        Default: /health/
EOF
}

YES=0
DRY_RUN=0
SKIP_FRONTEND=0
SKIP_BUILD=0
PRINT_UNIT=0

for arg in "$@"; do
    case "$arg" in
        -y|--yes) YES=1 ;;
        --dry-run) DRY_RUN=1 ;;
        --skip-frontend) SKIP_FRONTEND=1 ;;
        --skip-build) SKIP_BUILD=1; SKIP_FRONTEND=1 ;;
        --print-unit) PRINT_UNIT=1 ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $arg" >&2
            usage >&2
            exit 1
            ;;
    esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_DIR="${DEPLOY_DIR:-/opt/marble-erp}"
SERVICE_NAME="${SERVICE_NAME:-marble-erp}"
SERVICE_USER="${SERVICE_USER:-${SUDO_USER:-$(id -un)}}"
if [ "$SERVICE_USER" = "root" ] && id -u eyuce >/dev/null 2>&1; then
    SERVICE_USER="eyuce"
fi
SERVICE_GROUP="${SERVICE_GROUP:-$(id -gn "$SERVICE_USER" 2>/dev/null || echo "$SERVICE_USER")}"
SERVER_PORT="${SERVER_PORT:-8080}"
HEALTH_PATH="${HEALTH_PATH:-/health/}"
ENV_FILE="${DEPLOY_DIR}/marble-erp.env"
UNIT_PATH="/etc/systemd/system/${SERVICE_NAME}.service"
JAR_NAME="marble-erp-platform-1.0.0.jar"
BUILT_JAR="${REPO_ROOT}/target/${JAR_NAME}"
LIVE_JAR="${DEPLOY_DIR}/app.jar"

die() {
    echo "ERROR: $*" >&2
    exit 1
}

need_cmd() {
    command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

java_major_version() {
    java -version 2>&1 | awk -F '"' '/version/ { split($2, a, "."); print a[1]; exit }'
}

sudo_cmd() {
    if [ "$(id -u)" -eq 0 ]; then
        "$@"
    else
        sudo "$@"
    fi
}

install_inapp_deploy_support() {
    local src="$REPO_ROOT/scripts/deploy_from_app.sh"
    local dest="/usr/local/sbin/marble-erp-inapp-deploy"
    local sudoers_file="/etc/sudoers.d/marble-erp-inapp-deploy"

    if [ ! -f "$src" ]; then
        echo "[INAPP] $src not found; skipping in-app deploy setup"
        return 0
    fi

    echo "[INAPP] Installing root-owned $dest"
    sudo_cmd install -m 755 -o root -g root "$src" "$dest"

    local tmp
    tmp="$(mktemp)"
    cat > "$tmp" <<EOF
# Allow the ERP service user to start a production deploy from the web UI.
# The command target is root-owned so the git working copy cannot replace it.
${SERVICE_USER} ALL=(root) NOPASSWD: ${dest}
EOF
    local visudo_ok=0
    if visudo -cf "$tmp" >/dev/null 2>&1; then
        visudo_ok=1
    elif sudo_cmd visudo -cf "$tmp" >/dev/null 2>&1; then
        visudo_ok=1
    fi
    if [ "$visudo_ok" -eq 1 ]; then
        sudo_cmd install -m 440 -o root -g root "$tmp" "$sudoers_file"
        echo "[INAPP] Passwordless sudo: ${SERVICE_USER} -> ${dest}"
    else
        echo "[INAPP] visudo rejected ${sudoers_file}; in-app deploy will stay disabled."
        cat "$tmp" >&2
    fi
    rm -f "$tmp"
}

default_unit() {
    cat <<EOF
[Unit]
Description=Ozerler Mermer ERP Platform
After=network.target docker.service postgresql.service
Wants=docker.service

[Service]
Type=simple
User=${SERVICE_USER}
Group=${SERVICE_GROUP}
WorkingDirectory=${DEPLOY_DIR}
EnvironmentFile=-${ENV_FILE}
Environment=SPRING_PROFILES_ACTIVE=prod
ExecStart=/usr/bin/java -server -XX:+UseZGC -XX:MaxRAMPercentage=75.0 -jar ${LIVE_JAR}
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF
}

write_env_template() {
    sudo_cmd tee "$ENV_FILE" >/dev/null <<'EOF'
# Production environment for Özerler Mermer ERP
# Loaded by systemd (EnvironmentFile). Do not commit this file.
# Matches src/main/resources/application-prod.yml

SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
PORT=8080

# Database Configuration (PostgreSQL running in Docker container: marble-erp-postgres)
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=marble_erp
DB_USERNAME=marbleuser
DB_PASSWORD=marblepass
DB_URL=jdbc:postgresql://127.0.0.1:5432/marble_erp

# Database Connection Pool Settings
DB_POOL_MAX=10
DB_POOL_MIN_IDLE=2

# Legacy filesystem paths — kept only so existing files can be migrated to MinIO.
# New uploads go to MinIO, not these directories.
APP_UPLOAD_DIR=/opt/marble-erp/uploads
APP_MEDIA_DIR=/opt/marble-erp/media

# MinIO object storage (S3-compatible). Required. Do not leave CHANGE_ME in production.
STORAGE_TYPE=minio
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_PUBLIC_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=CHANGE_ME
MINIO_SECRET_KEY=CHANGE_ME
MINIO_BUCKET=erp-files
MINIO_REGION=us-east-1
MINIO_ROOT_USER=CHANGE_ME
MINIO_ROOT_PASSWORD=CHANGE_ME
MINIO_DATA_DIR=/opt/minio/data

# Public Domain and CORS Configuration
CORS_ALLOWED_ORIGIN=https://ozerler.naklink.com
CORS_ALLOWED_ORIGIN_PATTERN=https://*.naklink.com

# Rate Limiting Configuration
RATE_LIMIT_LOGIN_MAX=10
RATE_LIMIT_LOGIN_WINDOW=60
RATE_LIMIT_FORM_MAX=30
RATE_LIMIT_FORM_WINDOW=60
RATE_LIMIT_GENERAL_MAX=120
RATE_LIMIT_GENERAL_WINDOW=60
EOF
    sudo_cmd chmod 640 "$ENV_FILE"
    sudo_cmd chown "${SERVICE_USER}:${SERVICE_GROUP}" "$ENV_FILE" || true
}

if [ "$PRINT_UNIT" -eq 1 ]; then
    default_unit
    exit 0
fi

echo "======================================================================"
echo "   Özerler Mermer ERP — Production deploy"
echo "======================================================================"
echo "Repo:       $REPO_ROOT"
echo "Deploy dir: $DEPLOY_DIR"
echo "Live JAR:   $LIVE_JAR"
echo "Service:    $SERVICE_NAME"
echo "User:       $SERVICE_USER"
echo "Port:       $SERVER_PORT"
echo

# --- Safety checks -----------------------------------------------------------
if [ "$(uname -s)" != "Linux" ]; then
    die "This script is for the Linux production host only. Use scripts/run_local.sh for local/dev."
fi

if [ ! -f "$REPO_ROOT/pom.xml" ] || ! grep -q '<artifactId>marble-erp-platform</artifactId>' "$REPO_ROOT/pom.xml"; then
    die "Repo root does not look like marble-erp-platform (pom.xml artifactId). Refusing."
fi

if [ "$DEPLOY_DIR" != "/opt/marble-erp" ] && [ "${ALLOW_CUSTOM_DEPLOY_DIR:-}" != "1" ]; then
    die "DEPLOY_DIR is '$DEPLOY_DIR' (expected /opt/marble-erp). Set ALLOW_CUSTOM_DEPLOY_DIR=1 if intentional."
fi

case "$DEPLOY_DIR" in
    /|/boot|/etc|/usr|/bin|/sbin|/home|"$HOME")
        die "DEPLOY_DIR '$DEPLOY_DIR' looks like a destructive mistake."
        ;;
esac

# --- Dry run / interactive confirmation ------------------------------------
if [ "$DRY_RUN" -eq 1 ]; then
    echo "Dry run preview. Actions that would be executed:"
    echo "  1. Ensure Docker PostgreSQL (marble-erp-postgres) and MinIO (marble-minio) are running"
    echo "  2. Build frontend (npm install && npm run build) unless --skip-frontend"
    echo "  3. Package backend (./mvnw clean package -DskipTests)"
    echo "  4. Stop systemd service ($SERVICE_NAME)"
    echo "  5. Backup $LIVE_JAR -> ${LIVE_JAR}.bak.<timestamp>"
    echo "  6. Copy built JAR to $LIVE_JAR"
    echo "  7. Synchronize systemd unit $UNIT_PATH and restart $SERVICE_NAME"
    echo "  8. Verify health on 127.0.0.1:${SERVER_PORT}${HEALTH_PATH} and public domain"
    exit 0
fi

if [ "$YES" -ne 1 ]; then
    if [ -t 0 ]; then
        echo -n "Deploy Özerler Mermer ERP to production on this host? [Y/n]: "
        read -r reply
        reply="${reply:-y}"
        if [[ "$reply" =~ ^[Yy]$ ]]; then
            YES=1
        else
            echo "Deployment cancelled."
            exit 0
        fi
    else
        echo "Non-interactive shell without --yes or -y." >&2
        echo "Run with -y or --yes to execute deployment automatically." >&2
        exit 1
    fi
fi

need_cmd java
need_cmd curl
JAVA_MAJOR="$(java_major_version)"
if [ "${JAVA_MAJOR:-0}" -lt 24 ]; then
    die "JDK 24+ is required. Current major version: ${JAVA_MAJOR:-unknown}"
fi

if [ ! -x "$REPO_ROOT/mvnw" ]; then
    chmod +x "$REPO_ROOT/mvnw" || true
fi
[ -x "$REPO_ROOT/mvnw" ] || die "Maven wrapper not executable: $REPO_ROOT/mvnw"

# --- Deploy directory & environment file -----------------------------------
sudo_cmd mkdir -p "$DEPLOY_DIR/uploads" "$DEPLOY_DIR/logs" "$DEPLOY_DIR/media/images" "$DEPLOY_DIR/media/documents"

# Legacy media/uploads stay on disk so existing files can be migrated into MinIO.
# They are no longer the live upload target.
MEDIA_DIR="${DEPLOY_DIR}/media"
TARGET_USER="${SERVICE_USER:-eyuce}"
TARGET_GROUP="${SERVICE_GROUP:-eyuce}"
echo "[STORAGE] Preserving legacy media directories for MinIO migration: $MEDIA_DIR and $DEPLOY_DIR/uploads"
sudo_cmd chown -R "${TARGET_USER}:${TARGET_GROUP}" "$MEDIA_DIR" "$DEPLOY_DIR/uploads" || true
sudo_cmd chmod -R u+rwX "$MEDIA_DIR" "$DEPLOY_DIR/uploads" || true

ensure_minio_env_keys() {
    if [ ! -f "$ENV_FILE" ]; then
        return 0
    fi
    if grep -q '^MINIO_ENDPOINT=' "$ENV_FILE"; then
        return 0
    fi
    echo "[ENV] Appending MinIO object-storage settings to $ENV_FILE"
    sudo_cmd tee -a "$ENV_FILE" >/dev/null <<'EOF'

# MinIO object storage (S3-compatible). Required after the filesystem-to-MinIO migration.
STORAGE_TYPE=minio
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_PUBLIC_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=CHANGE_ME
MINIO_SECRET_KEY=CHANGE_ME
MINIO_BUCKET=erp-files
MINIO_REGION=us-east-1
MINIO_ROOT_USER=CHANGE_ME
MINIO_ROOT_PASSWORD=CHANGE_ME
MINIO_DATA_DIR=/opt/minio/data
EOF
}

if [ ! -f "$ENV_FILE" ]; then
    echo "[ENV] Writing initial $ENV_FILE"
    write_env_template
else
    ensure_minio_env_keys
fi
if grep -Eq 'CHANGE_ME|YOUR_PUBLIC_HOST|YOUR_STRONG' "$ENV_FILE"; then
    die "$ENV_FILE still contains placeholders (CHANGE_ME / YOUR_PUBLIC_HOST). Edit real values (including MinIO keys) and re-run."
fi

# Ensure correct base deploy directory permissions
sudo_cmd chown "${SERVICE_USER}:${SERVICE_GROUP}" "$DEPLOY_DIR" "$DEPLOY_DIR/uploads" "$DEPLOY_DIR/logs" || true
sudo_cmd chmod 640 "$ENV_FILE"

install_inapp_deploy_support

# --- Database health check (Docker PostgreSQL) -----------------------------
if command -v docker >/dev/null 2>&1; then
    if docker inspect marble-erp-postgres >/dev/null 2>&1; then
        if ! docker inspect -f '{{.State.Running}}' marble-erp-postgres 2>/dev/null | grep -q "true"; then
            echo "[DB] Starting Docker container 'marble-erp-postgres'..."
            docker start marble-erp-postgres
            sleep 2
        fi
        if docker exec marble-erp-postgres pg_isready -U marbleuser -d marble_erp -q 2>/dev/null; then
            echo "[DB] Docker container 'marble-erp-postgres' is healthy and ready."
        fi
    fi

    MINIO_DATA_DIR_VALUE="$(grep -E '^MINIO_DATA_DIR=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2- | tr -d ' "' || true)"
    MINIO_DATA_DIR_VALUE="${MINIO_DATA_DIR_VALUE:-/opt/minio/data}"
    sudo_cmd mkdir -p "$MINIO_DATA_DIR_VALUE"
    if docker inspect marble-minio >/dev/null 2>&1; then
        if ! docker inspect -f '{{.State.Running}}' marble-minio 2>/dev/null | grep -q "true"; then
            echo "[MINIO] Starting Docker container 'marble-minio'..."
            docker start marble-minio
            sleep 2
        fi
    elif [ -f "$REPO_ROOT/docker/docker-compose.yml" ]; then
        echo "[MINIO] Creating MinIO container from docker/docker-compose.yml (data: $MINIO_DATA_DIR_VALUE)"
        MINIO_ROOT_USER_VALUE="$(grep -E '^MINIO_ROOT_USER=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2- | tr -d ' "' || true)"
        MINIO_ROOT_PASSWORD_VALUE="$(grep -E '^MINIO_ROOT_PASSWORD=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2- | tr -d ' "' || true)"
        if [ -z "$MINIO_ROOT_USER_VALUE" ]; then
            MINIO_ROOT_USER_VALUE="$(grep -E '^MINIO_ACCESS_KEY=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2- | tr -d ' "' || true)"
        fi
        if [ -z "$MINIO_ROOT_PASSWORD_VALUE" ]; then
            MINIO_ROOT_PASSWORD_VALUE="$(grep -E '^MINIO_SECRET_KEY=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2- | tr -d ' "' || true)"
        fi
        (cd "$REPO_ROOT" && \
            MINIO_DATA_DIR="$MINIO_DATA_DIR_VALUE" \
            MINIO_ROOT_USER="$MINIO_ROOT_USER_VALUE" \
            MINIO_ROOT_PASSWORD="$MINIO_ROOT_PASSWORD_VALUE" \
            docker compose -f docker/docker-compose.yml up -d minio)
    fi
    if docker inspect marble-minio >/dev/null 2>&1; then
        echo "[MINIO] Docker container 'marble-minio' is present. Backup this data directory: $MINIO_DATA_DIR_VALUE"
    fi
fi

# --- Build (frontend + Maven fat JAR) --------------------------------------
cd "$REPO_ROOT"

if [ "$SKIP_BUILD" -eq 0 ]; then
    if [ "$SKIP_FRONTEND" -eq 0 ]; then
        need_cmd npm
        echo "[BUILD] frontend: npm install && npm run build"
        (cd "$REPO_ROOT/frontend" && npm install && npm run build)
    else
        echo "[SKIP] frontend build"
    fi
    echo "[BUILD] ./mvnw clean package -DskipTests"
    "$REPO_ROOT/mvnw" clean package -DskipTests
fi

[ -f "$BUILT_JAR" ] || die "Built JAR not found: $BUILT_JAR"

# --- Stop live service safely ----------------------------------------------
stop_live_jar_jvm() {
    local pids
    pids="$(pgrep -f "$LIVE_JAR" || true)"
    if [ -z "${pids}" ]; then
        return 0
    fi
    echo "[STOP] Sending SIGTERM to JVM(s) running $LIVE_JAR: $pids"
    # shellcheck disable=SC2086
    sudo_cmd kill $pids || true
    local i=0
    while [ "$i" -lt 20 ]; do
        pids="$(pgrep -f "$LIVE_JAR" || true)"
        [ -z "${pids}" ] && return 0
        sleep 1
        i=$((i + 1))
    done
    pids="$(pgrep -f "$LIVE_JAR" || true)"
    if [ -n "${pids}" ]; then
        echo "[STOP] SIGKILL leftover $LIVE_JAR JVM(s): $pids"
        # shellcheck disable=SC2086
        sudo_cmd kill -9 $pids || true
    fi
}

if systemctl list-unit-files "${SERVICE_NAME}.service" >/dev/null 2>&1 \
    && systemctl cat "${SERVICE_NAME}.service" >/dev/null 2>&1; then
    echo "[STOP] sudo systemctl stop ${SERVICE_NAME}"
    sudo_cmd systemctl stop "${SERVICE_NAME}" || true
else
    echo "[STOP] systemd unit ${SERVICE_NAME}.service not installed yet"
fi
stop_live_jar_jvm

# --- Backup + replace JAR ---------------------------------------------------
STAMP="$(date +%Y%m%d_%H%M%S)"
if [ -f "$LIVE_JAR" ]; then
    BACKUP_JAR="${LIVE_JAR}.bak.${STAMP}"
    echo "[BACKUP] $LIVE_JAR -> $BACKUP_JAR"
    sudo_cmd cp -a "$LIVE_JAR" "$BACKUP_JAR"
fi

echo "[INSTALL] $BUILT_JAR -> $LIVE_JAR"
sudo_cmd cp -a "$BUILT_JAR" "$LIVE_JAR"
sudo_cmd chown "${SERVICE_USER}:${SERVICE_GROUP}" "$LIVE_JAR" "$DEPLOY_DIR/uploads" "$DEPLOY_DIR/logs" || true

# Keep the last 8 backups
sudo_cmd bash -c "ls -1t '${LIVE_JAR}.bak.'* 2>/dev/null | tail -n +9 | xargs -r rm -f" || true

# --- systemd unit synchronization -------------------------------------------
if [ ! -f "$UNIT_PATH" ] || ! grep -q "EnvironmentFile=" "$UNIT_PATH" 2>/dev/null || ! grep -q "$LIVE_JAR" "$UNIT_PATH" 2>/dev/null; then
    echo "[UNIT] Installing/updating $UNIT_PATH"
    default_unit | sudo_cmd tee "$UNIT_PATH" >/dev/null
    sudo_cmd systemctl daemon-reload
    sudo_cmd systemctl enable "${SERVICE_NAME}"
else
    echo "[UNIT] Leaving existing $UNIT_PATH unchanged"
    sudo_cmd systemctl daemon-reload
fi

echo "[START] sudo systemctl start ${SERVICE_NAME}"
sudo_cmd systemctl start "${SERVICE_NAME}"

# --- Health check ----------------------------------------------------------
HEALTH_URL="http://127.0.0.1:${SERVER_PORT}${HEALTH_PATH}"
ACTUATOR_URL="http://127.0.0.1:${SERVER_PORT}/actuator/health"
echo "[HEALTH] Waiting for $HEALTH_URL"
ok=0
for i in $(seq 1 24); do
    health_resp="$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$HEALTH_URL" 2>/dev/null || true)"
    http_code="$(echo "$health_resp" | grep "HTTP_STATUS:" | cut -d':' -f2)"
    health_body="$(echo "$health_resp" | grep -v "HTTP_STATUS:")"

    if [ "$http_code" = "200" ] && echo "$health_body" | grep -q "UP"; then
        echo "[OK] $HEALTH_URL is UP (HTTP 200)"
        ok=1
        break
    fi

    if [ -n "$health_body" ] && [ "$http_code" != "000" ] && [ -n "$http_code" ]; then
        echo "  ... waiting ($i/24) [HTTP $http_code: $(echo "$health_body" | tr -d '\n\r' | cut -c1-140)]"
    else
        echo "  ... waiting ($i/24)"
    fi
    sleep 5
done

if [ "$ok" -ne 1 ]; then
    echo "[FAIL] Health check timed out. Recent logs:" >&2
    sudo_cmd journalctl -u "${SERVICE_NAME}" -n 80 --no-pager >&2 || true
    die "Service did not become healthy. JAR backup(s) are under ${DEPLOY_DIR}/app.jar.bak.*"
fi

if curl -sf "$ACTUATOR_URL" >/dev/null; then
    echo "[OK] $ACTUATOR_URL reachable"
fi

# Probe public domain if configured
PUBLIC_ORIGIN="$(grep -E '^CORS_ALLOWED_ORIGIN=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2- | cut -d',' -f1 | tr -d ' "' || true)"
if [[ "$PUBLIC_ORIGIN" =~ ^https?:// ]] && [[ "$PUBLIC_ORIGIN" != *"localhost"* ]] && [[ "$PUBLIC_ORIGIN" != *"127.0.0.1"* ]]; then
    PUBLIC_HEALTH_URL="${PUBLIC_ORIGIN}${HEALTH_PATH}"
    echo "[PUBLIC] Verifying public domain: $PUBLIC_HEALTH_URL"
    if curl -sf -m 8 "$PUBLIC_HEALTH_URL" | grep -q "UP"; then
        echo "[OK] Public URL $PUBLIC_HEALTH_URL is UP and healthy!"
    else
        echo "[INFO] Public URL probe did not immediately return UP (proxy/DNS may take a moment)."
    fi
fi

echo
echo "======================================================================"
echo "   Özerler Mermer ERP — Deploy complete!"
echo "======================================================================"
echo "  Public URL: ${PUBLIC_ORIGIN:-https://ozerler.naklink.com}"
echo "  Status:     sudo systemctl status ${SERVICE_NAME}"
echo "  Logs:       sudo journalctl -u ${SERVICE_NAME} -f -n 100"
echo "  Health:     $HEALTH_URL"
echo "  In-app:     /admin/deployment"
