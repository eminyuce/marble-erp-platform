# File storage (RustFS)

Uploaded files are stored in **RustFS** using the S3-compatible API. PostgreSQL keeps only metadata (`file_storage`). Binaries are never stored as database BLOBs.

RustFS runs as a Docker container. The Spring Boot process talks to it with the AWS SDK; it does not embed an object store.

## Local setup

1. Start RustFS (and PostgreSQL) with Docker Compose:

```bash
docker compose -f docker/docker-compose.yml up -d rustfs postgres
```

2. Configure environment variables (see [`.env.example`](../.env.example)):

| Variable | Purpose | Local default |
| :--- | :--- | :--- |
| `RUSTFS_ENDPOINT` | S3 API used by Spring Boot | `http://localhost:9000` when the app runs on the host; `http://rustfs:9000` when the app runs in Compose |
| `RUSTFS_PUBLIC_ENDPOINT` | Browser-facing endpoint for short-lived presigned URLs | `http://localhost:9000` |
| `RUSTFS_ACCESS_KEY` | Access key (must match the RustFS container) | set locally; never commit production values |
| `RUSTFS_SECRET_KEY` | Secret key (must match the RustFS container) | set locally; never commit production values |
| `RUSTFS_BUCKET` | Private bucket name | `erp-files` |
| `RUSTFS_REGION` | S3 region string | `us-east-1` |
| `STORAGE_TYPE` | `rustfs` in every real environment | `rustfs` |

3. Endpoints:

- RustFS API: `http://localhost:9000`
- RustFS Console: `http://localhost:9001`

The application creates the configured bucket on startup if it is missing. The bucket is private. Downloads go through authenticated Spring Boot endpoints (`/api/upload/view/{id}`, `/api/upload/download/{id}`) or a short-lived presigned URL from `GET /api/upload/{id}/download-url`.

### IDE vs Docker networking

| How Spring Boot runs | `RUSTFS_ENDPOINT` |
| :--- | :--- |
| From the IDE / `./mvnw spring-boot:run` | `http://localhost:9000` |
| Inside Docker Compose (`app` service) | `http://rustfs:9000` |

Do not hardcode either value in Java. Use environment variables.

The `dev` profile defaults to the local Docker RustFS placeholders (`marblerustfs` / `marblerustfs_change_me`) so developers can start quickly. Production must set real keys in the host environment file. Do not use the well-known `rustfsadmin` credential.

## Production (Ubuntu)

Run RustFS as a Docker container with a persistent data directory. The container process is user `10001`. A Compose init container sets that ownership before RustFS starts.

```text
Ubuntu Server
├── Docker
│   ├── marble-erp-postgres
│   └── marble-rustfs   (volume or /opt/rustfs/data)
└── marble-erp-platform (JAR or app container)
```

Set a host path with `RUSTFS_DATA_DIR=/opt/rustfs/data` so object data survives container recreation. Example:

```bash
sudo mkdir -p /opt/rustfs/data
export RUSTFS_DATA_DIR=/opt/rustfs/data
docker compose -f docker/docker-compose.yml up -d rustfs
```

Native JAR installs (systemd) should still run RustFS in Docker and point the JAR at it:

```bash
RUSTFS_ENDPOINT=http://127.0.0.1:9000
RUSTFS_PUBLIC_ENDPOINT=https://files.example.com   # only if RustFS is reverse-proxied
RUSTFS_ACCESS_KEY=...
RUSTFS_SECRET_KEY=...
RUSTFS_BUCKET=erp-files
```

Do not store production objects only inside the container writable layer.

RustFS does not read a MinIO data directory. If objects already live in MinIO, copy them with an S3 client into the new RustFS bucket before stopping the MinIO container, then keep the MinIO disk only as a backup until downloads from RustFS are verified.

## Backups

Backing up PostgreSQL is **not** enough.

| Data | What it contains |
| :--- | :--- |
| PostgreSQL dump | File metadata (`file_storage`: object key, original name, entity link) |
| RustFS data directory | The actual images, SVG, PDF, Office, and other binaries |

Backup both. Restore both together. A metadata-only restore leaves download URLs pointing at missing objects.

## Migrating existing local files

Legacy files under `media/` and `uploads/` are **not** deleted automatically.

1. Keep RustFS running and confirm the application can upload a new test file.
2. Dry-run:

```bash
STORAGE_MIGRATE_ENABLED=true STORAGE_MIGRATE_DRY_RUN=true ./mvnw spring-boot:run
```

3. Apply (still leaves original files on disk after a verified upload + metadata update):

```bash
STORAGE_MIGRATE_ENABLED=true STORAGE_MIGRATE_DRY_RUN=false ./mvnw spring-boot:run
```

The job is idempotent: rows that already have an `object_key` present in RustFS are skipped. Delete leftover filesystem files only after you have verified downloads from RustFS.

## Application URLs

| Method | Path | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/upload` | Authenticated upload |
| `GET` | `/api/upload/view/{id}` | Inline display after authorization |
| `GET` | `/api/upload/download/{id}` | Attachment download after authorization |
| `GET` | `/api/upload/{id}/download-url` | Short-lived presigned URL after authorization |
| `DELETE` | `/api/upload/{id}` | Soft-delete metadata, then delete the object |
