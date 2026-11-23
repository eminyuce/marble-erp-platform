# Agent instructions

## Development standards

Every change must follow [docs/DEVELOPMENT_STANDARDS.md](docs/DEVELOPMENT_STANDARDS.md). The 12 mandatory rules and the per-feature checklist in that document are required for UI, API, and data changes.

## Version bump

Every pull request bumps the application version by one patch step.

- Source of truth: `pom.xml` `<version>` (`major.minor.patch`).
- One step means patch only: `1.0.0` becomes `1.0.1`, `1.0.5` becomes `1.0.6`.
- Also set `app.version` in `src/main/resources/application.yml` to that same value.
- Do not invent another version in Java, workflows, or docs. Image builds read `pom.xml`. Runtime version comes from Spring Boot `build-info` (`/actuator/info`, system health, deployment console).
- Bump once per pull request, not once per commit.
- `.github/workflows/version-bump.yml` rejects a PR to `main` whose `pom.xml` version is not greater than `main`.

The same rule is enforced for Cursor agents in `.cursor/rules/version-bump.mdc`.

## Architecture

The full rules are in `.cursor/rules/architecture.mdc`. In short:

- Stay on Java 24, Spring Boot 4.2.x, Security 7, Hibernate 7, PostgreSQL 16, Flyway 12, and Testcontainers 2. Do not downgrade. UI stays server-rendered Thymeleaf, Tailwind, HTMX, and Alpine.
- Package by layer under `com.ozerler.marble`. Constructor injection only. Business logic and transactions stay in services.
- Schema changes are Flyway only. Money is `BigDecimal`. Do not return JPA entities from services.
- YAML profiles only. No `.env` and no `.properties`. Do not change Spring Session.
- The production Postgres volume name `docker_postgres_data` and the RustFS host path `RUSTFS_DATA_DIR` stay attached to existing data.
