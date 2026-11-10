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
    local started_at
    started_at="$(iso_now)"
    prepare_log
    write_status "RUNNING" "$started_at" "" ""
    local unit_home="${HOME:-}"
    if [ -z "$unit_home" ]; then
        unit_home="$(getent passwd "$(id -un)" 2>/dev/null | cut -d: -f6 || true)"
    fi
    unit_home="${unit_home:-/root}"
    if ! systemd-run \
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
            --branch "$GIT_BRANCH" >> "$LOG_FILE" 2>&1; then
        echo "[FAIL] systemd-run başlatılamadı." >> "$LOG_FILE"
        write_status "FAILED" "$started_at" "$(iso_now)" "1"
        exit 1
    fi
}

detach_into_sibling_container() {
    local host_stack="${MARBLE_HOST_STACK_DIR:-}"
    local host_logs="${MARBLE_HOST_LOG_DIR:-}"
    local app_image="${DOCKERHUB_USERNAME:-guvenulu}/marble-erp-platform:${APP_TAG:-latest}"
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
    prepare_log
    write_status "RUNNING" "$started_at" "" ""
    local rollout_script
    rollout_script="$(dirname "$LOG_FILE")/rollout.sh"
    cat > "$rollout_script" <<EOF
#!/bin/sh
LOG=/logs/inapp-deploy.log
STATUS=/logs/inapp-deploy.status
LAST_GOOD=/logs/last-known-good.env
STARTED='${started_at}'
APP_IMAGE='${app_image}'
IMAGE_REPO='${DOCKERHUB_USERNAME:-guvenulu}/marble-erp-platform'
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
    chown 1001:1001 "\$STATUS" "\$LOG" 2>/dev/null || true
    chmod 664 "\$STATUS" "\$LOG" 2>/dev/null || true
}
wait_for_app_health() {
    echo "Uygulama konteynerinin sağlığı bekleniyor."
    i=0
    while [ "\$i" -lt 60 ]; do
        health=\$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' marble-erp 2>/dev/null || echo missing)
        echo "health=\$health"
        if [ "\$health" = "healthy" ]; then
            health_json=\$(docker exec marble-erp curl -fsS http://127.0.0.1:81/health/ 2>/dev/null || true)
            case "\$health_json" in
                *'"status":"UP"'*)
                    echo "[OK] Spring Boot /health/ doğrulandı (database=UP, rustfs=UP)."
                    echo "[OK] Docker Hub imajı yayında."
                    return 0
                    ;;
            esac
            echo "[OK] Docker Hub imajı yayında."
            return 0
        fi
        cstate=\$(docker inspect -f '{{.State.Status}}' marble-erp 2>/dev/null || echo missing)
        if [ "\$cstate" = "exited" ] || [ "\$cstate" = "dead" ]; then
            echo "[FAIL] Uygulama konteyneri durdu (durum=\$cstate)."
            docker logs --tail 30 marble-erp 2>&1 || true
            return 1
        fi
        i=\$((i + 1))
        sleep 3
    done
    echo "[FAIL] Uygulama konteyneri sağlıklı olmadı."
    docker logs --tail 30 marble-erp 2>&1 || true
    return 1
}
write_status RUNNING ""
code=0
STACK='${host_stack}'
{
    if [ -d "\$STACK/.env" ]; then
        rmdir "\$STACK/.env" 2>/dev/null || true
    fi
    prev_id=\$(docker inspect -f '{{.Image}}' marble-erp 2>/dev/null || true)
    prev_health=\$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' marble-erp 2>/dev/null || true)
    if [ -n "\$prev_id" ] && [ "\$prev_health" = "healthy" ]; then
        prev_digest=\$(docker image inspect -f '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "\$prev_id" 2>/dev/null || true)
        prev_rev=\$(docker image inspect -f '{{index .Config.Labels "org.opencontainers.image.revision"}}' "\$prev_id" 2>/dev/null || true)
        docker tag "\$prev_id" "\${IMAGE_REPO}:rollback-previous" 2>/dev/null || true
        cat > "\$LAST_GOOD" <<GOOD_EOF
PREV_IMAGE_REF=\$APP_IMAGE
PREV_IMAGE_ID=\$prev_id
PREV_IMAGE_DIGEST=\$prev_digest
PREV_COMMIT_SHA=\$prev_rev
RECORDED_AT=\$STARTED
GOOD_EOF
        chown 1001:1001 "\$LAST_GOOD" 2>/dev/null || true
        echo "[INFO] Önceki sağlıklı imaj rollback için kaydedildi (\$prev_id)."
    fi
    echo "\$ docker pull \$APP_IMAGE"
    docker pull "\$APP_IMAGE" || code=1
    if [ "\$code" -eq 0 ]; then
        cid=\$(docker create "\$APP_IMAGE" 2>/dev/null || true)
        if [ -n "\$cid" ]; then
            if docker cp "\$cid:/stack/docker-compose.prod.yml" "\$STACK/docker-compose.prod.yml.tmp" 2>/dev/null && [ -s "\$STACK/docker-compose.prod.yml.tmp" ]; then
                mv -f "\$STACK/docker-compose.prod.yml.tmp" "\$STACK/docker-compose.prod.yml"
                chmod 644 "\$STACK/docker-compose.prod.yml" 2>/dev/null || true
            else
                rm -f "\$STACK/docker-compose.prod.yml.tmp"
            fi
            docker rm -f "\$cid" >/dev/null 2>&1 || true
        fi
    fi
    if [ "\$code" -eq 0 ] && [ ! -f "\$STACK/docker-compose.prod.yml" ]; then
        echo "Sunucuda compose dosyası yok: \$STACK/docker-compose.prod.yml"
        code=1
    elif [ "\$code" -eq 0 ]; then
        pg_running=\$(docker inspect -f '{{.State.Running}}' marble-erp-postgres 2>/dev/null || echo false)
        rustfs_running=\$(docker inspect -f '{{.State.Running}}' marble-rustfs 2>/dev/null || echo false)
        if [ -f "\$STACK/.env" ]; then
            echo "\$ docker compose -f docker-compose.prod.yml pull app"
            docker compose --project-directory "\$STACK" --env-file "\$STACK/.env" -f "\$STACK/docker-compose.prod.yml" pull app || code=1
            if [ "\$code" -eq 0 ]; then
                if [ "\$pg_running" = "true" ] && [ "\$rustfs_running" = "true" ]; then
                    echo "\$ docker compose -f docker-compose.prod.yml up -d --no-deps app"
                    docker compose --project-directory "\$STACK" --env-file "\$STACK/.env" -f "\$STACK/docker-compose.prod.yml" up -d --no-deps app || code=1
                else
                    echo "\$ docker compose -f docker-compose.prod.yml up -d --no-recreate"
                    docker compose --project-directory "\$STACK" --env-file "\$STACK/.env" -f "\$STACK/docker-compose.prod.yml" up -d --no-recreate || code=1
                    docker compose --project-directory "\$STACK" --env-file "\$STACK/.env" -f "\$STACK/docker-compose.prod.yml" up -d --no-deps app || code=1
                fi
            fi
        else
            echo "\$ docker compose -f docker-compose.prod.yml pull app"
            docker compose --project-directory "\$STACK" -f "\$STACK/docker-compose.prod.yml" pull app || code=1
            if [ "\$code" -eq 0 ]; then
                if [ "\$pg_running" = "true" ] && [ "\$rustfs_running" = "true" ]; then
                    echo "\$ docker compose -f docker-compose.prod.yml up -d --no-deps app"
                    docker compose --project-directory "\$STACK" -f "\$STACK/docker-compose.prod.yml" up -d --no-deps app || code=1
                else
                    echo "\$ docker compose -f docker-compose.prod.yml up -d --no-recreate"
                    docker compose --project-directory "\$STACK" -f "\$STACK/docker-compose.prod.yml" up -d --no-recreate || code=1
                    docker compose --project-directory "\$STACK" -f "\$STACK/docker-compose.prod.yml" up -d --no-deps app || code=1
                fi
            fi
        fi
    fi
    if [ "\$code" -eq 0 ]; then
        if ! wait_for_app_health; then
            code=1
            if docker image inspect "\${IMAGE_REPO}:rollback-previous" >/dev/null 2>&1; then
                echo "[WARN] Yeni sürüm sağlık kontrolünü geçemedi. Otomatik geri alma (rollback) başlatılıyor..."
                docker tag "\${IMAGE_REPO}:rollback-previous" "\$APP_IMAGE"
                if [ -f "\$STACK/.env" ]; then
                    docker compose --project-directory "\$STACK" --env-file "\$STACK/.env" -f "\$STACK/docker-compose.prod.yml" up -d --no-deps app || true
                else
                    docker compose --project-directory "\$STACK" -f "\$STACK/docker-compose.prod.yml" up -d --no-deps app || true
                fi
                if wait_for_app_health; then
                    echo "[OK] Otomatik geri alma tamamlandı; önceki sağlıklı sürüm yayında."
                else
                    echo "[FAIL] Otomatik geri alma sonrasında da sağlık kontrolü başarısız oldu."
                fi
            fi
        fi
    fi
} >> "\$LOG" 2>&1
if [ "\$code" -eq 0 ]; then
    write_status SUCCESS 0
else
    write_status FAILED "\$code"
fi
chown -R 1001:1001 /logs 2>/dev/null || true
exit "\$code"
EOF
    chmod 755 "$rollout_script" 2>/dev/null || true

    if ! docker run -d \
        --name "$ROLLOUT_NAME" \
        --label marble-erp.rollout=true \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v "${host_stack}:${host_stack}" \
        -v "${host_logs}:/logs" \
        docker:27.5.1-cli \
        sh /logs/rollout.sh >> "$LOG_FILE" 2>&1; then
        echo "[FAIL] Rollout konteyneri başlatılamadı." >> "$LOG_FILE"
        write_status "FAILED" "$started_at" "$(iso_now)" "1"
        exit 1
    fi
}

if [ "$FOREGROUND" -ne 1 ]; then
    # Inside the app container /stack is optional. The rollout container
    # mounts MARBLE_HOST_STACK_DIR from the host and reads the compose file there.
    if [ "${MARBLE_IN_CONTAINER:-}" != "true" ]; then
        require_compose_file
    fi
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

echo "\$ docker compose -f docker-compose.prod.yml pull app"
docker compose "${compose_args[@]}" pull app

pg_running="$(docker inspect -f '{{.State.Running}}' marble-erp-postgres 2>/dev/null || echo false)"
rustfs_running="$(docker inspect -f '{{.State.Running}}' marble-rustfs 2>/dev/null || echo false)"
if [ "$pg_running" = "true" ] && [ "$rustfs_running" = "true" ]; then
    echo "\$ docker compose -f docker-compose.prod.yml up -d --no-deps app"
    docker compose "${compose_args[@]}" up -d --no-deps app
else
    echo "\$ docker compose -f docker-compose.prod.yml up -d --no-recreate"
    docker compose "${compose_args[@]}" up -d --no-recreate
    docker compose "${compose_args[@]}" up -d --no-deps app
fi

if command -v systemctl >/dev/null 2>&1 && systemctl is-active --quiet marble-erp.service; then
    echo "NOT: systemd marble-erp hâlâ çalışıyor. Yeni yığın sağlıklıysa eski JAR servisini durdurun."
fi

echo
echo "[OK] Docker Hub imajı çekildi ve yığın yenilendi."
