#!/usr/bin/env bash
# ==============================================================================
# Özerler Mermer ERP — In-app production deploy
#
# Invoked from the ERP "Sistem Araçları > Deployment" screen (or by sudo from
# the marble-erp service user). Must survive `systemctl stop marble-erp`, so
# the real work is re-parented into a transient systemd unit.
#
# Installed (root-owned) copy:
#   /usr/local/sbin/marble-erp-inapp-deploy
#
# Usage:
#   marble-erp-inapp-deploy --ping
#   marble-erp-inapp-deploy --repo /home/eyuce/marble-erp-platform
# ==============================================================================

set -euo pipefail

REPO_ROOT="${MARBLE_REPO_ROOT:-/home/eyuce/marble-erp-platform}"
LOG_FILE="${MARBLE_DEPLOY_LOG:-/opt/marble-erp/logs/inapp-deploy.log}"
STATUS_FILE="${MARBLE_DEPLOY_STATUS:-/opt/marble-erp/logs/inapp-deploy.status}"
LOCK_FILE="${MARBLE_DEPLOY_LOCK:-/opt/marble-erp/logs/inapp-deploy.lock}"
GIT_REMOTE="${MARBLE_GIT_REMOTE:-origin}"
GIT_BRANCH="${MARBLE_GIT_BRANCH:-main}"
UNIT_NAME="marble-erp-inapp-deploy"
FOREGROUND=0

usage() {
    cat <<'EOF'
Usage: marble-erp-inapp-deploy [options]

  --ping             Print READY and exit (sudo / path probe).
  --foreground       Fetch origin/main, hard-reset, then deploy (used by systemd-run).
  --repo DIR         Git clone root (default: /home/eyuce/marble-erp-platform)
  --log FILE         Deploy log path
  --status FILE      Status file path
  --lock FILE        flock path
  --remote NAME      Git remote (default: origin)
  --branch NAME      Git branch (default: main)
  -h, --help         Show this help
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --ping)
            echo "READY"
            exit 0
            ;;
        --foreground)
            FOREGROUND=1
            shift
            ;;
        --repo)
            REPO_ROOT="$2"
            shift 2
            ;;
        --log)
            LOG_FILE="$2"
            shift 2
            ;;
        --status)
            STATUS_FILE="$2"
            shift 2
            ;;
        --lock)
            LOCK_FILE="$2"
            shift 2
            ;;
        --remote)
            GIT_REMOTE="$2"
            shift 2
            ;;
        --branch)
            GIT_BRANCH="$2"
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

write_status() {
    local state="$1"
    local started_at="${2:-}"
    local finished_at="${3:-}"
    local exit_code="${4:-}"
    mkdir -p "$(dirname "$STATUS_FILE")"
    cat > "$STATUS_FILE" <<EOF
STATE=${state}
STARTED_AT=${started_at}
FINISHED_AT=${finished_at}
EXIT_CODE=${exit_code}
EOF
    chmod 644 "$STATUS_FILE" 2>/dev/null || true
}

iso_now() {
    date -u +"%Y-%m-%dT%H:%M:%SZ"
}

prepare_log() {
    mkdir -p "$(dirname "$LOG_FILE")" "$(dirname "$LOCK_FILE")"
    : > "$LOG_FILE"
    chmod 644 "$LOG_FILE" 2>/dev/null || true
}

# Git 2.35.2+ refuses a work tree owned by another user (CVE-2022-24765).
# Interactive SSH uses `sudo git pull` (root, clone owned by eyuce). systemd-run
# starts a clean root service whose gitconfig does not yet trust that path.
ensure_git_safe_directory() {
    local dir="$1"
    local existing=""
    if [ "$(id -u)" -ne 0 ]; then
        return 0
    fi
    existing="$(git config --system --get-all safe.directory 2>/dev/null || true)"
    if printf '%s\n' "$existing" | grep -Fxq "$dir"; then
        return 0
    fi
    git config --system --add safe.directory "$dir"
}

# Same effective command as: sudo git -c safe.directory=* ...
git_cmd() {
    git -c safe.directory="$REPO_ROOT" -c safe.directory='*' "$@"
}

detach_into_independent_unit() {
    if ! command -v systemd-run >/dev/null 2>&1; then
        echo "systemd-run is required so deploy survives marble-erp restart." >&2
        exit 1
    fi
    systemctl reset-failed "${UNIT_NAME}.service" 2>/dev/null || true
    if systemctl is-active --quiet "${UNIT_NAME}.service"; then
        echo "A deploy is already running (${UNIT_NAME}.service)." >&2
        exit 2
    fi
    local unit_home="${HOME:-}"
    if [ -z "$unit_home" ]; then
        unit_home="$(getent passwd "$(id -un)" 2>/dev/null | cut -d: -f6 || true)"
    fi
    unit_home="${unit_home:-/root}"
    systemd-run \
        --unit="${UNIT_NAME}" \
        --collect \
        --quiet \
        --property=KillMode=mixed \
        --working-directory="${REPO_ROOT}" \
        --setenv=HOME="$unit_home" \
        --setenv=GIT_CONFIG_COUNT=1 \
        --setenv=GIT_CONFIG_KEY_0=safe.directory \
        --setenv=GIT_CONFIG_VALUE_0="$REPO_ROOT" \
        /bin/bash "$0" \
            --foreground \
            --repo "$REPO_ROOT" \
            --log "$LOG_FILE" \
            --status "$STATUS_FILE" \
            --lock "$LOCK_FILE" \
            --remote "$GIT_REMOTE" \
            --branch "$GIT_BRANCH"
}

if [ "$FOREGROUND" -ne 1 ]; then
    prepare_log
    write_status "RUNNING" "$(iso_now)" "" ""
    ensure_git_safe_directory "$REPO_ROOT"
    detach_into_independent_unit
    exit 0
fi

STARTED_AT="$(iso_now)"
write_status "RUNNING" "$STARTED_AT" "" ""

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
    echo "Deploy lock is held: $LOCK_FILE" >&2
    write_status "FAILED" "$STARTED_AT" "$(iso_now)" "2"
    exit 2
fi

# Also append to the log file in case systemd StandardOutput is not wired.
exec > >(tee -a "$LOG_FILE") 2>&1

cleanup() {
    local code=$?
    local finished
    finished="$(iso_now)"
    if [ "$code" -eq 0 ]; then
        write_status "SUCCESS" "$STARTED_AT" "$finished" "0"
    else
        write_status "FAILED" "$STARTED_AT" "$finished" "$code"
        echo "[FAIL] In-app deploy exited with code $code"
    fi
}
trap cleanup EXIT

export GIT_TERMINAL_PROMPT=0

echo "======================================================================"
echo "   Özerler Mermer ERP — In-app deploy (no SSH)"
echo "======================================================================"
echo "Started:    $STARTED_AT"
echo "Repo:       $REPO_ROOT"
echo "Branch:     ${GIT_REMOTE}/${GIT_BRANCH}"
echo "Log:        $LOG_FILE"
echo

if [ ! -d "$REPO_ROOT/.git" ]; then
    echo "ERROR: Not a git repository: $REPO_ROOT" >&2
    exit 1
fi

if [ ! -x "$REPO_ROOT/scripts/deploy_production.sh" ]; then
    echo "ERROR: Missing deploy script: $REPO_ROOT/scripts/deploy_production.sh" >&2
    exit 1
fi

cd "$REPO_ROOT"
ensure_git_safe_directory "$REPO_ROOT"

# Always match the remote branch. Local commits and edits on the server are discarded.
echo "\$ git fetch ${GIT_REMOTE} ${GIT_BRANCH}"
git_cmd fetch "$GIT_REMOTE" "$GIT_BRANCH"

echo "\$ git checkout -B ${GIT_BRANCH} ${GIT_REMOTE}/${GIT_BRANCH}"
git_cmd checkout -B "$GIT_BRANCH" "${GIT_REMOTE}/${GIT_BRANCH}"

echo "\$ git reset --hard ${GIT_REMOTE}/${GIT_BRANCH}"
git_cmd reset --hard "${GIT_REMOTE}/${GIT_BRANCH}"

echo
echo "\$ ./scripts/deploy_production.sh -y"
"$REPO_ROOT/scripts/deploy_production.sh" -y

echo
echo "[OK] In-app deploy finished."
