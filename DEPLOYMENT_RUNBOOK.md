# Özerler Mermer ERP — Production Deployment Guide & Runbook

This document is the operational guide for deploying and maintaining the Özerler Mermer ERP platform on this Linux production host (`eyucedev`).

---

## 1. Quick Deployment ("Next Time")

Whenever you push new changes to `main` and want to update the production server, simply SSH into the server and run:

```bash
cd /home/eyuce/marble-erp-platform
git pull origin main
./scripts/deploy_production.sh
```

> [!TIP]
> - Running without flags prompts `Deploy Özerler Mermer ERP to production on this host? [Y/n]: ` — simply press **Enter** or **Y**.
> - For fully automated / non-interactive deploys, pass `-y` or `--yes`:
>   ```bash
>   ./scripts/deploy_production.sh -y
>   ```

---

## 2. What the Deploy Script Automates

Executing [`scripts/deploy_production.sh`](file:///home/eyuce/marble-erp-platform/scripts/deploy_production.sh) performs the following end-to-end steps automatically:

1. **Pre-flight Checks**:
   - Confirms Linux host and OpenJDK 24+ (running OpenJDK 25).
   - Verifies Docker PostgreSQL container (`marble-erp-postgres`) is healthy and listening on port `5432`. If stopped, it automatically starts it.
   - Verifies `/opt/marble-erp/marble-erp.env` exists, contains no placeholders, and has strict permissions (`640`).
2. **Frontend Build**:
   - Executes `npm install && npm run build` inside `frontend/` to compile Tailwind CSS assets into `src/main/resources/static/css/app.css`.
3. **Backend Build**:
   - Runs `./mvnw clean package -DskipTests` to package `target/marble-erp-platform-1.0.0.jar`.
4. **Zero-Orphan Shutdown**:
   - Gracefully stops the `marble-erp` systemd service (and sends SIGTERM/SIGKILL to any stray `/opt/marble-erp/app.jar` JVMs if needed).
5. **Safe Rollback Backup**:
   - Backs up the current active JAR:
     `/opt/marble-erp/app.jar` ➔ `/opt/marble-erp/app.jar.bak.<YYYYMMDD_HHMMSS>`
   - Automatically cleans up older backups, keeping the last 8 versions.
6. **JAR Installation**:
   - Copies the fresh build to `/opt/marble-erp/app.jar`.
   - Sets correct ownership (`eyuce:eyuce`).
7. **Systemd Unit Synchronization**:
   - Ensures `/etc/systemd/system/marble-erp.service` is synced with ZGC garbage collection (`-server -XX:+UseZGC -XX:MaxRAMPercentage=75.0`), `prod` profile, and `docker.service` dependency.
   - Reloads systemd daemon and restarts `marble-erp.service`.
8. **Automated Health Probes**:
   - Waits for local health endpoint (`http://127.0.0.1:8080/health/` and `/actuator/health`) until `status: UP`.
   - Probes the live public domain (`https://ozerler.naklink.com/health/`) to ensure external traffic routing is operational.

---

## 3. Server Layout & Configuration

| Path | Purpose | Notes |
| :--- | :--- | :--- |
| `/home/eyuce/marble-erp-platform` | Git repository | Source code, Tailwind frontend, Maven wrapper |
| `/opt/marble-erp/app.jar` | Production executable | Live Spring Boot fat JAR |
| `/opt/marble-erp/app.jar.bak.*` | Rollback archives | Kept up to 8 timestamped backups |
| `/opt/marble-erp/marble-erp.env` | Production environment | Database credentials, CORS origins, ports (`chmod 640`) |
| `/opt/marble-erp/uploads` | Legacy upload storage | Kept for MinIO migration; not the live store |
| `/opt/minio/data` | MinIO object data | Must be backed up together with PostgreSQL |
| `/etc/systemd/system/marble-erp.service` | systemd service unit | Manages the live Java process under `eyuce:eyuce` |
| `marble-erp-postgres` | Docker container | PostgreSQL 16 on port `5432` (`restart: always`) |
| `marble-minio` | Docker container | MinIO S3 API `9000`, console `9001` |

### Active Environment Variables (`/opt/marble-erp/marble-erp.env`)

```ini
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
PORT=8080

# Database Configuration (Docker container: marble-erp-postgres)
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=marble_erp
DB_USERNAME=marbleuser
DB_PASSWORD=marblepass
DB_URL=jdbc:postgresql://127.0.0.1:5432/marble_erp

# Connection Pool Settings
DB_POOL_MAX=10
DB_POOL_MIN_IDLE=2

# Storage (MinIO is live; local dirs are migration sources only)
APP_UPLOAD_DIR=/opt/marble-erp/uploads
APP_MEDIA_DIR=/opt/marble-erp/media
STORAGE_TYPE=minio
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_PUBLIC_ENDPOINT=http://127.0.0.1:9000
MINIO_BUCKET=erp-files
MINIO_DATA_DIR=/opt/minio/data

# Public Domain & CORS
CORS_ALLOWED_ORIGIN=https://ozerler.naklink.com
CORS_ALLOWED_ORIGIN_PATTERN=https://*.naklink.com

# Rate Limiting
RATE_LIMIT_LOGIN_MAX=10
RATE_LIMIT_LOGIN_WINDOW=60
RATE_LIMIT_FORM_MAX=30
RATE_LIMIT_FORM_WINDOW=60
RATE_LIMIT_GENERAL_MAX=120
RATE_LIMIT_GENERAL_WINDOW=60
```

---

## 4. Useful Operational Commands

### Check Service Status
```bash
sudo systemctl status marble-erp
```

### View Live Application Logs
```bash
sudo journalctl -u marble-erp -f -n 100
```

### View Recent Errors
```bash
sudo journalctl -u marble-erp -p err -n 50 --no-pager
```

### Check Database Container
```bash
docker ps -f name=marble-erp-postgres
docker logs --tail 50 marble-erp-postgres
```

### Test Health Endpoints Manually
```bash
# Internal health
curl -s http://127.0.0.1:8080/health/ | jq .
curl -s http://127.0.0.1:8080/actuator/health | jq .

# Public health
curl -I https://ozerler.naklink.com/health/
```

---

## 5. Rollback Procedure

If a new build fails or introduces a critical regression, roll back to the previous JAR in seconds:

```bash
# 1. Stop service
sudo systemctl stop marble-erp

# 2. List available backups
ls -lt /opt/marble-erp/app.jar.bak.*

# 3. Restore the desired backup (replace TIMESTAMP with actual timestamp)
sudo cp -a /opt/marble-erp/app.jar.bak.<TIMESTAMP> /opt/marble-erp/app.jar

# 4. Start service
sudo systemctl start marble-erp

# 5. Verify health
curl -sf http://127.0.0.1:8080/health/
```

> [!NOTE]
> Database schema updates are managed by Flyway migrations (`src/main/resources/db/migration/`). Migrations run forward on application startup.

---

## 6. Deploy Script Flags Reference

| Flag | Description |
| :--- | :--- |
| *(no flags)* | Prompts `[Y/n]` interactively in terminal |
| `-y`, `--yes` | Non-interactive deploy; skips confirmation prompt |
| `--dry-run` | Shows preview of actions without altering the host |
| `--skip-frontend` | Skips `npm install && npm run build` (faster backend-only update) |
| `--skip-build` | Skips frontend & Maven build; restarts existing `/opt/marble-erp/app.jar` |
| `--print-unit` | Prints the default systemd unit specification and exits |
| `-h`, `--help` | Displays help manual |
