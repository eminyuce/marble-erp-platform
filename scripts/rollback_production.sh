#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — Safe Application-Only Rollback Script
#
# Restores the previous known-good application Docker image and recreates ONLY
# the Marble ERP application container (`app`).
# Leaves PostgreSQL (`marble-erp-postgres`) and RustFS (`marble-rustfs`) untouched.
# ==============================================================================

set -euo pipefail

STACK_DIR="${MARBLE_HOST_STACK_DIR:-/opt/marble-erp}"
LOG_DIR="${MARBLE_HOST_LOG_DIR:-/opt/marble-erp/logs}"
LAST_GOOD_FILE="${LOG_DIR}/last-known-good.env"
TARGET_IMAGE=""

usage() {
    cat <<'EOF'
Usage: rollback_production.sh [options]

  --stack DIR     Directory containing docker-compose.prod.yml (default: /opt/marble-erp)
  --image IMAGE   Explicit image tag or digest to roll back to (default: rollback-previous)
  -h, --help      Show this help
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --stack)
            STACK_DIR="$2"
            shift 2
            ;;
        --image)
            TARGET_IMAGE="$2"
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

COMPOSE_FILE="${STACK_DIR}/docker-compose.prod.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "[FAIL] Compose file not found: ${COMPOSE_FILE}" >&2
    exit 1
fi

DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:-guvenulu}"
APP_TAG="${APP_TAG:-latest}"
if [ -f "${STACK_DIR}/.env" ]; then
    # shellcheck disable=SC1090
    set -a && . "${STACK_DIR}/.env" && set +a
fi

IMAGE_REPO="${DOCKERHUB_USERNAME}/marble-erp-platform"
ACTIVE_TAG="${IMAGE_REPO}:${APP_TAG}"

if [ -z "$TARGET_IMAGE" ]; then
    if docker image inspect "${IMAGE_REPO}:rollback-previous" >/dev/null 2>&1; then
        TARGET_IMAGE="${IMAGE_REPO}:rollback-previous"
    elif [ -f "$LAST_GOOD_FILE" ]; then
        # shellcheck disable=SC1090
        . "$LAST_GOOD_FILE"
        TARGET_IMAGE="${PREV_IMAGE_ID:-${PREV_IMAGE_DIGEST:-}}"
    fi
fi

if [ -z "$TARGET_IMAGE" ]; then
    echo "[FAIL] No rollback image found (${IMAGE_REPO}:rollback-previous or ${LAST_GOOD_FILE})." >&2
    exit 1
fi

echo "[INFO] Rolling back application image to: ${TARGET_IMAGE}"
if ! docker image inspect "$TARGET_IMAGE" >/dev/null 2>&1; then
    docker pull "$TARGET_IMAGE"
fi

docker tag "$TARGET_IMAGE" "$ACTIVE_TAG"

compose_args=(--project-directory "$STACK_DIR" -f "$COMPOSE_FILE")
if [ -f "${STACK_DIR}/.env" ]; then
    compose_args=(--project-directory "$STACK_DIR" --env-file "${STACK_DIR}/.env" -f "$COMPOSE_FILE")
fi

echo "[INFO] Recreating ONLY the marble-erp application container (--no-deps app)..."
docker compose "${compose_args[@]}" up -d --no-deps app

echo "[INFO] Waiting for rolled-back application health check..."
i=0
while [ "$i" -lt 60 ]; do
    health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' marble-erp 2>/dev/null || echo missing)"
    echo "health=${health}"
    if [ "$health" = "healthy" ]; then
        echo "[OK] Rollback succeeded. Application container is healthy; PostgreSQL and RustFS were untouched."
        exit 0
    fi
    i=$((i + 1))
    sleep 3
done

echo "[FAIL] Rolled-back container did not become healthy within timeout." >&2
docker logs --tail 30 marble-erp >&2 || true
exit 1
