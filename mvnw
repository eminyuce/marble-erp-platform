#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper script for UNIX
# ----------------------------------------------------------------------------
set -e

if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
else
    echo "mvn command not found on PATH. Please ensure Maven is installed." >&2
    exit 1
fi
