#!/usr/bin/env bash
# Create file-backed secrets outside the git checkout.
# Never prints secret values. Does not overwrite existing files unless --regenerate.
set -euo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/marble-erp}"
SECRETS_DIR="${SECRETS_DIR:-$DEPLOY_DIR/secrets}"
REGENERATE=0
NONINTERACTIVE=0

usage() {
    echo "Usage: DEPLOY_DIR=/opt/marble-erp $0 [--non-interactive] [--regenerate]" >&2
}

while [ $# -gt 0 ]; do
    case "$1" in
        --regenerate) REGENERATE=1 ;;
        --non-interactive) NONINTERACTIVE=1 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Unknown argument." >&2; usage; exit 1 ;;
    esac
    shift
done

if [ "$REGENERATE" -eq 1 ] && [ "$NONINTERACTIVE" -eq 0 ]; then
    echo "Regenerate replaces existing secret files. Database password must still match the live Postgres role." >&2
    printf "Type REGENERATE to continue: " >&2
    read -r confirm
    if [ "$confirm" != "REGENERATE" ]; then
        echo "Aborted." >&2
        exit 1
    fi
fi

mkdir -p "$SECRETS_DIR"
chmod 700 "$SECRETS_DIR"
# The app image runs as UID 1001. Root (the Postgres entrypoint) can still read these files.
APP_UID="${APP_UID:-1001}"
APP_GID="${APP_GID:-1001}"

own_secret() {
    target="$1"
    chown "$APP_UID:$APP_GID" "$target"
    chmod 400 "$target"
}

write_secret() {
    name="$1"
    min_length="$2"
    target="$SECRETS_DIR/$name.txt"
    if [ -f "$target" ] && [ "$REGENERATE" -eq 0 ]; then
        own_secret "$target"
        return 0
    fi
    value="$(openssl rand -base64 48 | tr -d '\n' | cut -c1-48)"
    if [ "${#value}" -lt "$min_length" ]; then
        echo "Generated value for $name is too short." >&2
        exit 1
    fi
    umask 077
    printf '%s\n' "$value" > "$target"
    own_secret "$target"
}

# AES-256 key for TOTP secrets: exactly 32 random bytes, base64 encoded (44 characters).
# Never regenerate it on a live system: enrolled authenticators would stop working.
write_aes_key() {
    name="$1"
    target="$SECRETS_DIR/$name.txt"
    if [ -f "$target" ]; then
        own_secret "$target"
        return 0
    fi
    umask 077
    openssl rand -base64 32 | tr -d '\n' > "$target"
    printf '\n' >> "$target"
    own_secret "$target"
}

write_secret db_password 24
write_secret remember_me_key 32
write_secret rustfs_access_key 16
write_secret rustfs_secret_key 24
write_aes_key totp_encryption_key

echo "Secret files are in $SECRETS_DIR (owner ${APP_UID}:${APP_GID}, mode 400). Values were not printed." >&2
if [ "$REGENERATE" -eq 1 ]; then
    echo "Regenerated files do not change an existing Postgres password until you ALTER ROLE with the same value." >&2
fi
