#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — Fresh Customer Installation Script
#
# Strictly separated from production upgrades!
# Refuses to run if existing production volumes or directories are detected.
# ==============================================================================

set -euo pipefail

INSTALL_DIR="${1:-./marble-erp-install}"

# Safety guard: NEVER run against the existing production environment
if [ -d "/opt/marble-erp" ] && [ -f "/opt/marble-erp/docker-compose.prod.yml" ]; then
    echo "[ABORT] Existing production installation detected at /opt/marble-erp." >&2
    echo "        Fresh-install initialization is strictly prohibited on a production host." >&2
    exit 1
fi

if docker volume ls --format '{{.Name}}' 2>/dev/null | grep -qx "docker_postgres_data"; then
    echo "[ABORT] Existing production volume 'docker_postgres_data' detected." >&2
    echo "        Refusing to run fresh installation on a host with production data." >&2
    exit 1
fi

mkdir -p "$INSTALL_DIR"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [ -f "${REPO_ROOT}/docker/docker-compose.yml" ]; then
    cp -n "${REPO_ROOT}/docker/docker-compose.yml" "${INSTALL_DIR}/docker-compose.yml"
fi
if [ -f "${REPO_ROOT}/.env.example" ] && [ ! -f "${INSTALL_DIR}/.env" ]; then
    cp "${REPO_ROOT}/.env.example" "${INSTALL_DIR}/.env"
    echo "[INFO] Created ${INSTALL_DIR}/.env from template. Update passwords before starting."
fi

echo "[OK] Fresh installation directory prepared at: ${INSTALL_DIR}"
echo "Next steps for a new customer installation:"
echo "  1. Edit ${INSTALL_DIR}/.env and set strong credentials for DB_PASSWORD and RUSTFS_SECRET_KEY."
echo "  2. Run: cd ${INSTALL_DIR} && docker compose pull && docker compose up -d"
echo "  3. Verify readiness: curl -fsS http://127.0.0.1:81/health/"
