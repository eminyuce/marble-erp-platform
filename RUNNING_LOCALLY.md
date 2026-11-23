# Özerler Mermer ERP - How to Run Locally

This guide provides step-by-step instructions for running the Özerler Mermer ERP platform locally on Windows, macOS, or Linux for development and testing.

---

## 1. System Requirements & Prerequisites

Ensure the following tools are installed on your workstation:

| Dependency | Minimum Version | Notes |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **24+** | Recommended: [Eclipse Temurin 24](https://adoptium.net/) |
| **Docker Desktop / Docker Engine** | **24.0+** | Required for PostgreSQL 16 and RustFS containers |
| **Node.js & npm** *(Optional)* | **22.x+** | Only needed if modifying Tailwind CSS in `frontend/` |
| **Git** | Any modern version | Source code version control |

Verify your environment by running:
```bash
# Verify Java version (must report 24 or newer)
java -version

# Verify Docker daemon is running
docker ps
```

---

## 2. Quick Start Options

### Which profile to use

| Profile | Config file | When |
| :--- | :--- | :--- |
| `dev` | `application-dev.yml` | Default for `./mvnw spring-boot:run` and `scripts/run_local.sh`. PostgreSQL on `localhost:5432`. Session cookie is not Secure. |
| `local` | `application-local.yml` | No Docker. H2 file at `./data/marble_local_db`. Start with `SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run`. |
| `prod` | `application-prod.yml` | Production HTTPS and the Compose stacks. Session cookie defaults to Secure. The local HTTP Compose file sets `SESSION_COOKIE_SECURE=false` so a browser on `http://localhost` still stores the cookie. |
| `test` | `src/test/resources/application-test.yml` | JUnit only. In-memory H2. |

Shared settings (session store, Flyway locations, cookie name) stay in `application.yml`. There is no `.env` file. Export a variable only when you need to override a default.

### Option A: Hybrid Run (Docker PostgreSQL + Spring Boot Local) *(Recommended)*

This setup runs PostgreSQL and RustFS in Docker and executes the Spring Boot backend directly on your host machine, enabling hot reload and fast debugging.

#### On Windows (PowerShell):
```powershell
# 1. Start PostgreSQL container and run the app
.\scripts\run_local.ps1
```

Or step-by-step manually:
```powershell
# Start PostgreSQL 16 and RustFS containers
docker compose -f docker/docker-compose.yml up -d postgres rustfs

# Start Spring Boot application on port 8080 (dev profile)
.\mvnw.cmd spring-boot:run
```

#### On Linux / macOS (Bash):
```bash
# 1. Make script executable and run
chmod +x scripts/run_local.sh
./scripts/run_local.sh
```

Or step-by-step manually:
```bash
# Start PostgreSQL and RustFS containers
docker compose -f docker/docker-compose.yml up -d postgres rustfs

# Start Spring Boot application
./mvnw spring-boot:run
```

The application will start on **`http://localhost:8080`**.

---

### Option B: Full Stack in Docker Compose (Local Build or Docker Hub Image)

Because PostgreSQL (`postgres:16-alpine`), RustFS (`rustfs/rustfs:1.0.1`), and Marble ERP (`guvenulu/marble-erp-platform:latest`) run as separate containers, running `docker run ... guvenulu/marble-erp-platform:latest` alone will fail unless PostgreSQL and RustFS are already running and reachable. Additionally, the application listens on port **81** inside the container (not `85`), so host port mappings must target `:81` (for example `-p 8081:81` or `-p 81:81`).

#### 1. From the Cloned Git Repository:
```bash
# Pull pre-built images from Docker Hub and start PostgreSQL + RustFS + App
docker compose -f docker/docker-compose.yml pull
docker compose -f docker/docker-compose.yml up -d

# Or build the image locally from source and start all services:
docker compose -f docker/docker-compose.yml up -d --build

# View real-time container logs
docker compose -f docker/docker-compose.yml logs -f app
```

#### 2. Without Cloning the Repository (Linux / macOS — Extract Compose from Docker Hub Image):
```bash
mkdir -p ~/marble-erp-local && cd ~/marble-erp-local
docker run --rm guvenulu/marble-erp-platform:latest --print-compose > docker-compose.yml
docker compose up -d
```

#### 3. Windows PowerShell Copy-Paste Setup (Port `8081 -> 81` + PostgreSQL + RustFS):
If you are on Windows PowerShell and want to run the complete stack from Docker Hub on `http://localhost:8081`:

```powershell
# 1. Remove any standalone failed container if present
docker rm -f marble-erp 2>$null

# 2. Create and enter a local working directory
mkdir -Force "$HOME\marble-erp-local"
cd "$HOME\marble-erp-local"

# 3. Write docker-compose.yml (PostgreSQL + RustFS + Marble ERP on port 8081:81)
@"
services:
  postgres:
    image: postgres:16-alpine
    container_name: marble-erp-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: marble_erp
      POSTGRES_USER: marbleuser
      POSTGRES_PASSWORD: marblepass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U marbleuser -d marble_erp"]
      interval: 5s
      timeout: 5s
      retries: 10

  rustfs-perms:
    image: alpine:3.20
    user: "0:0"
    command: ["sh", "-c", "chown -R 10001:10001 /data /logs"]
    volumes:
      - rustfs_data:/data
      - rustfs_logs:/logs

  rustfs:
    image: rustfs/rustfs:1.0.1
    container_name: marble-rustfs
    restart: unless-stopped
    depends_on:
      rustfs-perms:
        condition: service_completed_successfully
    environment:
      RUSTFS_ACCESS_KEY: marblerustfs
      RUSTFS_SECRET_KEY: marblerustfs_change_me
      RUSTFS_VOLUMES: /data
      RUSTFS_ADDRESS: ":9000"
      RUSTFS_CONSOLE_ADDRESS: ":9001"
      RUSTFS_CONSOLE_ENABLE: "true"
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - rustfs_data:/data
      - rustfs_logs:/logs

  app-perms:
    image: alpine:3.20
    user: "0:0"
    command: ["sh", "-c", "chown -R 1001:1001 /uploads /media"]
    volumes:
      - uploads_data:/uploads
      - media_data:/media

  app:
    image: guvenulu/marble-erp-platform:latest
    container_name: marble-erp
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
      rustfs:
        condition: service_started
      app-perms:
        condition: service_completed_successfully
    ports:
      - "8081:81"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: 81
      DB_URL: jdbc:postgresql://postgres:5432/marble_erp
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: marble_erp
      DB_USERNAME: marbleuser
      DB_PASSWORD: marblepass
      APP_UPLOAD_DIR: /app/uploads
      APP_MEDIA_DIR: /app/media
      STORAGE_TYPE: rustfs
      RUSTFS_ENDPOINT: http://rustfs:9000
      RUSTFS_PUBLIC_ENDPOINT: http://localhost:9000
      RUSTFS_ACCESS_KEY: marblerustfs
      RUSTFS_SECRET_KEY: marblerustfs_change_me
      RUSTFS_BUCKET: erp-files
      RUSTFS_REGION: us-east-1
      PRESIGNED_IMAGE_EXPIRY_MINUTES: "30"
      PRESIGNED_DOCUMENT_EXPIRY_MINUTES: "5"
      SESSION_STORE_TYPE: jdbc
      SESSION_TIMEOUT: 30m
      SESSION_COOKIE_SECURE: "false"
    volumes:
      - uploads_data:/app/uploads
      - media_data:/app/media

volumes:
  postgres_data:
  rustfs_data:
  rustfs_logs:
  uploads_data:
  media_data:
"@ | Set-Content -Encoding UTF8 docker-compose.yml

# 4. Start the full stack
docker compose up -d

# 5. Check container status and logs
docker compose ps
docker logs -f marble-erp
```

> [!NOTE]
> - Default `docker/docker-compose.yml` exposes port **81** (`http://localhost:81/account/adminlogin/`).
> - In the Windows PowerShell snippet above, host port **8081** maps to container port **81** (`http://localhost:8081/account/adminlogin/`).

To stop the containers without deleting your local database volume:
```bash
docker compose down
```

---

### Option C: Frontend Assets (Tailwind CSS 4)

The repository comes with pre-compiled CSS files in `src/main/resources/static/css/`. If you modify styles in `frontend/src/`:

```bash
cd frontend
npm install
npm run build      # Production minified build
# Or for live compilation during UI design:
npm run dev        # Watch mode
cd ..
```

---

## 3. Seed Demo Data & Credentials

Database migrations and initial schemas are automatically applied via **Flyway** on first launch.

### Default Admin Accounts

| Account Name | Username / Email | Password | Role |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin` *(or `admin@example.com`)* | `changeit` | `ROLE_ADMIN`, `ROLE_USER` |
| **Eimece Admin** | `eimece_admin` *(or `admin@eimece.test`)* | `B2u5c8JB` | `ROLE_ADMIN`, `ROLE_EXECUTIVE` |
| **Factory Manager** | `factory_mgr` *(or `fabrika@ozerler.com`)* | `changeit` | `ROLE_FACTORY_MANAGER` |
| **Site Engineer** | `site_chief` *(or `santiye@ozerler.com`)* | `changeit` | `ROLE_SITE_ENGINEER` |

### Optional: Populating Rich Demo Records

To populate sample quarry blocks, cut orders, slabs, and barcode records:
```powershell
# Windows
.\scripts\seed_dummy_data.ps1

# Linux / macOS
./scripts/seed_dummy_data.sh
```

---

## 4. Key Local URLs & Endpoints

| Resource | Local Dev URL | Full Docker URL | Description |
| :--- | :--- | :--- | :--- |
| **Admin Login** | `http://localhost:8080/account/adminlogin/` | `http://localhost:81/account/adminlogin/` | Primary administrative entry |
| **Dashboard** | `http://localhost:8080/` | `http://localhost:81/` | Executive KPI dashboard |
| **Block Inventory** | `http://localhost:8080/erp/blocks/` | `http://localhost:81/erp/blocks/` | Quarry block tracking |
| **Gangsaw Production**| `http://localhost:8080/erp/production/` | `http://localhost:81/erp/production/` | Slab cutting orders |
| **Workshop Orders** | `http://localhost:8080/erp/workshop/` | `http://localhost:81/erp/workshop/` | Dimensioning & bridge sawing |
| **Stone Passport** | `http://localhost:8080/passport/` | `http://localhost:81/passport/` | Public QR code verification |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | `http://localhost:81/swagger-ui.html` | OpenAPI interactive docs |
| **Health Check** | `http://localhost:8080/actuator/health` | `http://localhost:81/actuator/health` | System health probe |

---

## 4b. RustFS presigned URLs

There is no `.env` file. Defaults live in the YAML files and in the Compose `environment` entries.

| Variable | dev / local HTTP | prod |
| :--- | :--- | :--- |
| `RUSTFS_ENDPOINT` | `http://localhost:9000` when the JVM runs on the host. `http://rustfs:9000` inside Compose (the dev profile default). | `http://rustfs:9000` (app only, Docker DNS) or `http://127.0.0.1:9000` for a host-side JAR |
| `RUSTFS_PUBLIC_ENDPOINT` | `http://localhost:9000` | `https://files.example.com` — must be **https** behind Nginx or another reverse proxy. Startup fails for plain http or an internal host (`rustfs`, `localhost`). |
| `RUSTFS_BUCKET` | `erp-files` | `erp-files` (private; no permanent public URL) |
| `RUSTFS_REGION` | `us-east-1` | `us-east-1` |
| `RUSTFS_ACCESS_KEY` / `RUSTFS_SECRET_KEY` | dev defaults in `application-dev.yml` | required; empty values abort startup. Never log the secret. |
| `PRESIGNED_IMAGE_EXPIRY_MINUTES` | `30` | `30` |
| `PRESIGNED_DOCUMENT_EXPIRY_MINUTES` | `5` | `5` |

The S3 client uses the internal endpoint for upload, delete, and head. The presigner uses the public endpoint, so the browser downloads the file directly from RustFS. Presigned URLs are created per page render and per `GET /api/upload/{id}/download-url`. They are not stored in the database.

To change the image expiry from 30 minutes to 15 minutes without a code change:

```bash
export PRESIGNED_IMAGE_EXPIRY_MINUTES=15
./mvnw spring-boot:run
```

Or edit the default in `src/main/resources/application.yml`:

```yaml
presigned:
  url:
    image-expiry-minutes: ${PRESIGNED_IMAGE_EXPIRY_MINUTES:15}
```

### Manual test

1. Start PostgreSQL and RustFS, then the app on the host (`./scripts/run_local.sh` or `.\scripts\run_local.ps1`). That sets both RustFS endpoints to `http://localhost:9000`.
2. Sign in and open a block that has a photo, for example `http://localhost:8080/blocks/1` (gallery) or `http://localhost:8080/blocks/1/photos`.
3. View page source. The photo `<img>` `src` is a `http://localhost:9000/...` URL containing `X-Amz-Expires=1800`. It is not `/api/upload/view/...` when the file already lives in RustFS.
4. Open that `src` in the browser. The bytes come from RustFS, not from the application.
5. Wait 31 minutes and open the same URL again. RustFS returns an expired signature (`AccessDenied` or request expired). The bucket stays private.
6. `GET /api/upload/{id}/download-url` for a PDF returns `expiresInSeconds` `300` and `X-Amz-Expires=300`.

Clock skew between the app and RustFS can make a fresh URL look expired. A page on https that embeds an http RustFS URL is mixed content and the browser will block it. A JavaScript `fetch` of the presigned URL needs CORS on RustFS; a plain `<img src>` does not.

## 5. Running Automated Tests

Run the full JUnit 5 & Mockito test suite:

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

To run a specific test class:
```bash
.\mvnw.cmd test -Dtest=GlobalExceptionHandlerTest
```

---

## 6. Troubleshooting Local Setup

### Port 8080 Already in Use
Set an alternative port via environment variable:
```powershell
$env:SERVER_PORT="8085"
.\mvnw.cmd spring-boot:run
```

### PostgreSQL Connection Refused
- Ensure Docker Desktop is running.
- Verify container status with `docker ps` and ensure `marble-erp-postgres` is listed as `healthy`.
- Check database logs: `docker logs marble-erp-postgres`.

---

## 7. Shared sessions across replicas

Tomcat no longer keeps the login in memory. Spring Session JDBC writes it to PostgreSQL (`SPRING_SESSION` / `SPRING_SESSION_ATTRIBUTES`). Flyway migration `V33` creates those tables. `spring.session.jdbc.initialize-schema` stays `never` so production does not create them beside Flyway.

The browser cookie is `SESSION` (HttpOnly, SameSite=Lax). `dev` and `local` default `server.servlet.session.cookie.secure` to `false` so HTTP on localhost works. `application-prod.yml` defaults it to `true` because production is HTTPS; that does not require `SESSION_COOKIE_SECURE` in the environment. Set `SESSION_COOKIE_SECURE=false` only for an explicit HTTP exception. Idle timeout is `SESSION_TIMEOUT` (default `30m`). Expired rows are deleted every minute (`SESSION_CLEANUP_CRON`, default `0 * * * * *`).

`SESSION_STORE_TYPE` is kept for operators. Spring Boot 4 no longer reads `spring.session.store-type`; the JDBC starter on the classpath is what selects the store.

### Two local processes, one database

`docker/docker-compose.yml` sets `container_name` on the app service, so `docker compose up --scale app=2` will not start a second container. Run two JVM processes against the same Postgres instead:

```bash
docker compose -f docker/docker-compose.yml up -d postgres

export DB_URL=jdbc:postgresql://localhost:5432/marble_erp
export DB_USERNAME=marbleuser
export DB_PASSWORD=marblepass
export SESSION_STORE_TYPE=jdbc
export SESSION_TIMEOUT=30m
# dev profile already leaves the session cookie non-Secure. Do not export SESSION_COOKIE_SECURE.

SERVER_PORT=8080 ./mvnw spring-boot:run
# second terminal, same exports
SERVER_PORT=8081 ./mvnw spring-boot:run
```

1. Open `http://localhost:8080/account/adminlogin/` and sign in (for example `admin@eimece.test` / `B2u5c8JB`).
2. In the browser devtools, copy the `SESSION` cookie value.
3. Call the other port with that cookie. The dashboard must load. A redirect to the login page means the session did not survive the replica change.

```bash
curl -s -o /dev/null -w "%{http_code} %{redirect_url}\n" \
  -H "Cookie: SESSION=<paste>" \
  http://localhost:8081/admin/dashboard
```

Expect `200` (or a redirect that stays inside the app, not `/account/adminlogin/`). Repeat a POST that sends the `XSRF-TOKEN` cookie and the `_csrf` field (HTMX does this). It must not fail with a CSRF error.

4. Stop the process that served the login (`Ctrl+C` on port 8080, or kill that JVM) and request port 8081 again with the same cookie. The user must still be authenticated.
5. Confirm a row exists, then that an expired row disappears after the cleanup minute:

```bash
docker exec -it marble-erp-postgres psql -U marbleuser -d marble_erp \
  -c "SELECT SESSION_ID, PRINCIPAL_NAME, TO_TIMESTAMP(EXPIRY_TIME/1000) FROM SPRING_SESSION;"
```

### Swarm (production)

With 2 or 3 app replicas and one Postgres service:

1. Log in through the load balancer.
2. Note the replica that handled the request (`docker service ps <stack>_app`).
3. Stop that task (`docker kill <container>` or scale `2 -> 1` and back to `2`).
4. Reload the dashboard and submit a normal form. The user stays logged in, the browser is not sent to `/account/adminlogin/`, and CSRF does not fail.
5. Roll the service (`docker service update --force <stack>_app`). Existing `SESSION` cookies must still open the dashboard after the new tasks are healthy.

Remember-me stays a signed cookie (`remember-me`), independent of the JDBC store. Every replica already uses the same key, so that cookie still works after a replica switch. Logout clears `SESSION`, the old `JSESSIONID` name, and `remember-me`.
