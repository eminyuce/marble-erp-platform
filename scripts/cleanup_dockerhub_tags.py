#!/usr/bin/env python3
"""Delete Docker Hub tags that have not been updated for 14 days.

Floating aliases (latest, prod, stable) and semantic version tags are kept.
Every kept or deleted tag is printed so the CI log names the image tag.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone

HUB = "https://hub.docker.com"
PROTECTED_NAMES = frozenset({"latest", "prod", "stable"})
RELEASE_TAG = re.compile(r"^v?\d+\.\d+(?:\.\d+)?$")
RETENTION_DAYS = 14


def classify(name: str, last_updated: datetime, now: datetime) -> str:
    """Return why a tag is kept, or 'delete' when it is old enough to remove."""
    if name in PROTECTED_NAMES:
        return "alias"
    if RELEASE_TAG.fullmatch(name):
        return "release"
    cutoff = now - timedelta(days=RETENTION_DAYS)
    if last_updated >= cutoff:
        return "fresh"
    return "delete"


def parse_hub_time(value: str) -> datetime:
    text = value.replace("Z", "+00:00")
    parsed = datetime.fromisoformat(text)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def _redact(text: str) -> str:
    token = os.environ.get("DOCKERHUB_TOKEN", "")
    if token:
        text = text.replace(token, "***")
    return text


def _request(method: str, url: str, token: str | None = None, body: dict | None = None) -> tuple[int, dict | str]:
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            raw = response.read().decode("utf-8")
            if not raw:
                return response.status, {}
            return response.status, json.loads(raw)
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        try:
            payload: dict | str = json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            payload = raw
        return error.code, payload


def login(username: str, password: str) -> str:
    status, payload = _request(
        "POST",
        f"{HUB}/v2/users/login/",
        body={"username": username, "password": password},
    )
    if status != 200 or not isinstance(payload, dict) or "token" not in payload:
        detail = payload if isinstance(payload, str) else payload.get("detail", payload)
        raise SystemExit(f"Docker Hub girişi başarısız ({status}): {_redact(str(detail))}")
    return str(payload["token"])


def list_tags(namespace: str, repository: str, token: str) -> list[dict]:
    quoted_namespace = urllib.parse.quote(namespace, safe="")
    quoted_repository = urllib.parse.quote(repository, safe="")
    url = (
        f"{HUB}/v2/repositories/{quoted_namespace}/{quoted_repository}/tags"
        "?page_size=100"
    )
    tags: list[dict] = []
    while url:
        if urllib.parse.urlparse(url).netloc != "hub.docker.com":
            raise SystemExit(f"Beklenmeyen Docker Hub adresi: {url}")
        status, payload = _request("GET", url, token=token)
        if status != 200 or not isinstance(payload, dict):
            raise SystemExit(f"Etiket listesi alınamadı ({status}): {_redact(str(payload))}")
        tags.extend(payload.get("results") or [])
        url = payload.get("next") or ""
    return tags


def delete_tag(namespace: str, repository: str, name: str, token: str) -> None:
    quoted_namespace = urllib.parse.quote(namespace, safe="")
    quoted_repository = urllib.parse.quote(repository, safe="")
    quoted_name = urllib.parse.quote(name, safe="")
    url = (
        f"{HUB}/v2/repositories/{quoted_namespace}/{quoted_repository}/tags/{quoted_name}/"
    )
    status, payload = _request("DELETE", url, token=token)
    if status not in (200, 202, 204):
        raise RuntimeError(f"{status}: {_redact(str(payload))}")


def _truthy(value: str | None) -> bool:
    return (value or "").strip().lower() in {"1", "true", "yes", "on"}


def run_cleanup(namespace: str, repository: str, token: str, dry_run: bool, now: datetime) -> int:
    print(f"Depo: {namespace}/{repository}")
    print(f"Kesit: {RETENTION_DAYS} günden eski etiketler")
    print("Korunan adlar: latest, prod, stable")
    print("Korunan sürüm etiketleri: 1.2.3, v1.2.3, 1.2")
    print("Mod: deneme (silinmez)" if dry_run else "Mod: silme")

    tags = list_tags(namespace, repository, token)
    deleted = 0
    failed = 0
    for tag in tags:
        name = str(tag.get("name") or "")
        raw_updated = tag.get("last_updated")
        if not name or not raw_updated:
            print(f"Atlanıyor: {name or '(adsız)'} - tarih yok")
            continue
        last_updated = parse_hub_time(str(raw_updated))
        reason = classify(name, last_updated, now)
        stamp = last_updated.isoformat()
        if reason == "alias":
            print(f"Korunuyor: {name} (korumalı etiket) - son güncelleme: {stamp}")
            continue
        if reason == "release":
            print(f"Korunuyor: {name} (sürüm etiketi) - son güncelleme: {stamp}")
            continue
        if reason == "fresh":
            print(f"Kalıyor: {name} - son güncelleme: {stamp}")
            continue
        if dry_run:
            print(f"Silinecek (deneme): {name} - son güncelleme: {stamp}")
            continue
        print(f"Siliniyor: {name} - son güncelleme: {stamp}")
        try:
            delete_tag(namespace, repository, name, token)
        except RuntimeError as error:
            failed += 1
            print(f"Silinemedi: {name} - {error}")
            continue
        deleted += 1
        print(f"Silindi: {name}")

    print(f"Özet: {len(tags)} etiket tarandı, {deleted} silindi, {failed} silinemedi")
    return 1 if failed else 0


def self_test() -> None:
    now = datetime(2026, 11, 10, tzinfo=timezone.utc)
    old = now - timedelta(days=20)
    fresh = now - timedelta(days=3)
    assert classify("latest", old, now) == "alias"
    assert classify("prod", old, now) == "alias"
    assert classify("stable", old, now) == "alias"
    assert classify("1.2.3", old, now) == "release"
    assert classify("v1.2.3", old, now) == "release"
    assert classify("1.2", old, now) == "release"
    assert classify("a1b2c3d", fresh, now) == "fresh"
    assert classify("a1b2c3d", old, now) == "delete"
    assert parse_hub_time("2026-10-01T00:00:00.123456Z").year == 2026
    print("self-test geçti")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Delete stale Docker Hub tags and log each one.")
    parser.add_argument("--self-test", action="store_true", help="Run decision checks and exit")
    parser.add_argument("--dry-run", action="store_true", help="Log deletions without calling DELETE")
    args = parser.parse_args(argv)
    if args.self_test:
        self_test()
        return 0

    namespace = os.environ.get("DOCKERHUB_USERNAME", "").strip()
    repository = os.environ.get("DOCKERHUB_REPOSITORY", "marble-erp-platform").strip()
    password = os.environ.get("DOCKERHUB_TOKEN", "").strip()
    if not namespace or not password:
        print("DOCKERHUB_USERNAME ve DOCKERHUB_TOKEN gerekli", file=sys.stderr)
        return 2

    dry_run = args.dry_run or _truthy(os.environ.get("DRY_RUN"))
    jwt = login(namespace, password)
    return run_cleanup(namespace, repository, jwt, dry_run, datetime.now(timezone.utc))


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
