# Agent instructions

These rules are tool-agnostic. Cursor, Claude, GitHub Copilot, and any other assistant follow the same documents. This file is the entry point; it does not replace the linked standards.

- Development standards (12 mandatory rules and the feature checklist): [docs/DEVELOPMENT_STANDARDS.md](docs/DEVELOPMENT_STANDARDS.md)
- Architecture and clean-code rules: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- UI/UX design system and page inventory: [docs/UI_UX_STANDARDS.md](docs/UI_UX_STANDARDS.md)

## Development standards

Every change must follow [docs/DEVELOPMENT_STANDARDS.md](docs/DEVELOPMENT_STANDARDS.md). The 12 mandatory rules and the per-feature checklist in that document are required for UI, API, and data changes.

## Version bump

Every pull request bumps the application version by one patch step.

- Source of truth: `pom.xml` `<version>` (`major.minor.patch`).
- Read the `pom.xml` `<version>` on the PR's base branch (`main` unless another base is named). Increase the patch component by exactly one. `1.0.0` becomes `1.0.1`, `1.0.5` becomes `1.0.6`. Do not skip numbers and do not bump major or minor unless a breaking or feature release is explicitly requested.
- Set `<project><version>` in `pom.xml` to that new value. Do not change the Spring Boot parent version for this rule.
- Also set `app.version` in `src/main/resources/application.yml` to that same value.
- Do not invent another version in Java, workflows, or docs, and do not hardcode the new version anywhere else. Image builds and Docker/GitLab tags read `pom.xml`. Runtime version comes from Spring Boot `build-info` (`/actuator/info`, system health, deployment console), which the `spring-boot-maven-plugin` `build-info` goal generates from `pom.xml`.
- Bump once per pull request, not once per commit. If this branch already bumped the version once for its PR, do not bump it again on later commits to the same PR.
- `.github/workflows/version-bump.yml` rejects a PR to `main` whose `pom.xml` version is not greater than `main`.

## Architecture

The full rules are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). In short:

- Stay on Java 24, Spring Boot 4.2.x, Security 7, Hibernate 7, PostgreSQL 16, Flyway 12, and Testcontainers 2. Do not downgrade. UI stays server-rendered Thymeleaf, Tailwind, HTMX, and Alpine.
- Package by layer under `com.ozerler.marble`. Constructor injection only. Business logic and transactions stay in services.
- Schema changes are Flyway only. Money is `BigDecimal`. Do not return JPA entities from services.
- YAML profiles only. No `.env` and no `.properties`. Do not change Spring Session.
- The production Postgres volume name `docker_postgres_data` and the RustFS host path `RUSTFS_DATA_DIR` stay attached to existing data.

## UI/UX

User-visible text is Turkish. New and changed screens follow [docs/UI_UX_STANDARDS.md](docs/UI_UX_STANDARDS.md): shared page layout, the shared DataGrid for interactive business lists, and the documented tokens and components. Do not invent a second design language or a second grid.
