# Özerler Mermer ERP - How to Run Locally

This guide provides step-by-step instructions for running the Özerler Mermer ERP platform locally on Windows, macOS, or Linux for development and testing.

---

## 1. System Requirements & Prerequisites

Ensure the following tools are installed on your workstation:

| Dependency | Minimum Version | Notes |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **24+** | Recommended: [Eclipse Temurin 24](https://adoptium.net/) |
| **Docker Desktop / Docker Engine** | **24.0+** | Required for PostgreSQL 16 and MinIO containers |
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

### Option A: Hybrid Run (Docker PostgreSQL + Spring Boot Local) *(Recommended)*

This setup runs PostgreSQL and MinIO in Docker and executes the Spring Boot backend directly on your host machine, enabling hot reload and fast debugging.

#### On Windows (PowerShell):
```powershell
# 1. Start PostgreSQL container and run the app
.\scripts\run_local.ps1
```

Or step-by-step manually:
```powershell
# Start PostgreSQL 16 and MinIO containers
docker compose -f docker/docker-compose.yml up -d postgres minio

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
# Start PostgreSQL and MinIO containers
docker compose -f docker/docker-compose.yml up -d postgres minio

# Start Spring Boot application
./mvnw spring-boot:run
```

The application will start on **`http://localhost:8080`**.

---

### Option B: Full Stack in Docker Compose

Run Spring Boot, PostgreSQL, and MinIO together in isolated containers.

```bash
# Start both PostgreSQL and the application
docker compose -f docker/docker-compose.yml up -d --build

# View real-time container logs
docker compose -f docker/docker-compose.yml logs -f app
```

> [!NOTE]
> In the full Docker Compose setup, the application exposes port **81**:
> - App URL: **`http://localhost:81`**
> - Admin Login: **`http://localhost:81/account/adminlogin/`**

To stop the containers:
```bash
docker compose -f docker/docker-compose.yml down
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
