# Özerler Mermer ERP Platform

> **Enterprise-grade, end-to-end physical traceability, 10-code scrap management, and dynamic Activity-Based Costing (ABC) platform for the natural stone industry.**

[![Java](https://img.shields.io/badge/Java-24-orange.svg)](https://openjdk.org/projects/jdk/24/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Port](https://img.shields.io/badge/Port-81-blue.svg)](http://localhost:81)
[![PMD](https://img.shields.io/badge/PMD-7.17.0%20(0%20Violations)-blueviolet.svg)](https://pmd.github.io/)
[![JaCoCo](https://img.shields.io/badge/Coverage-JaCoCo%200.8.13-success.svg)](https://www.jacoco.org/)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%2016-336791.svg)](https://www.postgresql.org/)
[![Storage](https://img.shields.io/badge/Storage-MinIO%20(S3)-red.svg)](https://min.io/)

---

## 🎯 Value Proposition

**Özerler Mermer ERP** replaces generic inventory tools with domain-driven stone lifecycle management. From raw quarry block extraction ($m^3$, tonnage deviation analysis) through gangsaw cutting, resin/polishing lines, custom workshop sizing, and architectural site Work Breakdown Structures (WBS), every physical transformation is tracked with full bidirectional genealogy, dynamic grade-weighted pricing, and root-cause scrap telemetry.

---

## 🚀 Key Features

- **Quarry Block Intelligence**: 3-axis dimensional measurement ($m^3$), theoretical vs. weighbridge scale variance tracking ($>5\%$ deviation alerts), and gangsaw dispatch workflows.
- **Gangsaw & Grade Multiplier Costing**: Weighted allocation algorithm ($K_{Extra}=1.30$, $K_A=1.15$, $K_B=1.00$, $K_C=0.65$) distributing block cost based on realized surface quality.
- **10-Reason Scrap Management**: Granular root-cause tracking from `FR-01` (Saw Dust) through `FR-10` (Site Installation Breakage) with real-time financial impact calculation.
- **Genealogy & Digital Stone Passport**: Bidirectional lineage trees mapping final installation tiles back to source slabs, production orders, and extraction quarries via public QR-code URLs.
- **Smart Pricing & Margin Guardrails**: Real-time margin simulation calculating standard cost, suggested selling price, and minimum floor price warnings.
- **Enterprise System Settings & Security**: Database-backed dynamic controls for Two-Factor Authentication (2FA), Google reCAPTCHA v2/v3, dynamic SMTP dispatch, and customizable HTML email templates.
- **Operational Report Center**: Multi-tab live reporting engine exporting 6 domain datasets to auto-formatted Excel (`.xlsx`) and UTF-8 BOM CSV (`.csv`).
- **Structured JSON Observability**: Production-grade Logstash JSON logging over Logback with MDC diagnostic correlation (`traceId`, `userId`) and a dedicated `/admin/dashboard/systemhealth/` metric cockpit.
- **Object File Storage**: Block photos, SVG, PDF, Excel, and Word attachments stored in private MinIO buckets (S3 API), with metadata in PostgreSQL.

---

## 🛠️ Tech Stack

| Category | Technologies & Tools |
| :--- | :--- |
| **Backend Framework** | Java 24 (Eclipse Temurin 24.0.2), Spring Boot 4.0.7, Spring Security 7.x, Spring Data JPA |
| **Persistence & Migration** | PostgreSQL 16, Hibernate 7.x, Flyway 11.x, HikariCP |
| **Object Storage** | MinIO (S3-compatible API), AWS SDK for Java v2 |
| **Frontend & UI/UX** | Thymeleaf 3, Tailwind CSS 4, HTMX 2, Alpine.js 3, Tabulator 6, Lucide Icons |
| **Rich Editing & Uploads**| TipTap Editor, CodeMirror 6, FilePond 4 with client-side image optimization |
| **Reporting & Utilities** | Apache POI 5.3.0, Apache Commons (`commons-lang3`, `commons-collections4 4.5.0`) |
| **Logging & Telemetry** | SLF4J, Logback, `logstash-logback-encoder 8.0`, Spring Boot Actuator |
| **Quality & Testing** | JUnit 5, Mockito, AssertJ, `maven-pmd-plugin 3.28.0` (PMD 7.17.0), `jacoco-maven-plugin 0.8.13` |
| **Container & CI/CD** | Docker (Multi-stage Temurin 24), Docker Compose v2; production deploys are manual via `scripts/deploy_production.sh` |

---

## 🏛️ Architecture & Design

The platform adheres to **Layered Clean Architecture** and **Domain-Driven Design (DDD)** principles:

- **Presentation Layer (`com.ozerler.marble.controller`)**: Dedicated controllers for Administrative operations, ERP domain modules, Public Digital Passports, and JSON REST endpoints.
- **Data Transfer & Dual Mapping (`com.ozerler.marble.dto`)**: DTOs annotated with `@JsonProperty("snake_case")` paired with `@JsonAlias("camelCase")` and `@NotBlank(message = "Missing required field: {field_name}")` for complete client interoperability.
- **Domain Service Layer (`com.ozerler.marble.service`)**: Encapsulates business logic, activity-based cost calculations, gangsaw transformations, and report synthesis.
- **Object Storage (`com.ozerler.marble.storage`)**: `ObjectStorageService` abstraction over the S3 API; MinIO stores binaries, PostgreSQL stores `file_storage` metadata.
- **Persistence Layer (`com.ozerler.marble.repository`)**: Strongly typed Spring Data JPA repositories with parameterized native/HQL queries.
- **Cross-Cutting Concerns (`com.ozerler.marble.config`, `common`)**: Centralized [Constants.java](src/main/java/com/ozerler/marble/common/Constants.java), Spring Security RBAC filter chains, structured logging converters, and global exception handlers.

---

## 🏁 Getting Started

### Prerequisites
- **JDK 24+** (Eclipse Temurin 24.0.2 recommended)
- **Apache Maven 3.9+** (or bundled `./mvnw`)
- **Docker & Docker Compose** (for containerized setup)
- **Node.js 22+ & npm** (only if rebuilding frontend assets)

### Environment Variables

Copy [`.env.example`](.env.example) for a full local template. Do not commit production secrets.

| Variable | Description | Default (Local) |
| :--- | :--- | :--- |
| `SERVER_PORT` | Application HTTP port | `81` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `DB_HOST` | PostgreSQL hostname | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database schema name | `marble_erp` |
| `DB_USERNAME` | Database username | `marbleuser` |
| `DB_PASSWORD` | Database password | `marblepass` |
| `MINIO_ENDPOINT` | MinIO S3 API used by Spring Boot | `http://localhost:9000` (IDE) / `http://minio:9000` (Compose app) |
| `MINIO_PUBLIC_ENDPOINT` | Browser-facing endpoint for presigned URLs | `http://localhost:9000` |
| `MINIO_ACCESS_KEY` | MinIO access key | set locally (see `.env.example`) |
| `MINIO_SECRET_KEY` | MinIO secret key | set locally (see `.env.example`) |
| `MINIO_BUCKET` | Private bucket name | `erp-files` |
| `MINIO_REGION` | S3 region string | `us-east-1` |
| `APP_MEDIA_DIR` | Legacy local media dir (migration only) | `media` |
| `APP_UPLOAD_DIR` | Legacy local upload dir (migration only) | `media` |

### Step-by-Step Local Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/eminyuce/marble-erp-platform.git
   cd marble-erp-platform
   ```

2. **Start Infrastructure Services (Docker):**
   ```bash
   docker compose -f docker/docker-compose.yml up -d postgres minio
   ```

   MinIO API: `http://localhost:9000`  
   MinIO Console: `http://localhost:9001`

3. **Compile, Check Quality & Run Tests:**
   ```bash
   # Validate PMD 7.x rules (0 violations enforced)
   ./mvnw pmd:check

   # Run test suite with JaCoCo coverage report
   ./mvnw test
   ```

4. **Launch Application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *Access the web application at `http://localhost:81` (or `http://localhost:8080` with the local run scripts).*

The `dev` profile talks to MinIO on `localhost:9000`. When the Spring Boot `app` container runs inside Compose, set `MINIO_ENDPOINT=http://minio:9000`.

Full object-storage, backup, and filesystem-migration notes: [docs/storage.md](docs/storage.md).

---

## 🔑 Authentication & Default Credentials

Seeded via Flyway migration ([V4__add_settings_and_admin_eimece.sql](src/main/resources/db/migration/V4__add_settings_and_admin_eimece.sql)):

- **Admin Login Portal:** `http://localhost:81/account/adminlogin/`
- **Email:** `admin@eimece.test`
- **Password:** `B2u5c8JB`
- **Assigned Roles:** `ROLE_ADMIN`, `ROLE_USER`, `ROLE_EXECUTIVE`

---

## 📡 Usage & Key Endpoints

| Endpoint | Method | Access | Description |
| :--- | :--- | :--- | :--- |
| `/health/` | `GET` | Public | JSON health check returning status `UP` and port `81` |
| `/account/adminlogin/` | `GET` | Public | Glassmorphic admin sign-in portal |
| `/admin/dashboard` | `GET` | `ROLE_ADMIN` | Executive operational cockpit and KPI summaries |
| `/admin/dashboard/systemhealth/` | `GET` | `ROLE_ADMIN` | Live visual system telemetry (JVM Heap, DB ping, uptime) |
| `/admin/dashboard/oursitefeatures/`| `GET` | Authenticated | Interactive user guide and ERP pipeline visual tour |
| `/admin/settings` | `GET`/`POST` | `ROLE_ADMIN` | 2FA, reCAPTCHA, SMTP server, and Email template manager |
| `/blocks` | `GET` | Authenticated | Tabulator-powered Quarry & Block management grid |
| `/blocks/api/data` | `GET` | Authenticated | Remote paginated JSON feed for quarry blocks |
| `/production` | `GET` | Authenticated | Factory gangsaw orders and scrap registry |
| `/workshop` | `GET` | Authenticated | Custom bridge-cutting and dimensional order manager |
| `/projects` | `GET` | Authenticated | Architectural installation projects and WBS metraj trees |
| `/costs` | `GET` | `ROLE_FINANCE` | Multi-layer Activity-Based Costing & margin simulator |
| `/reports` | `GET` | Authenticated | Operational Report Center (Quarry, Factory, Site, Cost) |
| `/reports/export` | `GET` | Authenticated | Instant Excel (`.xlsx`) and CSV (`.csv`) export engine |
| `/api/upload` | `POST` | Authenticated | Upload images, SVG, PDF, Excel, Word, CSV, TXT to MinIO |
| `/api/upload/download/{id}` | `GET` | Authenticated | Stream a stored file as an attachment |
| `/api/upload/{id}/download-url` | `GET` | Authenticated | Short-lived MinIO presigned download URL |
| `/passport/{code}` | `GET` | Public | Digital Stone Passport & reverse genealogy by QR scan |

---

## 📜 Quality & Code Standards

- **Google Java Style**: Clean imports (no wildcard imports, zero fully qualified names).
- **PMD 7.x Compliance**: Proactively validated against `maven-pmd-plugin:3.28.0` with **0 failures**.
- **Dual DTO Serialization**: Full compatibility with both `snake_case` REST clients and `camelCase` data grids.
- **Zero-Allocation Logging**: Parameterized Logstash JSON output with contextual MDC propagation.

---

## 📦 File Storage (MinIO)

Uploads are stored in **MinIO** (S3 API). PostgreSQL keeps only metadata in `file_storage`. The MinIO bucket is private.

```bash
docker compose -f docker/docker-compose.yml up -d minio
```

| Service | URL |
| :--- | :--- |
| MinIO API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 |

Production Ubuntu deploys (`scripts/deploy_production.sh`) start `marble-minio` in Docker with persistent data at `MINIO_DATA_DIR` (default `/opt/minio/data`). Legacy folders `/opt/marble-erp/media` and `/opt/marble-erp/uploads` are kept only so existing files can be migrated; they are no longer the live store.

A PostgreSQL backup is not enough — back up the MinIO data directory as well. Details: [docs/storage.md](docs/storage.md).

---

## 📄 License

Copyright &copy; 2026 Özerler Mermer A.Ş. All rights reserved.  
Internal Enterprise Resource Planning Platform developed to BRD/SRS v1.0 specifications.
