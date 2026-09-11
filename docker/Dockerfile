# Multi-stage Dockerfile for Özerler Mermer ERP

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

# Copy Maven POM and source files
COPY pom.xml .
COPY src/ src/

# Build production JAR
RUN mvn clean package -DskipTests

# Stage 2: Production Runtime
FROM eclipse-temurin:24-jre-noble
WORKDIR /app

# Create unprivileged user
RUN useradd -m -u 1001 marbleuser && \
    mkdir -p /app/uploads && \
    chown -R marbleuser:marbleuser /app

USER marbleuser

COPY --from=builder --chown=marbleuser:marbleuser /build/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=81 \
    APP_UPLOAD_DIR=/app/uploads

EXPOSE 81

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:81/health/ || exit 1

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
