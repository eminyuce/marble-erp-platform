# DOCKER PRODUCTION DEPLOYMENT REPORT
**Project:** Özerler Mermer ERP Platform (`marble-erp-platform`)  
**Docker Hub Repository:** `guvenulu/marble-erp-platform`  
**Scope:** Production-Safe Docker Infrastructure, CI/CD & Zero Data Loss Deployment

---

## 1. Existing Production Architecture Discovered

Production is actively running under `/opt/marble-erp` managed by Docker Compose (`project: marble-erp`) with three long-running containers connected on the `marble-erp_default` bridge network, fronted on host port `8080` by `marble-erp-proxy.service` (forwarding to container port `81`):

| Service Role | Container Name | Image | Host -> Container Port | Status |
| :--- | :--- | :--- | :--- | :--- |
| **ERP Application** | `marble-erp` | `guvenulu/marble-erp-platform:latest` | `0.0.0.0:81->81/tcp` | Running (`healthy`) |
| **PostgreSQL Database** | `marble-erp-postgres` | `postgres:16-alpine` (PostgreSQL 16.15) | `127.0.0.1:5432->5432/tcp` | Running (`healthy`) |
| **Object Storage (S3)** | `marble-rustfs` | `rustfs/rustfs:1.0.1` | `127.0.0.1:9000-9001->9000-9001/tcp` | Running (`healthy`) |

One-shot initialization containers in `docker-compose.prod.yml`:
- `rustfs-perms` (`alpine:3.20`): Ensures `/opt/rustfs/data` is owned by `10001:10001` when initializing the full stack.
- `deploy-log-perms` (`alpine:3.20`): Ensures `/opt/marble-erp/logs`, `/app/uploads`, and `/app/media` are owned by `1001:1001`.

---

## 2. Existing PostgreSQL Container and Volume Information

- **Container Name:** `marble-erp-postgres`
- **Image & Version:** `postgres:16-alpine` (`PostgreSQL 16.15 on x86_64-pc-linux-musl`)
- **Docker Volume Name:** `docker_postgres_data` (`external: true`)
- **Host Mountpoint:** `/var/lib/docker/volumes/docker_postgres_data/_data` -> `/var/lib/postgresql/data`
- **Database Name:** `marble_erp`
- **Database User:** `marbleuser` (credentials read from `/opt/marble-erp/.env`, never committed to Git)
- **Installed Extensions:** `plpgsql` (`1.0`)
- **Schema State:** `58` tables in `public` schema
- **Flyway History (`flyway_schema_history`):** `31` applied migrations (`V1__create_security_tables.sql` through `V31__site_stock_waste_quantity_and_workshop_production.sql`), all with `success = true`.
- **Hibernate & Flyway Production Configuration:**
  - `spring.jpa.hibernate.ddl-auto=validate` (never drops or recreates schema)
  - `spring.flyway.enabled=true`
  - `spring.flyway.baseline-on-migrate=true`
  - `spring.flyway.out-of-order=true`

---

## 3. Existing RustFS Container and Volume Information

- **Container Name:** `marble-rustfs`
- **Image & Version:** `rustfs/rustfs:1.0.1`
- **Storage Mount:** Host bind mount `/opt/rustfs/data` -> `/data` (owned by UID/GID `10001:10001`)
- **Active Bucket:** `erp-files` (`/opt/rustfs/data/erp-files`, plus `.rustfs.sys`)
- **Endpoints:**
  - Internal Docker DNS endpoint (used by `marble-erp`): `http://rustfs:9000`
  - Loopback API / Console ports on host: `127.0.0.1:9000` (S3 API), `127.0.0.1:9001` (Console)
- **Legacy Migration Volumes (retained read/write on `marble-erp`):**
  - `marble-erp_uploads_data` -> `/app/uploads`
  - `marble-erp_media_data` -> `/app/media`

---

## 4. Existing Docker Network Information

- **Primary Production Network:** `marble-erp_default` (`bridge` driver)
  - Connected containers: `marble-erp`, `marble-erp-postgres` (DNS alias `postgres`), `marble-rustfs` (DNS alias `rustfs`).
- **Additional Bridge Networks Present on Host:** `docker_marble-network`, `bridge`, `host`, `none`.

---

## 5. Changes Made

1. **Zero-Data-Loss Application-Only Production Rollout (`--no-deps app`):**
   - Updated `scripts/deploy_from_app.sh` so that when `marble-erp-postgres` and `marble-rustfs` are already running, deployments execute:
     ```bash
     docker compose --project-directory /opt/marble-erp -f /opt/marble-erp/docker-compose.prod.yml pull app
     docker compose --project-directory /opt/marble-erp -f /opt/marble-erp/docker-compose.prod.yml up -d --no-deps app
     ```
   - `marble-erp-postgres` and `marble-rustfs` are **never** stopped, recreated, or touched during normal application updates.
2. **Self-Contained Docker Image (`Dockerfile` + `scripts/docker-entrypoint.sh`):**
   - Installed the `docker-compose` v2 CLI plugin (`/usr/local/lib/docker/cli-plugins/docker-compose`) inside the runtime image.
   - Bundled `/stack/docker-compose.prod.yml`, `/stack/docker-compose.yml`, `/stack/.env.example`, and `/stack/scripts/*.sh` inside `guvenulu/marble-erp-platform`.
   - Added `/usr/local/bin/docker-entrypoint.sh` supporting `--print-compose`, `--print-prod-compose`, `--version`, automatic `host.docker.internal` fallback when run via `docker run` outside Compose, and graceful JVM shutdown (`-Dserver.shutdown=graceful`).
   - Fixed `.dockerignore` (`!.env.example`) so `.env.example` is never excluded from the Docker build context.
3. **Standalone / New-Machine Full-Stack Compose (`docker/docker-compose.yml`):**
   - Added `image: ${DOCKERHUB_USERNAME:-guvenulu}/marble-erp-platform:${APP_TAG:-latest}` and an `app-perms` init service (`chown -R 1001:1001 /uploads /media`) so pulling from Docker Hub on any machine starts PostgreSQL + RustFS + Marble ERP cleanly.
4. **Immutable Image Tagging & Git SHA Metadata:**
   - Updated `.github/workflows/docker-publish.yml` and added `.gitlab-ci.yml` to publish immutable Git commit SHA tags, semantic version tags (`v*`), and `latest`, passing `GIT_COMMIT_SHA` and `APP_VERSION` build arguments into OCI image labels and runtime environment variables (`APP_GIT_SHA`, `APP_VERSION`).
   - Updated `DeploymentService.java` to report `APP_GIT_SHA` and `APP_VERSION` when `.git` is not mounted inside the container.
5. **Pre-Update Image Recording, Deep Health Verification & Automatic Rollback:**
   - Before pulling a new image, `scripts/deploy_from_app.sh` records the running container's image ID, digest, and Git SHA in `/opt/marble-erp/logs/last-known-good.env` and tags it as `guvenulu/marble-erp-platform:rollback-previous`.
   - After recreating `app`, it verifies both Docker health (`healthy`) and HTTP `http://127.0.0.1:81/health/` (`"status":"UP"`, which checks PostgreSQL and RustFS health).
   - If health verification fails, `scripts/deploy_from_app.sh` automatically rolls back `app` to `guvenulu/marble-erp-platform:rollback-previous` via `up -d --no-deps app`.
6. **Production Backup, Restore, Rollback & Fresh Install Scripts:**
   - Created `scripts/backup_production.sh`, `scripts/restore_production.sh`, `scripts/rollback_production.sh`, and `scripts/init_fresh_install.sh`.

---

## 6. Files Created or Modified

| File | Action | Purpose |
| :--- | :--- | :--- |
| `Dockerfile` | Modified | Added `docker-compose` v2 CLI plugin, `GIT_COMMIT_SHA`/`APP_VERSION` metadata, bundled `/stack/*` files, and `docker-entrypoint.sh`. |
| `docker-compose.prod.yml` | Modified | Added `init: true` and `stop_grace_period: 30s` to `app`; preserved external `docker_postgres_data` and `/opt/rustfs/data`. |
| `docker/docker-compose.yml` | Modified | Added `image: guvenulu/marble-erp-platform:latest`, `app-perms` volume init container, `init: true`, and `stop_grace_period: 30s`. |
| `.dockerignore` | Modified | Added `!.env.example` so `.env.example` is available during `docker build`. |
| `.gitignore` | Modified | Ignored backup artifacts (`backups/`, `*.dump`, `*.sql.gz`, `last-known-good.env`). |
| `.env.example` | Modified | Documented `APP_TAG` immutable tag usage and `BACKUP_ROOT`. |
| `.github/workflows/docker-publish.yml` | Modified | Added immutable SHA and semver tags + `GIT_COMMIT_SHA` and `APP_VERSION` build arguments. |
| `.gitlab-ci.yml` | Created | Full GitLab CI/CD pipeline (`test`, `quality`, `build-and-push`, `deploy`). |
| `scripts/docker-entrypoint.sh` | Created | Container entrypoint with `--print-compose`, `--print-prod-compose`, `--version`, and `host.docker.internal` fallback. |
| `scripts/deploy_from_app.sh` | Modified | App-only rollout (`pull app` + `up -d --no-deps app`), `rollback-previous` tagging, `/health/` check, and automatic rollback. |
| `scripts/backup_production.sh` | Created | Verified PostgreSQL (`pg_dump -Fc` + `.sql.gz` + `pg_restore --list`) and RustFS (`tar.gz` + `tar -tzf`) backup script with SHA-256 checksums. |
| `scripts/restore_production.sh` | Created | Safety-gated (`--confirm-restore`) restore script with automatic pre-restore backup. |
| `scripts/rollback_production.sh` | Created | One-command application-only rollback to `rollback-previous` or a specific tag. |
| `scripts/init_fresh_install.sh` | Created | Separate new-customer installation script that aborts if production resources exist. |
| `src/main/java/com/ozerler/marble/service/DeploymentService.java` | Modified | Reads `APP_GIT_SHA` and `APP_VERSION` fallback when `.git` directory is absent in container. |
| `src/test/java/com/ozerler/marble/service/DeploymentServiceTest.java` | Modified | Automated tests validating deployment safety rules, app-only rollout, and rollback behavior. |
| `README.md` & `docs/production-deploy.md` | Modified | Updated documentation for standalone Docker Hub usage and zero-data-loss production operations. |

---

## 7. GitHub Actions & GitLab CI/CD Workflow Summary

1. **Test & Quality Stage:**
   - Builds frontend CSS (`npm ci && npm run build`).
   - Runs `./mvnw test` and `./mvnw pmd:check` (0 PMD violations enforced).
2. **Build & Push Stage (`main` branch and `v*` release tags):**
   - Builds multi-stage `Dockerfile` using Docker BuildKit layer caching (`type=gha` on GitHub Actions, registry cache on GitLab CI).
   - Embeds `GIT_COMMIT_SHA` and `APP_VERSION` in OCI labels and container env vars.
   - Pushes `guvenulu/marble-erp-platform:<git-sha>`, `guvenulu/marble-erp-platform:<semver>` (on tags), and `guvenulu/marble-erp-platform:latest`.
3. **Production Deployment Stage:**
   - Connects to the production host (or triggers via `/settings/deployment` in-app rollout), takes a pre-deploy backup, runs `docker compose -f docker-compose.prod.yml pull app` and `docker compose -f docker-compose.prod.yml up -d --no-deps app`, verifies `/health/`, and rolls back automatically on failure.

---

## 8. Docker Image Tagging Strategy

- **Immutable Commit SHA Tag:** `guvenulu/marble-erp-platform:<git-sha>` — generated on every merge to `main`.
- **Semantic Release Tag:** `guvenulu/marble-erp-platform:1.0.0` (and `1.0`) — generated when a `v*` tag is pushed.
- **Latest Production Tag:** `guvenulu/marble-erp-platform:latest` — points to the latest verified `main` build.
- **Local Rollback Tag on Production Host:** `guvenulu/marble-erp-platform:rollback-previous` — automatically tagged on the production server immediately before pulling a new image.

---

## 9. Production Deployment Procedure

```bash
# 1. Verify existing production containers are healthy
docker ps --filter name=marble

# 2. Take a verified backup of PostgreSQL and RustFS
/home/eyuce/marble-erp-platform/scripts/backup_production.sh

# 3. Trigger application-only rollout (leaves marble-erp-postgres and marble-rustfs untouched)
cd /opt/marble-erp
docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d --no-deps app

# 4. Verify application, database, and RustFS health
curl -fsS http://127.0.0.1:81/health/
```

Alternatively, click **"Sistemi Güncelle"** in the Admin UI at `/settings/deployment`, which executes `scripts/deploy_from_app.sh` in a detached helper container and writes progress to `/opt/marble-erp/logs/deploy-status.env` and `/opt/marble-erp/logs/deploy-latest.log`.

---

## 10. Backup Strategy

Executed via `./scripts/backup_production.sh`:
- **Destination:** `/opt/marble-erp/backups/<YYYYMMDD_HHMMSS>/`
- **PostgreSQL Backup:**
  - Custom format dump (`postgres_marble_erp.dump`) via `pg_dump -Fc --no-owner --no-acl` inside `marble-erp-postgres`.
  - Plain compressed SQL dump (`postgres_marble_erp.sql.gz`).
  - Verified immediately using `pg_restore --list`.
- **RustFS Backup:**
  - Compressed archive (`rustfs_data.tar.gz`) of `/opt/rustfs/data` (bucket `erp-files`).
  - Verified immediately using `tar -tzf`.
- **Metadata & Integrity:**
  - `manifest.env` (timestamp, container IDs, image digests, Flyway migration count) and `SHA256SUMS` validated with `sha256sum -c`.

---

## 11. Rollback Strategy

- **Automatic Rollback:** If the newly started `marble-erp` container fails to become `healthy` or `/health/` does not return `"status":"UP"` within 90 seconds during `scripts/deploy_from_app.sh`, the script automatically restores `guvenulu/marble-erp-platform:rollback-previous` using `docker compose -f docker-compose.prod.yml up -d --no-deps app`.
- **Manual One-Command Rollback:**
  ```bash
  ./scripts/rollback_production.sh
  ```
  Or roll back to a specific immutable tag:
  ```bash
  ./scripts/rollback_production.sh guvenulu/marble-erp-platform:<git-sha>
  ```
- Neither automatic nor manual rollback stops or modifies `marble-erp-postgres` or `marble-rustfs`.

---

## 12. Test Results

- **Shell Script Syntax Validation (`bash -n`):** Passed with `0` errors across all scripts (`docker-entrypoint.sh`, `deploy_from_app.sh`, `backup_production.sh`, `restore_production.sh`, `rollback_production.sh`, `init_fresh_install.sh`).
- **Docker Compose Configuration Validation:** Both `docker-compose.prod.yml` and `docker/docker-compose.yml` validated via `docker compose config`.
- **Unit & Integration Test Suite (`./mvnw test`):** Passed with `0` failures, including new deployment safety and rollback tests in `DeploymentServiceTest`.
- **Static Code Analysis (`./mvnw pmd:check`):** Passed with `0` violations.

---

## 13. Remaining Risks

1. **Forward-Only Flyway Migrations:** Application container rollback reverts the JVM code to the previous image, but does not automatically undo newly applied SQL migrations. All new Flyway migrations (`V32+`) must remain backward-compatible (additive columns/tables) or be preceded by `./scripts/backup_production.sh`.
2. **Single-Host Disk Storage:** Backups stored under `/opt/marble-erp/backups` reside on the same physical host disk unless synced off-host. Configure periodic off-host synchronization (`rsync` / S3 backup bucket) for disaster recovery.

---

## 14. Manual Actions Requiring Administrator Approval

- **Database Restores:** Running `./scripts/restore_production.sh --confirm-restore <backup_dir>` overwrites live data with the selected backup snapshot and therefore requires explicit administrator invocation and the `--confirm-restore` flag.
- **PostgreSQL or RustFS Major Version Upgrades:** Because `deploy_from_app.sh` uses `--no-deps app`, database and object storage container images (`postgres:16-alpine` and `rustfs/rustfs:1.0.1`) are pinned and never upgraded automatically. Any future major version upgrade must be scheduled during a maintenance window after running `./scripts/backup_production.sh`.
