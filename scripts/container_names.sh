#!/usr/bin/env bash
# Resolve a container that may still have its pre-1.0.4 name.
# Prints the preferred name when that container exists, otherwise the legacy
# name when that one exists, otherwise the preferred name.

resolve_docker_container() {
    preferred="$1"
    legacy="$2"
    if docker inspect "$preferred" >/dev/null 2>&1; then
        printf '%s\n' "$preferred"
        return 0
    fi
    if docker inspect "$legacy" >/dev/null 2>&1; then
        printf '%s\n' "$legacy"
        return 0
    fi
    printf '%s\n' "$preferred"
}

# Attach a running database container to the compose network under its DNS alias.
# "network connect" on an existing attachment is ignored. Volumes are not touched.
ensure_network_alias() {
    network="$1"
    alias_name="$2"
    container="$3"
    if ! docker inspect "$container" >/dev/null 2>&1; then
        return 0
    fi
    docker network create "$network" >/dev/null 2>&1 || true
    docker network connect --alias "$alias_name" "$network" "$container" >/dev/null 2>&1 || true
}
