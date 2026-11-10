#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — Controlled Production Restore Script (PostgreSQL + RustFS)
#
# Never runs automatically. Requires --confirm-restore and explicit file paths.
# Creates a pre-restore safety backup before applying any restoration.
# ==============================================================================

set -euo pipefail

PG_CONTAINER="${PG_DOCKER_CONTAINER:-marble-erp-postgres}"
DB_NAME="${DB_NAME:-marble_erp}"
DB_USERNAME="${DB_USERNAME:-marbleuser}"
RUSTFS_DATA_DIR="${RUSTFS_DATA_DIR:-/opt/rustfs/data}"
DB_BACKUP_FILE=""
RUSTFS_ARCHIVE=""
CONFIRMED=0

usage() {
    cat <<'EOF'
Usage: restore_production.sh --confirm-restore [--db-dump FILE] [--rustfs-archive FILE]

  --confirm-restore      Mandatory confirmation flag for production restore
  --db-dump FILE         PostgreSQL .dump or .sql.gz file to restore
  --rustfs-archive FILE  RustFS .tar.gz archive to restore
  -h, --help             Show this help
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --confirm-restore)
            CONFIRMED=1
            shift
            ;;
        --db-dump)
            DB_BACKUP_FILE="$2"
            shift 2
            ;;
        --rustfs-archive)
            RUSTFS_ARCHIVE="$2"
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

if [ "$CONFIRMED" -ne 1 ]; then
    echo "[FAIL] Restore requires explicit --confirm-restore approval flag." >&2
    exit 1
fi

if [ -z "$DB_BACKUP_FILE" ] && [ -z "$RUSTFS_ARCHIVE" ]; then
    echo "[FAIL] Specify at least one of --db-dump or --rustfs-archive." >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -x "${SCRIPT_DIR}/backup_production.sh" ]; then
    echo "[INFO] Taking pre-restore safety backup first..."
    "${SCRIPT_DIR}/backup_production.sh"
fi

if [ -n "$DB_BACKUP_FILE" ]; then
    if [ ! -f "$DB_BACKUP_FILE" ]; then
        echo "[FAIL] Database backup file not found: ${DB_BACKUP_FILE}" >&2
        exit 1
    fi
    echo "[INFO] Restoring PostgreSQL database '${DB_NAME}' from '${DB_BACKUP_FILE}'..."
    case "$DB_BACKUP_FILE" in
        *.dump)
            docker exec -i "$PG_CONTAINER" pg_restore -U "$DB_USERNAME" -d "$DB_NAME" --clean --if-exists < "$DB_BACKUP_FILE"
            ;;
        *.sql.gz)
            gunzip -c "$DB_BACKUP_FILE" | docker exec -i "$PG_CONTAINER" psql -U "$DB_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1
            ;;
        *.sql)
            docker exec -i "$PG_CONTAINER" psql -U "$DB_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1 < "$DB_BACKUP_FILE"
            ;;
        *)
            echo "[FAIL] Unsupported database dump format: ${DB_BACKUP_FILE}" >&2
            exit 1
            ;;
    esac
    echo "[OK] PostgreSQL restore completed."
fi

if [ -n "$RUSTFS_ARCHIVE" ]; then
    if [ ! -f "$RUSTFS_ARCHIVE" ]; then
        echo "[FAIL] RustFS archive not found: ${RUSTFS_ARCHIVE}" >&2
        exit 1
    fi
    tar -tzf "$RUSTFS_ARCHIVE" >/dev/null
    echo "[INFO] Restoring RustFS archive '${RUSTFS_ARCHIVE}' into '$(dirname "$RUSTFS_DATA_DIR")'..."
    tar -xzf "$RUSTFS_ARCHIVE" -C "$(dirname "$RUSTFS_DATA_DIR")"
    chown -R 10001:10001 "$RUSTFS_DATA_DIR" 2>/dev/null || true
    echo "[OK] RustFS restore completed."
fi
