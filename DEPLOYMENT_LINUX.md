# Özerler Mermer ERP - Linux Server Production Deployment Guide

This guide describes how to deploy the Özerler Mermer ERP platform to a production Linux server (Ubuntu 24.04 LTS, Debian 12, or RHEL/Rocky Linux 9).

---

## 1. Production Architecture Overview

```
 [Clients / Browsers]
          │ (HTTPS :443)
          ▼
    [Nginx Reverse Proxy & SSL Termination]
          │ (HTTP :8080 or :81)
          ▼
 [Özerler Mermer ERP (Spring Boot 4 / Java 24)]
          │ (TCP :3306)
          ▼
   [MySQL 8.4 Database Server]
```

### Key Highlights
- **Java Runtime**: OpenJDK 24 with Z Garbage Collector (`-XX:+UseZGC`).
- **Web Server**: Nginx handling SSL (Let's Encrypt), static compression, rate limiting, and request routing.
- **Data Persistence**: MySQL 8.4 UTF-8 (`utf8mb4_unicode_ci`) with automatic Flyway schema migrations on boot.
- **Localization**: System defaults to Turkish (`tr`) with externalized message bundles.

---

## 2. Server Preparation & Hardening

### Step 2.1: Update System Packages
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl wget git ufw certbot python3-certbot-nginx
```

### Step 2.2: Configure Firewall (UFW)
Only expose necessary ports:
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP (Let's Encrypt verification)
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

---

## 3. Deployment Method 1: Docker Compose *(Recommended)*

Using Docker ensures consistency across environments and isolates dependencies.

### Step 3.1: Install Docker & Docker Compose
```bash
# Add Docker's official GPG key & repository
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Enable Docker on startup
sudo systemctl enable --now docker
```

### Step 3.2: Clone Repository & Configure Environment
```bash
# Clone to production directory
sudo mkdir -p /opt/marble-erp
sudo chown -R $USER:$USER /opt/marble-erp
git clone https://github.com/eminyuce/marble-erp-platform.git /opt/marble-erp
cd /opt/marble-erp

# Create production environment variables file
cat << 'EOF' > /opt/marble-erp/.env
DB_NAME=marble_erp
DB_USERNAME=marbleuser
DB_PASSWORD=YOUR_STRONG_DB_PASSWORD_HERE
DB_ROOT_PASSWORD=YOUR_STRONG_ROOT_PASSWORD_HERE
APP_PORT=8080
CORS_ALLOWED_ORIGIN=https://erp.ozerlermermer.com
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
EOF
```
> [!WARNING]
> Replace `YOUR_STRONG_DB_PASSWORD_HERE` and `YOUR_STRONG_ROOT_PASSWORD_HERE` with secure generated passwords.

### Step 3.3: Build & Launch Containers
```bash
# Build multi-stage image (compiles Tailwind CSS & Spring Boot JAR) and start services
docker compose -f docker/docker-compose.yml --env-file .env up -d --build

# Verify container statuses
docker compose -f docker/docker-compose.yml ps

# Check logs
docker compose -f docker/docker-compose.yml logs -f app
```

---

## 4. Deployment Method 2: Standalone Native Systemd Service

If you prefer running directly on the bare-metal Linux host without Docker:

### Step 4.1: Install OpenJDK 24 & MySQL 8.4
```bash
# Install OpenJDK 24 (via Eclipse Temurin)
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /etc/apt/keyrings/adoptium.asc
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-24-jdk mysql-server

# Secure MySQL installation
sudo mysql_secure_installation
```

### Step 4.2: Initialize Production Database
```bash
sudo mysql -u root -p
```
Run within MySQL console:
```sql
CREATE DATABASE marble_erp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'marbleuser'@'127.0.0.1' IDENTIFIED BY 'YOUR_STRONG_DB_PASSWORD_HERE';
GRANT ALL PRIVILEGES ON marble_erp.* TO 'marbleuser'@'127.0.0.1';
FLUSH PRIVILEGES;
EXIT;
```

### Step 4.3: Create System User & Application Directory
```bash
sudo useradd -r -s /bin/false -m -d /opt/marble-erp marble
sudo mkdir -p /opt/marble-erp/uploads /opt/marble-erp/logs
sudo chown -R marble:marble /opt/marble-erp
```

### Step 4.4: Build and Deploy JAR
On your CI/CD runner or build machine:
```bash
# Compile frontend assets
cd frontend && npm install && npm run build && cd ..

# Package JAR
./mvnw clean package -DskipTests
```
Copy `target/marble-erp-platform-1.0.0.jar` to `/opt/marble-erp/app.jar` on the server:
```bash
sudo cp target/marble-erp-platform-1.0.0.jar /opt/marble-erp/app.jar
sudo chown marble:marble /opt/marble-erp/app.jar
```

### Step 4.5: Create Systemd Service Unit
Create `/etc/systemd/system/marble-erp.service`:
```ini
[Unit]
Description=Özerler Mermer ERP Platform
After=syslog.target network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=marble
Group=marble
WorkingDirectory=/opt/marble-erp

Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="SERVER_PORT=8080"
Environment="DB_URL=jdbc:mysql://127.0.0.1:3306/marble_erp?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci"
Environment="DB_USERNAME=marbleuser"
Environment="DB_PASSWORD=YOUR_STRONG_DB_PASSWORD_HERE"
Environment="APP_UPLOAD_DIR=/opt/marble-erp/uploads"

ExecStart=/usr/bin/java \
    -server \
    -XX:+UseZGC \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -jar /opt/marble-erp/app.jar

Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

Reload and start service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now marble-erp
sudo systemctl status marble-erp
```

---

## 5. Nginx Reverse Proxy & SSL Configuration

### Step 5.1: Create Nginx Site Configuration
Create `/etc/nginx/sites-available/marble-erp.conf`:
```nginx
server {
    listen 80;
    server_name erp.ozerlermermer.com; # Replace with your domain

    # Redirect all HTTP traffic to HTTPS
    location / {
        return 301 https://$host$request_uri;
    }

    # Let's Encrypt ACME Challenge
    location /.well-known/acme-challenge/ {
        root /var/www/html;
    }
}

server {
    listen 443 ssl http2;
    server_name erp.ozerlermermer.com; # Replace with your domain

    # SSL certificates will be configured by Certbot
    # ssl_certificate /etc/letsencrypt/live/erp.ozerlermermer.com/fullchain.pem;
    # ssl_certificate_key /etc/letsencrypt/live/erp.ozerlermermer.com/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;

    # Maximum file upload size (matches Spring Boot multipart limit)
    client_max_body_size 50M;

    # Proxy Headers
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Port $server_port;

    # Support HTMX & WebSocket streaming
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    # Timeouts
    proxy_connect_timeout 60s;
    proxy_send_timeout 120s;
    proxy_read_timeout 120s;

    # Gzip Compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml image/svg+xml;
    gzip_min_length 1024;

    # Application Proxy
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_redirect off;
    }

    # Static Asset Caching
    location ~* \.(ico|css|js|gif|jpe?g|png|woff2?|eot|ttf|svg)$ {
        proxy_pass http://127.0.0.1:8080;
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }
}
```

Enable site and test configuration:
```bash
sudo ln -s /etc/nginx/sites-available/marble-erp.conf /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### Step 5.2: Obtain Free Let's Encrypt SSL Certificate
```bash
sudo certbot --nginx -d erp.ozerlermermer.com
```
Certbot will automatically update the Nginx configuration with valid SSL certificates and configure auto-renewal via cron/systemd timer.

---

## 6. Production Maintenance & Operations

### 6.1: Automated Database Backups (Cron)
Create `/usr/local/bin/backup-marble-db.sh`:
```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="/var/backups/marble-erp"
DATE="$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Dump database
mysqldump -u marbleuser -p'YOUR_STRONG_DB_PASSWORD_HERE' --single-transaction --quick --routines marble_erp | gzip > "$BACKUP_DIR/marble_erp_$DATE.sql.gz"

# Retain only last 14 days of backups
find "$BACKUP_DIR" -type f -name "marble_erp_*.sql.gz" -mtime +14 -delete
```
Make executable and schedule daily at 02:00 AM:
```bash
sudo chmod +x /usr/local/bin/backup-marble-db.sh
(crontab -l 2>/dev/null; echo "0 2 * * * /usr/local/bin/backup-marble-db.sh") | crontab -
```

### 6.2: Inspecting Live Application Logs
- **Docker**:
  ```bash
  docker compose -f /opt/marble-erp/docker/docker-compose.yml logs -f --tail=100 app
  ```
- **Systemd**:
  ```bash
  journalctl -u marble-erp -f -n 100
  ```

### 6.3: Updating to a New Release
```bash
cd /opt/marble-erp
git pull origin main

# If using Docker:
docker compose -f docker/docker-compose.yml up -d --build

# If using Systemd:
cd frontend && npm run build && cd ..
./mvnw clean package -DskipTests
sudo systemctl restart marble-erp
```
Flyway will automatically execute any newly added migration scripts (`V9__...sql`, etc.) on restart.
