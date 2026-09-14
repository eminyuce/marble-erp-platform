# Production deploy (native JAR + systemd)

> **TR:** Üretim sunucusunda `scripts/run_local.sh` veya `mvn spring-boot:run` kullanmayın. Paketlenmiş JAR’ı systemd ile çalıştırın: `./scripts/deploy_production.sh` (veya `./scripts/deploy_production.sh -y`)
>
> **EN:** Do not use `scripts/run_local.sh` or `mvn spring-boot:run` on the production host. Run the packaged JAR under systemd: `./scripts/deploy_production.sh` (or `./scripts/deploy_production.sh -y`)

This runbook is for the **bare-metal JAR** layout already in use on the Linux host (`eyuce`, `/opt/marble-erp/app.jar`). First-time OS hardening, Nginx, and Docker Compose are documented in [DEPLOYMENT_LINUX.md](../DEPLOYMENT_LINUX.md). Automated image deploys (GHCR) live in [`.github/workflows/deploy-production.yml`](../.github/workflows/deploy-production.yml). For an operational runbook, see [`DEPLOYMENT_RUNBOOK.md`](../DEPLOYMENT_RUNBOOK.md).

Local development stays in [RUNNING_LOCALLY.md](../RUNNING_LOCALLY.md) and `scripts/run_local.sh`.

---

## Why not `run_local.sh` or `spring-boot:run`

| Wrong on production | Why |
| :--- | :--- |
| `scripts/run_local.sh` | Starts **dev** profile via `./mvnw spring-boot:run`, waits on a **Docker** Postgres named `marble-erp-postgres`, and binds the Maven process to your SSH session. Killing the shell (or `sudo kill` on a random Java PID) leaves an unmanaged orphan. |
| `./mvnw spring-boot:run` / `mvn spring-boot:run` | Same problem: Maven-launched JVM, `dev` defaults, no restart policy, no rollback artifact. |
| `sudo kill <pid>` | Easy to kill the wrong process. Use `systemctl` so only the ERP service stops. |

Production must run the **already packaged** JAR with `spring.profiles.active=prod`, managed by **systemd**, so a disconnect or reboot does not leave you hunting PIDs.

---

## What you should run on the host

On the Linux host, from the **git clone**:

```bash
cd /home/eyuce/marble-erp-platform
git pull origin main
./scripts/deploy_production.sh
```

Pass `-y` to skip the interactive confirmation prompt (`./scripts/deploy_production.sh -y`).

The first run writes `/opt/marble-erp/marble-erp.env` if it is missing, then **stops** if that file still has placeholders. Edit the file (database URL, user, password) and run the same command again.

If the `marble-erp` unit is not installed yet, the script installs it (see below) and starts it.

Quick recovery without a rebuild (JAR already at `/opt/marble-erp/app.jar`):

```bash
sudo systemctl start marble-erp
sudo systemctl status marble-erp
curl -sf http://127.0.0.1:8080/health/
```

---

## Layout on the host

| Path | Role |
| :--- | :--- |
| Git clone (wherever you `git pull`) | Source, frontend, `./mvnw` |
| `/opt/marble-erp/app.jar` | Live Spring Boot fat JAR |
| `/opt/marble-erp/app.jar.bak.*` | Timestamped rollback copies |
| `/opt/marble-erp/marble-erp.env` | Production environment (not committed) |
| `/opt/marble-erp/uploads` | `APP_UPLOAD_DIR` |
| `/opt/marble-erp/logs` | Extra file logs if you add them later |
| `/etc/systemd/system/marble-erp.service` | systemd unit |

Do not store secrets in git. Use placeholders locally; put real values only in `marble-erp.env` on the server.

---

## Build (same steps as Docker / CI)

Frontend CSS is compiled into `src/main/resources/static/css/app.css` **before** Maven packages the JAR. The GitHub workflow builds this inside `docker/Dockerfile`; on the host you do it explicitly:

```bash
cd frontend && npm install && npm run build && cd ..
./mvnw clean package -DskipTests
```

Artifact: `target/marble-erp-platform-1.0.0.jar`.

`scripts/deploy_production.sh` runs exactly those two steps, then replaces `/opt/marble-erp/app.jar`.

---

## Environment / `prod` profile

`application-prod.yml` is activated only when `SPRING_PROFILES_ACTIVE=prod`. Important bindings:

| Variable | Purpose | Notes |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Must be `prod` | systemd sets this |
| `SERVER_PORT` or `PORT` | HTTP port | Host layout and CI health check use **8080**. Prod YAML default is `10000` if neither is set — always set `SERVER_PORT=8080` on this host. |
| `DB_URL` | JDBC URL | Preferred. Example: `jdbc:postgresql://127.0.0.1:5432/marble_erp` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Used if `DB_URL` is unset | Prod default host is **`postgres`** (Docker Compose DNS). Native installs **must** set `DB_HOST=127.0.0.1` or a full `DB_URL`. |
| `DB_USERNAME` | DB user | Default in YAML: `marbleuser` |
| `DB_PASSWORD` | DB password | Default in YAML is a local placeholder — set a real password in `marble-erp.env` |
| `APP_UPLOAD_DIR` | Upload directory | `/opt/marble-erp/uploads` |
| `CORS_ALLOWED_ORIGIN` | Allowed browser origin | e.g. `https://erp.example.com` |

Example `/opt/marble-erp/marble-erp.env` (placeholders only):

```bash
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_URL=jdbc:postgresql://127.0.0.1:5432/marble_erp
DB_USERNAME=marbleuser
DB_PASSWORD=CHANGE_ME
APP_UPLOAD_DIR=/opt/marble-erp/uploads
CORS_ALLOWED_ORIGIN=https://YOUR_PUBLIC_HOST
```

Restrict the file: `sudo chmod 640 /opt/marble-erp/marble-erp.env` and keep it owned by the service user.

Flyway runs on startup (`validate` + migrate). JPA `ddl-auto` is `validate` in prod — schema changes come from `src/main/resources/db/migration/`, not Hibernate.

---

## systemd unit

Service name: `marble-erp`. The deploy script installs this unit **only if it does not already exist** (it will not overwrite a hand-tuned file).

```ini
[Unit]
Description=Ozerler Mermer ERP Platform
After=network.target docker.service postgresql.service
Wants=docker.service

[Service]
Type=simple
User=eyuce
Group=eyuce
WorkingDirectory=/opt/marble-erp
EnvironmentFile=-/opt/marble-erp/marble-erp.env
Environment=SPRING_PROFILES_ACTIVE=prod
ExecStart=/usr/bin/java -server -XX:+UseZGC -XX:MaxRAMPercentage=75.0 -jar /opt/marble-erp/app.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

Adjust `User=` / `Group=` if the process should not run as `eyuce`. After any unit edit:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now marble-erp
```

---

## Stop / start / restart (no `sudo kill`)

```bash
sudo systemctl status marble-erp
sudo systemctl stop marble-erp
sudo systemctl start marble-erp
sudo systemctl restart marble-erp
```

If a leftover Maven/`java -jar` process is still holding the port and it is **not** the systemd unit:

```bash
# Inspect first — do not killall java
pgrep -af marble-erp-platform
pgrep -af /opt/marble-erp/app.jar
sudo ss -lptn 'sport = :8080'
```

Stop only the process whose command line is `/opt/marble-erp/app.jar` or the `marble-erp` unit. The deploy script does that for you.

---

## Update

```bash
cd /path/to/marble-erp-platform
git pull origin main
./scripts/deploy_production.sh --yes
```

What the script does:

1. Builds frontend CSS, then `./mvnw clean package -DskipTests`
2. Stops `marble-erp` (or the `/opt/marble-erp/app.jar` JVM if the unit is missing)
3. Copies the live JAR to `/opt/marble-erp/app.jar.bak.<timestamp>`
4. Installs `target/marble-erp-platform-1.0.0.jar` as `/opt/marble-erp/app.jar`
5. Starts (or installs + starts) the systemd unit
6. Waits for `http://127.0.0.1:8080/health/` (and `/actuator/health`)

Optional flags: `--skip-frontend` if CSS did not change; omit `--yes` to print the plan and exit.

---

## Logs

```bash
sudo journalctl -u marble-erp -f -n 100
sudo journalctl -u marble-erp --since "10 minutes ago"
```

---

## Health check

Public app probe (Security permits `/health` and `/actuator/**`):

```bash
curl -sf http://127.0.0.1:8080/health/
curl -sf http://127.0.0.1:8080/actuator/health
```

Expect JSON containing `"status":"UP"`. Admin UI telemetry (auth required): `/admin/dashboard/systemhealth/`.

If Nginx terminates TLS, also hit the public URL after local health is green.

---

## Rollback

Keep the previous JAR (the script already does this):

```bash
sudo systemctl stop marble-erp
sudo cp -a /opt/marble-erp/app.jar.bak.NEWEST /opt/marble-erp/app.jar
sudo systemctl start marble-erp
curl -sf http://127.0.0.1:8080/health/
```

Replace `app.jar.bak.NEWEST` with the timestamped file you want (`ls -lt /opt/marble-erp/app.jar.bak.*`).

Database: Flyway migrations are **forward**. Rolling back the JAR does not undo SQL. Restore a Postgres dump if a migration is incompatible with the older JAR (see backup notes in [DEPLOYMENT_LINUX.md](../DEPLOYMENT_LINUX.md) §6.1).

---

## Docker / CI path (do not mix blindly)

- **This host (current):** native JAR + systemd + `/opt/marble-erp/app.jar`.
- **Repo CI:** push to `main` builds `docker/Dockerfile` and SSHs to the VPS to `docker compose up` and probe `http://localhost:8080/actuator/health`.
- **Compose file in git:** `docker/docker-compose.yml` publishes the app on port **81** inside a compose network whose DB host is `postgres`.

Do not run `run_local.sh`, `docker compose up` for the app, **and** systemd `java -jar` on port 8080 at the same time. Pick one process manager.

---

## Safety

- `scripts/deploy_production.sh` uses `set -euo pipefail`, requires Linux, requires `--yes` to change the host, and refuses default `DEPLOY_DIR` overrides that look accidental.
- It never runs `killall java` and never calls `spring-boot:run`.
- It will not overwrite an existing systemd unit or an existing `marble-erp.env`.
- It does not skip Git hooks; do not add `--no-verify` when committing deploy docs or scripts.
