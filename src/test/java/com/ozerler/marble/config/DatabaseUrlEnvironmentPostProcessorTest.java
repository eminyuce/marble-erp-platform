package com.ozerler.marble.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("Normalizes Render postgres:// URI into Spring JDBC datasource properties")
    void normalizesRenderPostgresUri() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DATABASE_URL", "postgres://marbleuser:secretpass@dpg-abc1234-a.render.com:5432/marble_erp");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-abc1234-a.render.com:5432/marble_erp");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("marbleuser");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("secretpass");
    }

    @Test
    @DisplayName("Normalizes postgresql:// URI format with default port")
    void normalizesPostgresqlUriWithoutPort() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DB_URL", "postgresql://admin:mypass@db.render.internal/marble_db");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.render.internal:5432/marble_db");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("admin");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("mypass");
    }

    @Test
    @DisplayName("Does nothing when URL is already standard JDBC format or missing")
    void leavesStandardJdbcUrlUnchanged() {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("DB_URL", "jdbc:postgresql://localhost:5432/marble_erp");

        processor.postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url")).isNull();
    }
}
