# syntax=docker/dockerfile:1

# Marble ERP production image.
# GitHub Actions builds this file and pushes it to Docker Hub.
# The server does not compile anything; it only pulls and runs the image.

FROM node:22-bookworm-slim AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/src ./src
COPY src/main/resources/templates /src/src/main/resources/templates
COPY src/main/resources/static/js /src/src/main/resources/static/js
RUN npm run build

FROM eclipse-temurin:24-jdk-noble AS build
WORKDIR /build

# The repo mvnw script only execs a Maven already on PATH. The JDK image has none.
# Pin the same distribution declared in .mvn/wrapper/maven-wrapper.properties.
ARG MAVEN_VERSION=3.9.9
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/* \
    && curl -fsSL -o /tmp/maven.tgz "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    && tar -xzf /tmp/maven.tgz -C /opt \
    && ln -s "/opt/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn \
    && rm -f /tmp/maven.tgz

COPY pom.xml .
RUN mvn -B -DskipTests dependency:resolve
COPY src ./src
COPY --from=frontend /src/src/main/resources/static/css/app.css src/main/resources/static/css/app.css
RUN mvn -B -DskipTests package

FROM eclipse-temurin:24-jre-noble AS runtime
WORKDIR /app

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates \
      curl \
      postgresql-client \
    && groupadd --system --gid 1001 marble \
    && useradd --system --uid 1001 --gid 1001 --create-home --home-dir /home/marble marble \
    && mkdir -p /app/uploads /app/media \
    && chown -R 1001:1001 /app

# Static docker client so the admin page can start a rollout container.
# The client is glibc-compatible; the Alpine docker CLI image is not copied in.
ARG TARGETARCH=amd64
ARG DOCKER_CLI_VERSION=27.5.1
RUN arch="x86_64" \
    && if [ "$TARGETARCH" = "arm64" ]; then arch="aarch64"; fi \
    && curl -fsSL -o /tmp/docker.tgz "https://download.docker.com/linux/static/stable/${arch}/docker-${DOCKER_CLI_VERSION}.tgz" \
    && tar -xzf /tmp/docker.tgz -C /tmp \
    && mv /tmp/docker/docker /usr/local/bin/docker \
    && chmod 755 /usr/local/bin/docker \
    && rm -rf /tmp/docker /tmp/docker.tgz

COPY --chmod=755 scripts/deploy_from_app.sh /usr/local/sbin/marble-erp-inapp-deploy
COPY docker-compose.prod.yml /stack/docker-compose.prod.yml
COPY --from=build --chown=1001:1001 --link /build/target/marble-erp-platform-*.jar /app/app.jar

LABEL org.opencontainers.image.title="Marble ERP Platform" \
      org.opencontainers.image.source="https://github.com/eminyuce/marble-erp-platform" \
      org.opencontainers.image.description="Ozerler Mermer ERP"

USER 1001:1001

# application-prod.yml uses PORT when it is set, otherwise SERVER_PORT.
# Do not set PORT here; Compose pins both so the process listens on 81.
ENV SERVER_PORT=81 \
    SPRING_PROFILES_ACTIVE=prod \
    APP_UPLOAD_DIR=/app/uploads \
    APP_MEDIA_DIR=/app/media \
    JAVA_OPTS="-XX:+UseSerialGC -Xms256m -Xmx768m -XX:MaxMetaspaceSize=192m -XX:+ExitOnOutOfMemoryError"

EXPOSE 81

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -fsS http://127.0.0.1:81/health/ || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
