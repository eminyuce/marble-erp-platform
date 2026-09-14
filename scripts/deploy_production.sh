#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — Production deploy (Linux host)
#
# Use this instead of scripts/run_local.sh on the server.
# See docs/production-deploy.md
#
# What this does:
#   1. Build frontend (npm install && npm run build) unless --skip-frontend
#   2. ./mvnw clean package -DskipTests
#   3. Stop marble-erp via systemd, or the /opt/marble-erp/app.jar JVM only
#   4. Backup the live JAR, copy target/*.jar -> /opt/marble-erp/app.jar
#   5. Start systemd unit (install a default unit if none exists)
#   6. Health-check http://127.0.0.1:${SERVER_PORT}/health/
#
# Never uses spring-boot:run or killall java.
# ==============================================================================

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: ./scripts/deploy_production.sh --yes [options]

  --yes              Required. Actually stop/replace/start production.
  --skip-frontend    Skip npm install && npm run build
  --skip-build       Skip frontend and Maven; only restart/replace if a JAR
                     already exists at target/marble-erp-platform-1.0.0.jar
  --print-unit       Print the default systemd unit and exit
  -h, --help         Show this help

Environment (optional):
  DEPLOY_DIR         Default: /opt/marble-erp
  SERVICE_NAME       Default: marble-erp
  SERVICE_USER       Default: current user (eyuce on this host)
  SERVER_PORT        Default: 8080 (must match marble-erp.env)
  HEALTH_PATH        Default: /health/

This script refuses to run without --yes, refuses non-Linux hosts, and
refuses a DEPLOY_DIR other than /opt/marble-erp unless you also set
ALLOW_CUSTOM_DEPLOY_DIR=1.
EOF
}

YES=0
SKIP_FRONTEND=0
SKIP_BUILD=0
PRINT_UNIT=0

for arg in "$@"; do
    case "$arg" in
        --yes) YES=1 ;;
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
SERVICE_USER="${SERVICE_USER:-$(id -un)}"
SERVICE_GROUP="${SERVICE_GROUP:-$(id -gn)}"
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

default_unit() {
    cat <<EOF
[Unit]
Description=Ozerler Mermer ERP Platform
After=network.target postgresql.service
Wants=postgresql.service

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

# Required on bare metal: prod YAML defaults DB_HOST to "postgres" (Docker DNS).
DB_URL=jdbc:postgresql://127.0.0.1:5432/marble_erp
DB_USERNAME=marbleuser
DB_PASSWORD=CHANGE_ME

APP_UPLOAD_DIR=/opt/marble-erp/uploads
CORS_ALLOWED_ORIGIN=https://YOUR_PUBLIC_HOST
EOF
    sudo_cmd chmod 640 "$ENV_FILE"
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

# --- Safety: this is not run_local.sh ---------------------------------------
if [ "$(uname -s)" != "Linux" ]; then
    die "This script is for the Linux production host only. Use scripts/run_local.sh for local/dev."
fi

if [ ! -f "$REPO_ROOT/pom.xml" ] || ! grep -q '<artifactId>marble-erp-platform</artifactId>' "$REPO_ROOT/pom.xml"; then
    die "Repo root does not look like marble-erp-platform (pom.xml artifactId). Cowardly refusing."
fi

if [ "$DEPLOY_DIR" != "/opt/marble-erp" ] && [ "${ALLOW_CUSTOM_DEPLOY_DIR:-}" != "1" ]; then
    die "DEPLOY_DIR is '$DEPLOY_DIR' (expected /opt/marble-erp). Set ALLOW_CUSTOM_DEPLOY_DIR=1 if intentional."
fi

case "$DEPLOY_DIR" in
    /|/boot|/etc|/usr|/bin|/sbin|/home|"$HOME")
        die "DEPLOY_DIR '$DEPLOY_DIR' looks like a destructive mistake."
        ;;
esac

if [ "$YES" -ne 1 ]; then
    echo "Dry run. Would:"
    echo "  1. Build frontend + ./mvnw clean package -DskipTests (unless skipped)"
    echo "  2. systemctl stop $SERVICE_NAME (or stop only the $LIVE_JAR JVM)"
    echo "  3. Backup $LIVE_JAR -> ${LIVE_JAR}.bak.<timestamp>"
    echo "  4. Copy $BUILT_JAR -> $LIVE_JAR"
    echo "  5. systemctl start $SERVICE_NAME (install unit if missing)"
    echo "  6. curl http://127.0.0.1:${SERVER_PORT}${HEALTH_PATH}"
    echo
    echo "Re-run with --yes to apply. See docs/production-deploy.md"
    exit 1
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

# --- Deploy directory + env (fail fast before a long build) -----------------
sudo_cmd mkdir -p "$DEPLOY_DIR/uploads" "$DEPLOY_DIR/logs"
if [ ! -f "$ENV_FILE" ]; then
    echo "[ENV] Writing template $ENV_FILE"
    write_env_template
    die "$ENV_FILE is new and still has placeholders. Set DB_URL / DB_PASSWORD / CORS_ALLOWED_ORIGIN, then re-run with --yes."
fi
if grep -Eq 'CHANGE_ME|YOUR_PUBLIC_HOST|YOUR_STRONG' "$ENV_FILE"; then
    die "$ENV_FILE still contains placeholders (CHANGE_ME / YOUR_PUBLIC_HOST). Edit real values and re-run."
fi

# --- Build (docs/production-deploy.md § Build) ------------------------------
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

# --- Stop safely (docs/production-deploy.md § Stop / start / restart) -------
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

# --- systemd start (docs/production-deploy.md § systemd unit) ---------------
if [ ! -f "$UNIT_PATH" ]; then
    echo "[UNIT] Installing $UNIT_PATH"
    default_unit | sudo_cmd tee "$UNIT_PATH" >/dev/null
    sudo_cmd systemctl daemon-reload
    sudo_cmd systemctl enable "${SERVICE_NAME}"
else
    echo "[UNIT] Leaving existing $UNIT_PATH unchanged"
    sudo_cmd systemctl daemon-reload
fi

echo "[START] sudo systemctl start ${SERVICE_NAME}"
sudo_cmd systemctl start "${SERVICE_NAME}"

# --- Health (docs/production-deploy.md § Health check) ----------------------
HEALTH_URL="http://127.0.0.1:${SERVER_PORT}${HEALTH_PATH}"
ACTUATOR_URL="http://127.0.0.1:${SERVER_PORT}/actuator/health"
echo "[HEALTH] Waiting for $HEALTH_URL"
ok=0
for i in $(seq 1 24); do
    if curl -sf "$HEALTH_URL" | grep -q "UP"; then
        echo "[OK] $HEALTH_URL is UP"
        ok=1
        break
    fi
    echo "  ... waiting ($i/24)"
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

echo
echo "Deploy complete."
echo "  status: sudo systemctl status ${SERVICE_NAME}"
echo "  logs:   sudo journalctl -u ${SERVICE_NAME} -f -n 100"
echo "  health: $HEALTH_URL"
