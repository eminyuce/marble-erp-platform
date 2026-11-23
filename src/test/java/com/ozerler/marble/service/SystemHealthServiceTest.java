package com.ozerler.marble.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.storage.ObjectStorageService;

import javax.sql.DataSource;

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
    @DisplayName("collectHealthMetrics gathers JVM, database, environmental, and RustFS metrics")
    void collectHealthMetricsGathersData() throws Exception {
        stubHealthyDatabase();
        Mockito.when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        Mockito.when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());
        Mockito.when(objectStorageService.getBucketName()).thenReturn("erp-files");

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        Map<String, Object> metrics = systemHealthService.collectHealthMetrics();

        Assertions.assertThat(metrics).isNotNull();
        Assertions.assertThat(metrics.get("overallStatus")).isEqualTo("UP");
        Assertions.assertThat(metrics.get("dbStatus")).isEqualTo("UP");
        Assertions.assertThat(metrics.get("rustfsStatus")).isEqualTo("UP");
        Assertions.assertThat(metrics.get("rustfsBucket")).isEqualTo("erp-files");
        Assertions.assertThat(metrics.get("dbName")).isEqualTo("H2");
        Assertions.assertThat(metrics.get("activeProfiles")).isEqualTo("test");
        Assertions.assertThat(metrics.get("usedMemMb")).isNotNull();
        Assertions.assertThat(componentNames(metrics)).anyMatch(name -> name.contains("RustFS"));

        String json = systemHealthService.formatHealthJson(metrics);
        Assertions.assertThat(json).contains("\"status\" : \"UP\"");
        Assertions.assertThat(json).contains("\"rustfs\"");
    }

    @Test
    @DisplayName("collectHealthMetrics returns overallStatus DOWN when RustFS is unreachable")
    void collectHealthMetricsRustFsDownSetsOverallStatusDown() throws Exception {
        stubHealthyDatabase();
        Mockito.when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});
        Mockito.when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.down("RustFS erişilemiyor"));
        Mockito.when(objectStorageService.getBucketName()).thenReturn("erp-files");

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        Map<String, Object> metrics = systemHealthService.collectHealthMetrics();

        Assertions.assertThat(metrics.get("overallStatus")).isEqualTo("DOWN");
        Assertions.assertThat(metrics.get("rustfsStatus")).isEqualTo("DOWN");
        Assertions.assertThat(metrics.get("upComponentCount")).isEqualTo(8);
        Assertions.assertThat(metrics.get("componentCount")).isEqualTo(9);
        String json = systemHealthService.formatHealthJson(metrics);
        Assertions.assertThat(json).contains("\"status\" : \"DOWN\"");
        Assertions.assertThat(json).contains("RustFS erişilemiyor");
    }

    @Test
    @DisplayName("buildHealthResponse returns UP when database and RustFS are healthy")
    void buildHealthResponseAllDependenciesUp() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(statement.execute(ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        var health = systemHealthService.buildHealthResponse();

        Assertions.assertThat(health.getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getDependencies()).containsKeys("database", "rustfs", "quarryService", "factoryService", "costAccounting", "diskSpace");
        Assertions.assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getDependencies().get("database").getError()).isNull();
        Assertions.assertThat(health.getDependencies().get("rustfs").getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getDependencies().get("rustfs").getError()).isNull();
        Assertions.assertThat(health.getDependencies().get("quarryService").getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getDependencies().get("diskSpace").getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("buildHealthResponse returns DOWN when RustFS is unreachable")
    void buildHealthResponseRustFsDown() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(statement.execute(ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.down("RustFS erişilemiyor"));

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        var health = systemHealthService.buildHealthResponse();

        Assertions.assertThat(health.getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getDependencies().get("rustfs").getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getDependencies().get("rustfs").getError()).isEqualTo("RustFS erişilemiyor");
        Assertions.assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("buildHealthResponse returns DOWN when database is unreachable")
    void buildHealthResponseDatabaseDown() throws Exception {
        Mockito.when(dataSource.getConnection()).thenThrow(new RuntimeException("Connection refused"));
        Mockito.when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());

        systemHealthService = new SystemHealthService(dataSource, environment, Optional.empty(), null, objectStorageService);

        var health = systemHealthService.buildHealthResponse();

        Assertions.assertThat(health.getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getDependencies().get("database").getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getDependencies().get("database").getError()).isEqualTo("Database connection failed");
        Assertions.assertThat(health.getDependencies().get("rustfs").getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getDependencies().get("quarryService").getStatus()).isEqualTo("UP");
        Assertions.assertThat(health.getDependencies().get("quarryService").getError()).isNull();
    }

    private void stubHealthyDatabase() throws Exception {
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        DatabaseMetaData metaData = Mockito.mock(DatabaseMetaData.class);

        Mockito.when(dataSource.getConnection()).thenReturn(connection);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(connection.getMetaData()).thenReturn(metaData);
        Mockito.when(metaData.getDatabaseProductName()).thenReturn("H2");
        Mockito.when(metaData.getDatabaseProductVersion()).thenReturn("2.3");
        Mockito.when(statement.execute(ArgumentMatchers.anyString())).thenReturn(true);
    }

    @SuppressWarnings("unchecked")
    private static List<String> componentNames(Map<String, Object> metrics) {
        List<Map<String, Object>> components = (List<Map<String, Object>>) metrics.get("components");
        return components.stream().map(component -> String.valueOf(component.get("name"))).toList();
    }
}
