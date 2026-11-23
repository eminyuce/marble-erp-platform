#!/usr/bin/env bash
# Encrypted backup of the secrets directory. Does not delete older backups.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/marble-erp}"
SECRETS_DIR="${SECRETS_DIR:-$DEPLOY_DIR/secrets}"
BACKUP_DIR="${BACKUP_DIR:-$DEPLOY_DIR/secret-backups}"
PASSPHRASE_FILE="${PASSPHRASE_FILE:-$DEPLOY_DIR/secrets/backup_passphrase.txt}"

if [ ! -d "$SECRETS_DIR" ]; then
    echo "Secrets directory is missing." >&2
    exit 1
fi
if [ ! -f "$PASSPHRASE_FILE" ]; then
    echo "Backup passphrase file is missing. Create it with mode 600. It is not printed." >&2
    exit 1
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
archive="$BACKUP_DIR/secrets-$stamp.tar.gz.enc"
tar -C "$DEPLOY_DIR" -czf - secrets | openssl enc -aes-256-cbc -pbkdf2 -salt -pass "file:$PASSPHRASE_FILE" -out "$archive"
chmod 600 "$archive"
echo "Encrypted backup written. Existing backups were kept." >&2
