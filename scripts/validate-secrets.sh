#!/usr/bin/env bash
# Check secret files exist, are non-empty, and are not world-readable.
# Never prints secret values.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/marble-erp}"
SECRETS_DIR="${SECRETS_DIR:-$DEPLOY_DIR/secrets}"
names="db_password remember_me_key rustfs_access_key rustfs_secret_key totp_encryption_key"

if [ ! -d "$SECRETS_DIR" ]; then
    echo "Secrets directory is missing." >&2
    exit 1
fi

dir_mode="$(stat -c %a "$SECRETS_DIR")"
if [ "$dir_mode" != "700" ]; then
    echo "Secrets directory must be mode 700." >&2
    exit 1
fi

for name in $names; do
    file="$SECRETS_DIR/$name.txt"
    if [ ! -f "$file" ] || [ ! -r "$file" ]; then
        echo "Missing readable secret file: $name" >&2
        exit 1
    fi
    mode="$(stat -c %a "$file")"
    owner="$(stat -c %u "$file")"
    group="$(stat -c %g "$file")"
    if [ "$mode" != "400" ]; then
        echo "Secret file $name must be mode 400 so only the app user can read it." >&2
        exit 1
    fi
    if [ "$owner" != "${APP_UID:-1001}" ] || [ "$group" != "${APP_GID:-1001}" ]; then
        echo "Secret file $name must be owned by ${APP_UID:-1001}:${APP_GID:-1001}." >&2
        exit 1
    fi
    if [ ! -s "$file" ]; then
        echo "Secret file $name is empty." >&2
        exit 1
    fi
done

echo "Secret files passed validation. Values were not printed." >&2
