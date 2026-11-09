#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — In-app production rollout
#
# The admin screen starts this script. It does not compile source and it does
# not reset git. GitHub Actions has already pushed the image to Docker Hub.
# This script pulls that image and recreates the Compose stack.
#
# The work is detached so it survives replacement of the app container:
#   - host JVM: systemd-run
#   - app container: a sibling "marble-erp-rollout" container
# ==============================================================================

set -euo pipefail

REPO_ROOT="${MARBLE_REPO_ROOT:-/home/eyuce/marble-erp-platform}"
LOG_FILE="${MARBLE_DEPLOY_LOG:-/opt/marble-erp/logs/inapp-deploy.log}"
STATUS_FILE="${MARBLE_DEPLOY_STATUS:-/opt/marble-erp/logs/inapp-deploy.status}"
LOCK_FILE="${MARBLE_DEPLOY_LOCK:-/opt/marble-erp/logs/inapp-deploy.lock}"
GIT_REMOTE="${MARBLE_GIT_REMOTE:-origin}"
GIT_BRANCH="${MARBLE_GIT_BRANCH:-main}"
UNIT_NAME="marble-erp-inapp-deploy"
ROLLOUT_NAME="marble-erp-rollout"
FOREGROUND=0

usage() {
    cat <<'EOF'
Usage: marble-erp-inapp-deploy [options]

  --ping             Print READY and exit.
  --foreground       Pull the Docker Hub image and recreate the stack.
  --repo DIR         Directory that contains docker-compose.prod.yml
  --log FILE         Deploy log path
  --status FILE      Status file path
  --lock FILE        flock path
  --remote NAME      Accepted for compatibility. Not used.
  --branch NAME      Accepted for compatibility. Not used.
  -h, --help         Show this help
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --ping)
            echo "READY"
            exit 0
            ;;
        --foreground)
            FOREGROUND=1
            shift
            ;;
        --repo)
            REPO_ROOT="$2"
            shift 2
            ;;
        --log)
            LOG_FILE="$2"
            shift 2
            ;;
        --status)
            STATUS_FILE="$2"
            shift 2
            ;;
        --lock)
            LOCK_FILE="$2"
            shift 2
            ;;
        --remote)
            GIT_REMOTE="$2"
            shift 2
            ;;
        --branch)
            GIT_BRANCH="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

write_status() {
    local state="$1"
    local started_at="${2:-}"
    local finished_at="${3:-}"
    local exit_code="${4:-}"
    mkdir -p "$(dirname "$STATUS_FILE")"
    cat > "$STATUS_FILE" <<EOF
STATE=${state}
STARTED_AT=${started_at}
FINISHED_AT=${finished_at}
EXIT_CODE=${exit_code}
EOF
    chmod 644 "$STATUS_FILE" 2>/dev/null || true
}

iso_now() {
    date -u +"%Y-%m-%dT%H:%M:%SZ"
}

prepare_log() {
    mkdir -p "$(dirname "$LOG_FILE")" "$(dirname "$LOCK_FILE")"
    : > "$LOG_FILE"
    chmod 644 "$LOG_FILE" 2>/dev/null || true
}

compose_file() {
    echo "${REPO_ROOT}/docker-compose.prod.yml"
}

require_compose_file() {
    if [ ! -f "$(compose_file)" ]; then
        echo "docker-compose.prod.yml bulunamadı: $(compose_file)" >&2
        exit 1
    fi
}

detach_into_independent_unit() {
    if ! command -v systemd-run >/dev/null 2>&1; then
        echo "systemd-run bulunamadı. Yayın, uygulama yeniden başlarken ayakta kalamaz." >&2
        exit 1
    fi
    systemctl reset-failed "${UNIT_NAME}.service" 2>/dev/null || true
    if systemctl is-active --quiet "${UNIT_NAME}.service"; then
        echo "Bir yayın zaten sürüyor (${UNIT_NAME}.service)." >&2
        exit 2
    fi
    local unit_home="${HOME:-}"
    if [ -z "$unit_home" ]; then
        unit_home="$(getent passwd "$(id -un)" 2>/dev/null | cut -d: -f6 || true)"
    fi
    unit_home="${unit_home:-/root}"
    systemd-run \
        --unit="${UNIT_NAME}" \
        --collect \
        --quiet \
        --property=KillMode=mixed \
        --working-directory="${REPO_ROOT}" \
        --setenv=HOME="$unit_home" \
        /bin/bash "$0" \
            --foreground \
            --repo "$REPO_ROOT" \
            --log "$LOG_FILE" \
            --status "$STATUS_FILE" \
            --lock "$LOCK_FILE" \
            --remote "$GIT_REMOTE" \
            --branch "$GIT_BRANCH"
}

detach_into_sibling_container() {
    local host_stack="${MARBLE_HOST_STACK_DIR:-}"
    local host_logs="${MARBLE_HOST_LOG_DIR:-}"
    if [ -z "$host_stack" ] || [ -z "$host_logs" ]; then
        echo "MARBLE_HOST_STACK_DIR ve MARBLE_HOST_LOG_DIR tanımlı değil." >&2
        exit 1
    fi
    if ! command -v docker >/dev/null 2>&1; then
        echo "İmajın içinde docker istemcisi yok." >&2
        exit 1
    fi
    if docker ps -q --filter "name=^${ROLLOUT_NAME}$" | grep -q .; then
        echo "Bir yayın zaten sürüyor (${ROLLOUT_NAME})." >&2
        exit 2
    fi
    docker rm -f "$ROLLOUT_NAME" >/dev/null 2>&1 || true

    local started_at
    started_at="$(iso_now)"
    local rollout_script
    rollout_script="$(dirname "$LOG_FILE")/rollout.sh"
    cat > "$rollout_script" <<EOF
#!/bin/sh
LOG=/logs/inapp-deploy.log
STATUS=/logs/inapp-deploy.status
STARTED='${started_at}'
write_status() {
    finished=""
    if [ "\$1" != "RUNNING" ]; then
        finished=\$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    fi
    cat > "\$STATUS" <<STATUS_EOF
STATE=\$1
STARTED_AT=\$STARTED
FINISHED_AT=\$finished
EXIT_CODE=\$2
STATUS_EOF
}
write_status RUNNING ""
code=0
{
    if [ -f /stack/.env ]; then
        echo "\$ docker compose -f docker-compose.prod.yml pull"
        docker compose --env-file /stack/.env -f /stack/docker-compose.prod.yml pull || code=1
        if [ "\$code" -eq 0 ]; then
            echo "\$ docker compose -f docker-compose.prod.yml up -d"
            docker compose --env-file /stack/.env -f /stack/docker-compose.prod.yml up -d || code=1
        fi
    else
        echo "\$ docker compose -f docker-compose.prod.yml pull"
        docker compose -f /stack/docker-compose.prod.yml pull || code=1
        if [ "\$code" -eq 0 ]; then
            echo "\$ docker compose -f docker-compose.prod.yml up -d"
            docker compose -f /stack/docker-compose.prod.yml up -d || code=1
        fi
    fi
    if [ "\$code" -eq 0 ]; then
        echo "Uygulama konteynerinin sağlığı bekleniyor."
        i=0
        ok=0
        while [ "\$i" -lt 40 ]; do
            health=\$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' marble-erp 2>/dev/null || echo missing)
            echo "health=\$health"
            if [ "\$health" = "healthy" ]; then
                echo "[OK] Docker Hub imajı yayında."
                ok=1
                break
            fi
            i=\$((i + 1))
            sleep 3
        done
        if [ "\$ok" -ne 1 ]; then
            echo "[FAIL] Uygulama konteyneri sağlıklı olmadı."
            code=1
        fi
    fi
} >> "\$LOG" 2>&1
if [ "\$code" -eq 0 ]; then
    write_status SUCCESS 0
else
    write_status FAILED "\$code"
fi
exit "\$code"
EOF
    chmod 755 "$rollout_script" 2>/dev/null || true

    docker run -d \
        --name "$ROLLOUT_NAME" \
        --label marble-erp.rollout=true \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v "${host_stack}:/stack:ro" \
        -v "${host_logs}:/logs" \
        docker:27.5.1-cli \
        sh /logs/rollout.sh
}

if [ "$FOREGROUND" -ne 1 ]; then
    require_compose_file
    prepare_log
    write_status "RUNNING" "$(iso_now)" "" ""
    if [ "${MARBLE_IN_CONTAINER:-}" = "true" ]; then
        detach_into_sibling_container
    else
        detach_into_independent_unit
    fi
    exit 0
fi

STARTED_AT="$(iso_now)"
write_status "RUNNING" "$STARTED_AT" "" ""

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
    echo "Yayın kilidi dolu: $LOCK_FILE" >&2
    write_status "FAILED" "$STARTED_AT" "$(iso_now)" "2"
    exit 2
fi

exec > >(tee -a "$LOG_FILE") 2>&1

cleanup() {
    local code=$?
    local finished
    finished="$(iso_now)"
    if [ "$code" -eq 0 ]; then
        write_status "SUCCESS" "$STARTED_AT" "$finished" "0"
    else
        write_status "FAILED" "$STARTED_AT" "$finished" "$code"
        echo "[FAIL] Yayın çıkış kodu $code"
    fi
}
trap cleanup EXIT

require_compose_file
if ! command -v docker >/dev/null 2>&1; then
    echo "Bu makinede docker komutu yok." >&2
    exit 1
fi

echo "======================================================================"
echo "   Özerler Mermer ERP — Docker Hub yayını"
echo "======================================================================"
echo "Started:  $STARTED_AT"
echo "Compose:  $(compose_file)"
echo "Log:      $LOG_FILE"
echo

cd "$REPO_ROOT"
compose_args=(-f "$(compose_file)")
if [ -f "${REPO_ROOT}/.env" ]; then
    compose_args=(--env-file "${REPO_ROOT}/.env" "${compose_args[@]}")
fi

echo "\$ docker compose -f docker-compose.prod.yml pull"
docker compose "${compose_args[@]}" pull

echo "\$ docker compose -f docker-compose.prod.yml up -d"
docker compose "${compose_args[@]}" up -d

if command -v systemctl >/dev/null 2>&1 && systemctl is-active --quiet marble-erp.service; then
    echo "NOT: systemd marble-erp hâlâ çalışıyor. Yeni yığın sağlıklıysa eski JAR servisini durdurun."
fi

echo
echo "[OK] Docker Hub imajı çekildi ve yığın yenilendi."
