# Architecture standards

This is the canonical architecture and clean-code standard for Özerler Mermer ERP. Every change, regardless of the editor or assistant, follows these rules. Start at [AGENTS.md](../AGENTS.md). Related documents: [DEVELOPMENT_STANDARDS.md](DEVELOPMENT_STANDARDS.md), [UI_UX_STANDARDS.md](UI_UX_STANDARDS.md).

## Stack

The stack stays fixed: Java 24, Spring Boot 4.x (currently 4.2.0), Spring Security 7, JPA/Hibernate 7, PostgreSQL 16, Flyway 12, Testcontainers 2. The UI is server-rendered Thymeleaf, Tailwind, HTMX, and Alpine. Do not downgrade. Do not replace JDBC access with `JdbcClient`. Keep package-by-layer `com.ozerler.marble.{controller,service,repository,dto,config,storage}`.

## Rules

- Constructor injection only, `private final` fields. No field `@Autowired`.
- Controllers stay thin. Business logic and `@Transactional` live in services. Query methods use `@Transactional(readOnly = true)`.
- Domain factors such as `K_Extra = 1.30` and `K_A = 1.15` live in constants or enums. No magic numbers.
- No lazy loading from a controller or Thymeleaf. Lists use a fetch join, entity graph, or DTO projection. List queries take `Pageable`. No unbounded `findAll()`.
- Schema changes go through Flyway only. No profile may use `ddl-auto: update`.
- Timestamps are `TIMESTAMPTZ`. Money is `NUMERIC(19,4)` and `BigDecimal`. Foreign keys and indexes are explicit.
- Services do not return JPA entities. Request and response DTOs are immutable records with Jakarta Validation. JSON keeps `@JsonProperty` snake_case and `@JsonAlias` camelCase. Mapping is explicit and static. No ModelMapper.
- `/api/*` failures use RFC 7807 `ProblemDetail` from `@RestControllerAdvice` and domain exceptions. UI failures use the error view and a flash message, without a stack trace.
- Configuration is `@ConfigurationProperties` plus `@Validated` records, not scattered `@Value`. No hardcoded secrets, URLs, or ports. Logging is SLF4J with parameters, not string concatenation or `System.out`. MDC carries `traceId` and `userId` where the request already has them.
- Config stays split YAML: `application.yml` and `application-{local,dev,prod,test}.yml`. No `.properties` files and no `.env` files. Use `${VAR:default}`.
- Do not change Spring Session configuration, session Flyway migrations, or session logic.
- Docker image: multi-stage build, Temurin 24 JDK to compile, Temurin 24 JRE to run, non-root user, `-XX:MaxRAMPercentage=75.0`. Compose uses an explicit network, keeps the existing `docker_postgres_data` volume and `RUSTFS_DATA_DIR` mount, and healthchecks Postgres with `pg_isready` and the app with its health endpoint.
- Tests: JUnit 5 and AssertJ. Integration tests that need a database use PostgreSQL via Testcontainers. Every new service method that contains business logic has a test.
- Every pull request bumps `pom.xml` `<version>` by one patch step. See the Version bump section in [AGENTS.md](../AGENTS.md).
- The production Postgres volume name `docker_postgres_data` and the RustFS host path `RUSTFS_DATA_DIR` stay attached to existing data.
