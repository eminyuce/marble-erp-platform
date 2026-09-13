# Multi-stage Dockerfile for Özerler Mermer ERP
# Optimized for Render Free Tier (512 MB RAM, Container cgroups)

# Stage 1: Build
FROM eclipse-temurin:24-jdk-noble AS builder
WORKDIR /build

# Install Node.js & npm for Tailwind CSS compilation
RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs maven && \
    rm -rf /var/lib/apt/lists/*

# Copy frontend dependencies and build CSS
COPY frontend/package.json frontend/
RUN cd frontend && npm install
COPY frontend/src/ frontend/src/
RUN cd frontend && npm run build

# Copy Maven POM, wrapper and source files
COPY pom.xml .
COPY mvnw* .
COPY .mvn/ .mvn/
COPY src/ src/

# Build production JAR
RUN mvn clean package -DskipTests

# Stage 2: Production Runtime
FROM eclipse-temurin:24-jre-noble
WORKDIR /app

# Install curl for container health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create unprivileged user
RUN useradd -m -u 1001 marbleuser && \
    mkdir -p /app/uploads && \
    chown -R marbleuser:marbleuser /app

USER marbleuser

COPY --from=builder --chown=marbleuser:marbleuser /build/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=10000 \
    SERVER_PORT=10000 \
    APP_UPLOAD_DIR=/app/uploads

EXPOSE 10000 8080 81

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-10000}/health/ || exit 1

# Optimized JVM memory flags for 512 MB cgroups:
# - SerialGC eliminates multi-threaded GC memory overhead
# - Max heap capped at 320 MB, leaving 192 MB for Metaspace, CodeCache, and native memory
# - Thread stack capped at 512 KB
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--XX:+UseSerialGC -Xms128m -Xmx320m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m -Xss512k -XX:+ExitOnOutOfMemoryError} -jar app.jar"]
