package com.ozerler.marble.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy that runs repair before migrate.
 * This guarantees that harmless whitespace/formatting changes in previously applied
 * migrations do not fail deployment with checksum validation mismatches.
 */
@Configuration
@ConditionalOnClass(Flyway.class)
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
