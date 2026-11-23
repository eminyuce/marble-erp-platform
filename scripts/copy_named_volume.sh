#!/usr/bin/env bash
# Copy one Docker named volume into another.
# The source is never deleted. A destination that already has files is left as-is.
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 SOURCE_VOLUME DEST_VOLUME" >&2
    exit 1
fi

src="$1"
dst="$2"

docker volume inspect "$src" >/dev/null
docker volume create "$dst" >/dev/null

count="$(docker run --rm -v "${dst}:/dst" alpine sh -c 'ls -A /dst | wc -l' | tr -d '[:space:]')"
if [ "$count" != "0" ]; then
    echo "Destination ${dst} already has files. Left unchanged. Source ${src} was not deleted."
    exit 0
fi

docker run --rm -v "${src}:/from" -v "${dst}:/to" alpine cp -a /from/. /to/
echo "Copied ${src} -> ${dst}. Source ${src} was kept."
