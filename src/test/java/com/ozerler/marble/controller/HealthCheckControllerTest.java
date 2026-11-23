package com.ozerler.marble.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.dto.HealthResponse;
import com.ozerler.marble.service.SystemHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckControllerTest {

    @Mock
    private SystemHealthService systemHealthService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        HealthCheckController controller = new HealthCheckController(systemHealthService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("All dependencies UP returns HTTP 200 and overall UP")
    void allDependenciesUp_returnsOk() throws Exception {
        when(systemHealthService.buildHealthResponse()).thenReturn(allUpHealthResponse());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.dependencies").doesNotExist())
                .andExpect(jsonPath("$.response").doesNotExist())
                .andExpect(jsonPath("$.serviceStatus").doesNotExist())
                .andExpect(jsonPath("$.port").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    @DisplayName("Database DOWN returns HTTP 503 with database error")
    void databaseDown_returnsServiceUnavailable() throws Exception {
        Map<String, DependencyHealth> dependencies = healthyDependencies();
        dependencies.put("database", DependencyHealth.down("Database connection failed"));

        when(systemHealthService.buildHealthResponse()).thenReturn(
                HealthResponse.builder().status("DOWN").dependencies(dependencies).build());

        mockMvc.perform(get("/health/"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.dependencies").doesNotExist());
    }

    @Test
    @DisplayName("External service DOWN returns HTTP 503 with error only for failed dependency")
    void externalServiceDown_returnsServiceUnavailableWithTargetedError() throws Exception {
        Map<String, DependencyHealth> dependencies = healthyDependencies();
        dependencies.put("quarryService", DependencyHealth.down("Unable to connect to Quarry Service"));

        when(systemHealthService.buildHealthResponse()).thenReturn(
                HealthResponse.builder().status("DOWN").dependencies(dependencies).build());

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.dependencies").doesNotExist());
    }

    @Test
    @DisplayName("RustFS DOWN returns HTTP 503 and overall DOWN")
    void rustfsDown_returnsServiceUnavailable() throws Exception {
        Map<String, DependencyHealth> dependencies = healthyDependencies();
        dependencies.put("rustfs", DependencyHealth.down("RustFS erişilemiyor"));

        when(systemHealthService.buildHealthResponse()).thenReturn(
                HealthResponse.builder().status("DOWN").dependencies(dependencies).build());

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.dependencies").doesNotExist());
    }

    @Test
    @DisplayName("Multiple dependencies DOWN include respective errors")
    void multipleDependenciesDown_includeAllErrors() throws Exception {
        Map<String, DependencyHealth> dependencies = new LinkedHashMap<>();
        dependencies.put("database", DependencyHealth.up());
        dependencies.put("rustfs", DependencyHealth.up());
        dependencies.put("quarryService", DependencyHealth.down("Unable to connect to Quarry Service"));
        dependencies.put("factoryService", DependencyHealth.up());
        dependencies.put("costAccounting", DependencyHealth.down("Connection timeout"));
        dependencies.put("diskSpace", DependencyHealth.up());

        when(systemHealthService.buildHealthResponse()).thenReturn(
                HealthResponse.builder().status("DOWN").dependencies(dependencies).build());

        mockMvc.perform(get("/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.dependencies").doesNotExist());
    }

    @Test
    @DisplayName("Response JSON matches minimal health structure")
    void responseJsonStructure_isMinimal() throws Exception {
        when(systemHealthService.buildHealthResponse()).thenReturn(allUpHealthResponse());

        String json = mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var tree = objectMapper.readTree(json);
        org.assertj.core.api.Assertions.assertThat(tree.fieldNames())
                .toIterable()
                .containsExactly("status");
    }

    private HealthResponse allUpHealthResponse() {
        return HealthResponse.builder()
                .status("UP")
                .dependencies(healthyDependencies())
                .build();
    }

    private Map<String, DependencyHealth> healthyDependencies() {
        Map<String, DependencyHealth> dependencies = new LinkedHashMap<>();
        dependencies.put("database", DependencyHealth.up());
        dependencies.put("rustfs", DependencyHealth.up());
        dependencies.put("quarryService", DependencyHealth.up());
        dependencies.put("factoryService", DependencyHealth.up());
        dependencies.put("costAccounting", DependencyHealth.up());
        dependencies.put("diskSpace", DependencyHealth.up());
        return dependencies;
    }
}
