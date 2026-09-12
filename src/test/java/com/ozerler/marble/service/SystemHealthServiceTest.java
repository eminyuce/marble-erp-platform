package com.ozerler.marble.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemHealthServiceTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Environment environment;

    private SystemHealthService systemHealthService;

    @Test
    @DisplayName("collectHealthMetrics gathers JVM, database, and environmental metrics")
    void collectHealthMetrics_GathersData() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");
        when(metaData.getDatabaseProductVersion()).thenReturn("2.3");
        when(statement.execute(anyString())).thenReturn(true);

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty());

        Map<String, Object> metrics = systemHealthService.collectHealthMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.get("overallStatus")).isEqualTo("UP");
        assertThat(metrics.get("dbStatus")).isEqualTo("UP");
        assertThat(metrics.get("dbName")).isEqualTo("H2");
        assertThat(metrics.get("activeProfiles")).isEqualTo("test");
        assertThat(metrics.get("usedMemMb")).isNotNull();

        String json = systemHealthService.formatHealthJson(metrics);
        assertThat(json).contains("\"status\" : \"UP\"");
    }
}
