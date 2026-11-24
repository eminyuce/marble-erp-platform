#!/bin/sh
# ==============================================================================
# Özerler Mermer ERP — production secret preflight
#
# Checks every host secret file the production compose file uses, before a
# container is recreated. POSIX sh / BusyBox (alpine) compatible.
#
#   * Files that are safe to create (no existing data depends on them) are
#     generated once when missing: totp_encryption_key, remember_me_key.
#     Creation is atomic (temp file + hard link) and never replaces a file.
#   * Every other missing file (db_password, rustfs keys, ...) aborts.
#   * A file that exists but is empty or whitespace-only aborts. It is never
#     overwritten.
#
# Secret values are never printed. Only file names and their state are logged.
#
# Usage:
#   ensure_secrets.sh --dir DIR [--compose FILE] [--host-dir HOSTDIR]
#                     [--name FILE.txt ...] [--list]
#
#   --dir       Directory holding the secret files as seen by this script
#               (inside the helper container: /s).
#   --compose   docker-compose.prod.yml. File paths under the top-level
#               "secrets:" block and "x-marble-host-secrets:" are checked.
#   --host-dir  Host path that --dir stands for. Compose paths must sit
#               directly under it. Defaults to --dir.
#   --name      Extra file name to check (repeatable).
#   --list      Print the resolved file names and exit (no changes).
#
# Exit codes: 0 all present, 3 aborted (missing/empty/invalid), 2 usage error.
# ==============================================================================

set -u

DIR=""
COMPOSE=""
HOST_DIR=""
NAMES=""
LIST_ONLY=0
APP_UID="${APP_UID:-1001}"
APP_GID="${APP_GID:-1001}"

usage() {
    echo "Kullanım: ensure_secrets.sh --dir DIZIN [--compose DOSYA] [--host-dir DIZIN] [--name DOSYA ...] [--list]" >&2
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --dir) DIR="${2:-}"; shift 2 ;;
        --compose) COMPOSE="${2:-}"; shift 2 ;;
        --host-dir) HOST_DIR="${2:-}"; shift 2 ;;
        --name) NAMES="$NAMES ${2:-}"; shift 2 ;;
        --list) LIST_ONLY=1; shift ;;
        -h|--help) usage; exit 0 ;;
        *) echo "Bilinmeyen seçenek: $1" >&2; usage; exit 2 ;;
    esac
done

if [ -z "$DIR" ]; then
    usage
    exit 2
fi
HOST_DIR="${HOST_DIR:-$DIR}"
HOST_DIR="${HOST_DIR%/}"

# Prints the secret file paths from the compose file, one per line, with
# ${SECRETS_DIR:-default} and ${SECRETS_DIR} resolved.
compose_secret_paths() {
    awk '
        /^[^[:space:]#][^:]*:/ {
            key = $0
            sub(/:.*/, "", key)
            block = key
            next
        }
        block == "secrets" && /^[[:space:]]+file:[[:space:]]*/ {
            line = $0
            sub(/^[[:space:]]+file:[[:space:]]*/, "", line)
            print line
            next
        }
        block == "x-marble-host-secrets" && /^[[:space:]]+[A-Za-z0-9_.-]+:[[:space:]]*[^[:space:]]/ {
            line = $0
            sub(/^[[:space:]]+[A-Za-z0-9_.-]+:[[:space:]]*/, "", line)
            print line
        }
    ' "$1" | sed -e 's/[[:space:]]*#.*$//' -e "s/^[\"']//" -e "s/[\"']\$//" | while IFS= read -r raw; do
        [ -n "$raw" ] || continue
        resolved="$raw"
        default_dir=$(printf '%s' "$raw" | sed -n 's/.*\${SECRETS_DIR:-\([^}]*\)}.*/\1/p')
        if [ -n "$default_dir" ]; then
            resolved=$(printf '%s' "$raw" | sed "s#\${SECRETS_DIR:-[^}]*}#${SECRETS_DIR:-$default_dir}#")
        fi
        if [ -n "${SECRETS_DIR:-}" ]; then
            resolved=$(printf '%s' "$resolved" | sed "s#\${SECRETS_DIR}#${SECRETS_DIR}#")
        fi
        printf '%s\n' "$resolved"
    done
}

# Generatable files and the number of random bytes they hold.
random_bytes_for() {
    case "$1" in
        totp_encryption_key.txt) echo 32 ;;
        remember_me_key.txt) echo 48 ;;
        *) echo 0 ;;
    esac
}

FILES=""
add_file() {
    case " $FILES " in
        *" $1 "*) ;;
        *) FILES="$FILES $1" ;;
    esac
}

if [ -n "$COMPOSE" ]; then
    if [ ! -r "$COMPOSE" ]; then
        echo "[HATA] Compose dosyası okunamadı: $COMPOSE" >&2
        exit 3
    fi
    for path in $(compose_secret_paths "$COMPOSE"); do
        parent="${path%/*}"
        name="${path##*/}"
        if [ "${parent%/}" != "$HOST_DIR" ]; then
            echo "[HATA] Gizli dosya beklenen klasörde değil: $path (beklenen: $HOST_DIR). Yayın durduruldu." >&2
            exit 3
        fi
        add_file "$name"
    done
fi
for name in $NAMES; do
    add_file "${name##*/}"
done

if [ -z "$FILES" ]; then
    echo "[HATA] Kontrol edilecek gizli dosya bulunamadı." >&2
    exit 3
fi

if [ "$LIST_ONLY" -eq 1 ]; then
    for name in $FILES; do
        echo "$name"
    done
    exit 0
fi

has_content() {
    grep -q '[^[:space:]]' "$1" 2>/dev/null
}

valid_aes_key() {
    bytes=$(tr -d ' \t\r\n' < "$1" | base64 -d 2>/dev/null | wc -c | tr -d ' ')
    [ "$bytes" = "32" ]
}

ensure_dir() {
    if [ ! -d "$DIR" ]; then
        mkdir -p "$DIR" || return 1
        chmod 700 "$DIR" 2>/dev/null || true
        echo "[OLUŞTURULDU] Gizli dosya klasörü (izin 700)."
    fi
    return 0
}

# Creates one generatable file. Never replaces an existing file.
create_file() {
    name="$1"
    bytes="$2"
    target="$DIR/$name"
    ensure_dir || return 1
    tmp=$(mktemp "$DIR/.${name}.XXXXXX") || return 1
    chmod 600 "$tmp" 2>/dev/null || true
    if ! head -c "$bytes" /dev/urandom | base64 | tr -d '\r\n' > "$tmp"; then
        rm -f "$tmp"
        return 1
    fi
    if ! has_content "$tmp"; then
        rm -f "$tmp"
        return 1
    fi
    if ! chown "$APP_UID:$APP_GID" "$tmp" 2>/dev/null; then
        echo "[UYARI] $name sahibi $APP_UID:$APP_GID yapılamadı (yetki yok)."
    fi
    chmod 400 "$tmp" 2>/dev/null || true
    # A hard link fails when the target already exists, so a file that
    # appeared meanwhile is never replaced.
    if ln "$tmp" "$target" 2>/dev/null; then
        rm -f "$tmp"
        echo "[OLUŞTURULDU] $name (sahip $APP_UID:$APP_GID, izin 400)"
        return 0
    fi
    rm -f "$tmp"
    if [ -e "$target" ]; then
        echo "[VAR] $name (eşzamanlı oluşturulmuş, dokunulmadı)"
        return 0
    fi
    return 1
}

# Pass 1: classify every file without changing anything.
problems=0
missing_required=""
to_create=""
for name in $FILES; do
    target="$DIR/$name"
    bytes=$(random_bytes_for "$name")
    if [ -e "$target" ] || [ -L "$target" ]; then
        if [ ! -f "$target" ]; then
            echo "[HATA] $name dosya değil (klasör veya bozuk bağlantı). Dokunulmadı." >&2
            problems=1
            continue
        fi
        if ! has_content "$target"; then
            echo "[HATA] $name var ama boş. Üzerine yazılmadı; dosyayı doldurun veya silin." >&2
            problems=1
            continue
        fi
        if [ "$name" = "totp_encryption_key.txt" ] && ! valid_aes_key "$target"; then
            echo "[HATA] $name geçerli bir AES-256 anahtarı değil (base64, 32 bayt olmalı). Üzerine yazılmadı." >&2
            problems=1
            continue
        fi
        echo "[VAR] $name"
        continue
    fi
    if [ "$bytes" -gt 0 ]; then
        to_create="$to_create $name"
    else
        missing_required="$missing_required $name"
    fi
done

if [ -n "$missing_required" ]; then
    for name in $missing_required; do
        echo "[HATA] Zorunlu gizli dosya eksik: $HOST_DIR/$name. Bu dosya mevcut veriyle eşleşmeli; otomatik üretilmez." >&2
    done
    problems=1
fi

if [ "$problems" -ne 0 ]; then
    echo "[HATA] Gizli dosya ön kontrolü başarısız. Yayın durduruldu; hiçbir dosya oluşturulmadı ve çalışan konteynere dokunulmadı." >&2
    exit 3
fi

# Pass 2: create the safe files that are missing.
for name in $to_create; do
    if ! create_file "$name" "$(random_bytes_for "$name")"; then
        echo "[HATA] $name oluşturulamadı. Yayın durduruldu." >&2
        exit 3
    fi
done

echo "[OK] Gizli dosya ön kontrolü tamam."
exit 0
