package com.ozerler.marble.service;

import com.ozerler.marble.dto.DependencyHealth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @Mock
    private FileStorageService fileStorageService;

    private SystemHealthService systemHealthService;

    @Test
    @DisplayName("collectHealthMetrics gathers JVM, database, environmental, and media storage metrics")
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
        when(fileStorageService.checkStorageHealth()).thenReturn(DependencyHealth.up());

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, fileStorageService);

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

    @Test
    @DisplayName("collectHealthMetrics returns overallStatus DOWN when mediaStorage has no write access")
    void collectHealthMetrics_MediaStorageDown_SetsOverallStatusDown() throws Exception {
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
        when(fileStorageService.checkStorageHealth()).thenReturn(
                DependencyHealth.down("Görseller klasöründe (media/images) yazma izni yok"));

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, fileStorageService);

        Map<String, Object> metrics = systemHealthService.collectHealthMetrics();

        assertThat(metrics.get("overallStatus")).isEqualTo("DOWN");
        String json = systemHealthService.formatHealthJson(metrics);
        assertThat(json).contains("\"status\" : \"DOWN\"");
        assertThat(json).contains("Görseller klasöründe (media/images) yazma izni yok");
    }

    @Test
    @DisplayName("buildHealthResponse returns UP when database and media storage are healthy")
    void buildHealthResponse_AllDependenciesUp() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(fileStorageService.checkStorageHealth()).thenReturn(DependencyHealth.up());

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, fileStorageService);

        var health = systemHealthService.buildHealthResponse();

        assertThat(health.getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies()).containsKeys("database", "quarryService", "factoryService", "costAccounting", "diskSpace", "mediaStorage");
        assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("database").getError()).isNull();
        assertThat(health.getDependencies().get("mediaStorage").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("mediaStorage").getError()).isNull();
        assertThat(health.getDependencies().get("quarryService").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("diskSpace").getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("buildHealthResponse returns DOWN when media storage directories are not accessible")
    void buildHealthResponse_MediaStorageDown() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(fileStorageService.checkStorageHealth()).thenReturn(
                DependencyHealth.down("Belgeler klasörüne (media/documents) yazma testi başarısız: Permission denied"));

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, fileStorageService);

        var health = systemHealthService.buildHealthResponse();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("mediaStorage").getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("mediaStorage").getError())
                .contains("media/documents")
                .contains("Permission denied");
    }

    @Test
    @DisplayName("buildHealthResponse returns DOWN when database is unreachable")
    void buildHealthResponse_DatabaseDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));
        when(fileStorageService.checkStorageHealth()).thenReturn(DependencyHealth.up());

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, fileStorageService);

        var health = systemHealthService.buildHealthResponse();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("database").getError()).isEqualTo("Database connection failed");
        assertThat(health.getDependencies().get("quarryService").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("quarryService").getError()).isNull();
    }
}
