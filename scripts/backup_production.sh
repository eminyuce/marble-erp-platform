#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — Production Backup Script (PostgreSQL + RustFS)
#
# Creates verified, non-overwriting backups outside the application container:
#   - PostgreSQL logical dump (custom .dump + plain .sql.gz + .sha256)
#   - RustFS object storage archive (.tar.gz + .sha256) without deleting source files
# ==============================================================================

set -euo pipefail

BACKUP_ROOT="${MARBLE_BACKUP_ROOT:-/opt/marble-erp/backups}"
PG_CONTAINER="${PG_DOCKER_CONTAINER:-marble-erp-postgres}"
DB_NAME="${DB_NAME:-marble_erp}"
DB_USERNAME="${DB_USERNAME:-marbleuser}"
RUSTFS_DATA_DIR="${RUSTFS_DATA_DIR:-/opt/rustfs/data}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
DO_DB=1
DO_RUSTFS=1
PRUNE_OLD=0

usage() {
    cat <<'EOF'
Usage: backup_production.sh [options]

  --backup-dir DIR       Root backup directory (default: /opt/marble-erp/backups)
  --db-only              Back up PostgreSQL only
  --rustfs-only          Back up RustFS data directory only
  --prune                Remove backups older than --retention-days
  --retention-days DAYS  Retention period in days when --prune is used (default: 14)
  -h, --help             Show this help
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --backup-dir)
            BACKUP_ROOT="$2"
            shift 2
            ;;
        --db-only)
            DO_DB=1
            DO_RUSTFS=0
            shift
            ;;
        --rustfs-only)
            DO_DB=0
            DO_RUSTFS=1
            shift
            ;;
        --prune)
            PRUNE_OLD=1
            shift
            ;;
        --retention-days)
            RETENTION_DAYS="$2"
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

TIMESTAMP="$(date -u +"%Y%m%d_%H%M%SZ")"
PG_BACKUP_DIR="${BACKUP_ROOT}/postgres"
RUSTFS_BACKUP_DIR="${BACKUP_ROOT}/rustfs"

mkdir -p "$PG_BACKUP_DIR" "$RUSTFS_BACKUP_DIR"

if [ "$DO_DB" -eq 1 ]; then
    if ! docker ps --format '{{.Names}}' | grep -qx "$PG_CONTAINER"; then
        echo "[FAIL] PostgreSQL container '${PG_CONTAINER}' is not running." >&2
        exit 1
    fi

    DUMP_FILE="${PG_BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.dump"
    SQL_GZ_FILE="${PG_BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

    if [ -e "$DUMP_FILE" ] || [ -e "$SQL_GZ_FILE" ]; then
        echo "[FAIL] Backup file already exists; refusing to overwrite: ${DUMP_FILE}" >&2
        exit 1
    fi

    echo "[INFO] Backing up PostgreSQL database '${DB_NAME}' from container '${PG_CONTAINER}'..."
    docker exec "$PG_CONTAINER" pg_dump -U "$DB_USERNAME" -d "$DB_NAME" -Fc > "$DUMP_FILE"
    docker exec "$PG_CONTAINER" pg_dump -U "$DB_USERNAME" -d "$DB_NAME" --clean --if-exists | gzip -c > "$SQL_GZ_FILE"

    if [ ! -s "$DUMP_FILE" ] || [ ! -s "$SQL_GZ_FILE" ]; then
        echo "[FAIL] PostgreSQL backup file is empty." >&2
        exit 1
    fi

    # Verify custom dump TOC
    docker exec -i "$PG_CONTAINER" pg_restore --list < "$DUMP_FILE" >/dev/null
    sha256sum "$DUMP_FILE" "$SQL_GZ_FILE" > "${DUMP_FILE}.sha256"
    chmod 600 "$DUMP_FILE" "$SQL_GZ_FILE" "${DUMP_FILE}.sha256" 2>/dev/null || true
    echo "[OK] PostgreSQL backup verified: ${DUMP_FILE}"
fi

if [ "$DO_RUSTFS" -eq 1 ]; then
    if [ ! -d "$RUSTFS_DATA_DIR" ]; then
        echo "[FAIL] RustFS data directory not found: ${RUSTFS_DATA_DIR}" >&2
        exit 1
    fi

    RUSTFS_ARCHIVE="${RUSTFS_BACKUP_DIR}/rustfs_data_${TIMESTAMP}.tar.gz"
    if [ -e "$RUSTFS_ARCHIVE" ]; then
        echo "[FAIL] RustFS backup archive already exists; refusing to overwrite: ${RUSTFS_ARCHIVE}" >&2
        exit 1
    fi

    echo "[INFO] Backing up RustFS storage directory '${RUSTFS_DATA_DIR}'..."
    tar -czf "$RUSTFS_ARCHIVE" -C "$(dirname "$RUSTFS_DATA_DIR")" "$(basename "$RUSTFS_DATA_DIR")"

    if [ ! -s "$RUSTFS_ARCHIVE" ]; then
        echo "[FAIL] RustFS backup archive is empty." >&2
        exit 1
    fi

    # Verify archive integrity without modifying source files
    tar -tzf "$RUSTFS_ARCHIVE" >/dev/null
    sha256sum "$RUSTFS_ARCHIVE" > "${RUSTFS_ARCHIVE}.sha256"
    chmod 600 "$RUSTFS_ARCHIVE" "${RUSTFS_ARCHIVE}.sha256" 2>/dev/null || true
    echo "[OK] RustFS backup verified: ${RUSTFS_ARCHIVE}"
fi

if [ "$PRUNE_OLD" -eq 1 ]; then
    echo "[INFO] Pruning backups older than ${RETENTION_DAYS} days..."
    find "$PG_BACKUP_DIR" -type f -mtime +"$RETENTION_DAYS" -delete
    find "$RUSTFS_BACKUP_DIR" -type f -mtime +"$RETENTION_DAYS" -delete
fi

echo "[OK] Backup completed successfully at ${TIMESTAMP}."
