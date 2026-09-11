#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP Platformu - Dummy Data Loader Script (Bash)
# Loads seed_dummy_data.sql into local MySQL or running Docker container
# ==============================================================================

set -e

DB_USER="${DB_USER:-marble_user}"
DB_PASS="${DB_PASS:-marble_pass}"
DB_NAME="${DB_NAME:-marble_erp}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
SQL_FILE="${1:-scripts/seed_dummy_data.sql}"
CONTAINER_NAME="marble-mysql"

echo "======================================================================"
echo "   Özerler Mermer ERP - Test & Demo Veri Yükleme Aracı (Seed Script)  "
echo "======================================================================"

if [ ! -f "$SQL_FILE" ]; then
    echo "Error: SQL file not found: $SQL_FILE"
    exit 1
fi

if docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null | grep -q "true"; then
    echo "[DOCKER DETECTED] Found active container '$CONTAINER_NAME'."
    echo "[IMPORTING] Importing $SQL_FILE into container..."
    docker exec -i "$CONTAINER_NAME" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL_FILE"
    echo "✔ Success! Data seeded into Docker database '$DB_NAME'."
    exit 0
fi

if command -v mysql >/dev/null 2>&1; then
    echo "[LOCAL MYSQL] Found local mysql client. Connecting to $DB_HOST:$DB_PORT..."
    mysql --host="$DB_HOST" --port="$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL_FILE"
    echo "✔ Success! Data seeded into local database '$DB_NAME'."
    exit 0
fi

echo "Warning: Neither Docker '$CONTAINER_NAME' nor 'mysql' client was found."
echo "Please execute $SQL_FILE via your database administration tool (e.g. DBeaver, MySQL Workbench)."
