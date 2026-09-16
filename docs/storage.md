# File storage (MinIO)

Uploaded files are stored in **MinIO** using the S3-compatible API. MySQL/PostgreSQL keeps only metadata (`file_storage`). Binaries are never stored as database BLOBs.

## Local setup

1. Start MinIO (and PostgreSQL) with Docker Compose:

```bash
docker compose -f docker/docker-compose.yml up -d minio postgres
```

2. Configure environment variables (see [`.env.example`](../.env.example)):

| Variable | Purpose | Local default |
| :--- | :--- | :--- |
| `MINIO_ENDPOINT` | S3 API used by Spring Boot | `http://localhost:9000` when the app runs on the host; `http://minio:9000` when the app runs in Compose |
| `MINIO_PUBLIC_ENDPOINT` | Browser-facing endpoint for short-lived presigned URLs | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | Access key (must match `MINIO_ROOT_USER`) | set locally; never commit production values |
| `MINIO_SECRET_KEY` | Secret key (must match `MINIO_ROOT_PASSWORD`) | set locally; never commit production values |
| `MINIO_BUCKET` | Private bucket name | `erp-files` |
| `MINIO_REGION` | S3 region string | `us-east-1` |
| `STORAGE_TYPE` | `minio` in every real environment | `minio` |

3. Endpoints:

- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

The application creates the configured bucket on startup if it is missing. The bucket is private. Downloads go through authenticated Spring Boot endpoints (`/api/upload/view/{id}`, `/api/upload/download/{id}`) or a short-lived presigned URL from `GET /api/upload/{id}/download-url`.

### IDE vs Docker networking

| How Spring Boot runs | `MINIO_ENDPOINT` |
| :--- | :--- |
| From the IDE / `./mvnw spring-boot:run` | `http://localhost:9000` |
| Inside Docker Compose (`app` service) | `http://minio:9000` |

Do not hardcode either value in Java. Use environment variables.

The `dev` profile defaults to the local Docker MinIO placeholders (`marbleminio` / `marbleminio_change_me`) so developers can start quickly. Production must set real keys in the host environment file.

## Production (Ubuntu)

Run MinIO as a Docker container with a persistent data directory:

```text
Ubuntu Server
├── Docker
│   ├── marble-erp-postgres
│   └── marble-minio   (volume or /opt/minio/data)
└── marble-erp-platform (JAR or app container)
```

Set a host path with `MINIO_DATA_DIR=/opt/minio/data` so object data survives container recreation. Example:

```bash
sudo mkdir -p /opt/minio/data
export MINIO_DATA_DIR=/opt/minio/data
docker compose -f docker/docker-compose.yml up -d minio
```

Native JAR installs (systemd) should still run MinIO in Docker and point the JAR at it:

```bash
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_PUBLIC_ENDPOINT=https://files.example.com   # only if MinIO is reverse-proxied
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
MINIO_BUCKET=erp-files
```

Do not store production objects only inside the container writable layer.

## Backups

Backing up PostgreSQL is **not** enough.

| Data | What it contains |
| :--- | :--- |
| PostgreSQL dump | File metadata (`file_storage`: object key, original name, entity link) |
| MinIO data directory | The actual images, SVG, PDF, Office, and other binaries |

Backup both. Restore both together. A metadata-only restore leaves download URLs pointing at missing objects.

## Migrating existing local files

Legacy files under `media/` and `uploads/` are **not** deleted automatically.

1. Keep MinIO running and confirm the application can upload a new test file.
2. Dry-run:

```bash
STORAGE_MIGRATE_ENABLED=true STORAGE_MIGRATE_DRY_RUN=true ./mvnw spring-boot:run
```

3. Apply (still leaves original files on disk after a verified upload + metadata update):

```bash
STORAGE_MIGRATE_ENABLED=true STORAGE_MIGRATE_DRY_RUN=false ./mvnw spring-boot:run
```

The job is idempotent: rows that already have an `object_key` present in MinIO are skipped. Delete leftover filesystem files only after you have verified downloads from MinIO.

## Application URLs

| Method | Path | Purpose |
| :--- | :--- | :--- |
| `POST` | `/api/upload` | Authenticated upload |
| `GET` | `/api/upload/view/{id}` | Inline display after authorization |
| `GET` | `/api/upload/download/{id}` | Attachment download after authorization |
| `GET` | `/api/upload/{id}/download-url` | Short-lived presigned URL after authorization |
| `DELETE` | `/api/upload/{id}` | Soft-delete metadata, then delete the object |
