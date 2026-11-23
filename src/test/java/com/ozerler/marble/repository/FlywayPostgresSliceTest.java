package com.ozerler.marble.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class FlywayPostgresSliceTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.session.jdbc.initialize-schema", () -> "never");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway applies the versioned scripts and the widened quantity scale")
    void flywayAppliesMigrations() {
        Integer latest = jdbc.queryForObject(
                "select max(version::int) from flyway_schema_history where version is not null",
                Integer.class);
        assertThat(latest).isGreaterThanOrEqualTo(38);

        Integer quantityScale = jdbc.queryForObject(
                "select numeric_scale from information_schema.columns "
                        + "where table_name = 'sales_order_items' and column_name = 'quantity'",
                Integer.class);
        assertThat(quantityScale).isEqualTo(4);

        Integer areaScale = jdbc.queryForObject(
                "select numeric_scale from information_schema.columns "
                        + "where table_name = 'project_locations' and column_name = 'planned_area_m2'",
                Integer.class);
        assertThat(areaScale).isEqualTo(4);

        Integer versionType = jdbc.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_name = 'invoices' and column_name = 'version'",
                Integer.class);
        assertThat(versionType).isEqualTo(1);
    }
}
