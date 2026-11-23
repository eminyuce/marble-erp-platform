# Agent instructions

## Version bump

Every pull request bumps the application version by one patch step.

- Source of truth: `pom.xml` `<version>` (`major.minor.patch`).
- One step means patch only: `1.0.0` becomes `1.0.1`, `1.0.5` becomes `1.0.6`.
- Also set `app.version` in `src/main/resources/application.yml` to that same value.
- Do not invent another version in Java, workflows, or docs. Image builds read `pom.xml`. Runtime version comes from Spring Boot `build-info` (`/actuator/info`, system health, deployment console).
- Bump once per pull request, not once per commit.
- `.github/workflows/version-bump.yml` rejects a PR to `main` whose `pom.xml` version is not greater than `main`.

The same rule is enforced for Cursor agents in `.cursor/rules/version-bump.mdc`.
