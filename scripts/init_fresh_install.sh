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
echo "[OK] Fresh installation directory prepared at: ${INSTALL_DIR}"
echo "Next steps for a new customer installation:"
echo "  1. No .env file is required. Defaults are in the Spring profile and the compose file."
echo "  2. Before the first start, export real secrets if the built-in placeholders are not acceptable:"
echo "       export DB_PASSWORD=... RUSTFS_ACCESS_KEY=... RUSTFS_SECRET_KEY=... CORS_ALLOWED_ORIGIN=..."
echo "  3. Run: cd ${INSTALL_DIR} && docker compose pull && docker compose up -d"
echo "  4. Verify readiness: curl -fsS http://127.0.0.1:81/health/"
