package com.ozerler.marble.service;

import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.storage.ObjectStorageService;
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
import java.util.List;
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
    private ObjectStorageService objectStorageService;

    private SystemHealthService systemHealthService;

    @Test
    @DisplayName("collectHealthMetrics gathers JVM, database, environmental, and MinIO metrics")
    void collectHealthMetrics_GathersData() throws Exception {
        stubHealthyDatabase();
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());
        when(objectStorageService.getBucketName()).thenReturn("erp-files");

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        Map<String, Object> metrics = systemHealthService.collectHealthMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.get("overallStatus")).isEqualTo("UP");
        assertThat(metrics.get("dbStatus")).isEqualTo("UP");
        assertThat(metrics.get("minioStatus")).isEqualTo("UP");
        assertThat(metrics.get("minioBucket")).isEqualTo("erp-files");
        assertThat(metrics.get("dbName")).isEqualTo("H2");
        assertThat(metrics.get("activeProfiles")).isEqualTo("test");
        assertThat(metrics.get("usedMemMb")).isNotNull();
        assertThat(componentNames(metrics)).anyMatch(name -> name.contains("MinIO"));

        String json = systemHealthService.formatHealthJson(metrics);
        assertThat(json).contains("\"status\" : \"UP\"");
        assertThat(json).contains("\"minio\"");
    }

    @Test
    @DisplayName("collectHealthMetrics returns overallStatus DOWN when MinIO is unreachable")
    void collectHealthMetrics_MinioDown_SetsOverallStatusDown() throws Exception {
        stubHealthyDatabase();
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.down("MinIO erişilemiyor"));
        when(objectStorageService.getBucketName()).thenReturn("erp-files");

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        Map<String, Object> metrics = systemHealthService.collectHealthMetrics();

        assertThat(metrics.get("overallStatus")).isEqualTo("DOWN");
        assertThat(metrics.get("minioStatus")).isEqualTo("DOWN");
        assertThat(metrics.get("upComponentCount")).isEqualTo(8);
        assertThat(metrics.get("componentCount")).isEqualTo(9);
        String json = systemHealthService.formatHealthJson(metrics);
        assertThat(json).contains("\"status\" : \"DOWN\"");
        assertThat(json).contains("MinIO erişilemiyor");
    }

    @Test
    @DisplayName("buildHealthResponse returns UP when database and MinIO are healthy")
    void buildHealthResponse_AllDependenciesUp() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        var health = systemHealthService.buildHealthResponse();

        assertThat(health.getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies()).containsKeys("database", "minio", "quarryService", "factoryService", "costAccounting", "diskSpace");
        assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("database").getError()).isNull();
        assertThat(health.getDependencies().get("minio").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("minio").getError()).isNull();
        assertThat(health.getDependencies().get("quarryService").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("diskSpace").getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("buildHealthResponse returns DOWN when MinIO is unreachable")
    void buildHealthResponse_MinioDown() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenReturn(true);
        when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.down("MinIO erişilemiyor"));

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        var health = systemHealthService.buildHealthResponse();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("minio").getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("minio").getError()).isEqualTo("MinIO erişilemiyor");
        assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("buildHealthResponse returns DOWN when database is unreachable")
    void buildHealthResponse_DatabaseDown() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));
        when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        var health = systemHealthService.buildHealthResponse();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("DOWN");
        assertThat(health.getDependencies().get("database").getError()).isEqualTo("Database connection failed");
        assertThat(health.getDependencies().get("minio").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("quarryService").getStatus()).isEqualTo("UP");
        assertThat(health.getDependencies().get("quarryService").getError()).isNull();
    }

    private void stubHealthyDatabase() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");
        when(metaData.getDatabaseProductVersion()).thenReturn("2.3");
        when(statement.execute(anyString())).thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    private static List<String> componentNames(Map<String, Object> metrics) {
        List<Map<String, Object>> components = (List<Map<String, Object>>) metrics.get("components");
        return components.stream().map(component -> String.valueOf(component.get("name"))).toList();
    }
}
