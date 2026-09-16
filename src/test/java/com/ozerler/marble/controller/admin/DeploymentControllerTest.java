package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.DeploymentLogDto;
import com.ozerler.marble.dto.DeploymentStartResult;
import com.ozerler.marble.dto.DeploymentState;
import com.ozerler.marble.dto.DeploymentStatusDto;
import com.ozerler.marble.service.DeploymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeploymentControllerTest {

    @Mock
    private DeploymentService deploymentService;

    private MockMvc mockMvc;
    private DeploymentController controller;

    @BeforeEach
    void setUp() {
        controller = new DeploymentController(deploymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /admin/deployment renders the live deploy page")
    void page_RendersTemplate() {
        when(deploymentService.currentStatus()).thenReturn(sampleStatus());

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.page(model);

        assertThat(view).isEqualTo("admin/deployment");
        assertThat(model.get("currentSection")).isEqualTo("settings");
        assertThat(model.get("deploymentStatus")).isNotNull();
    }

    @Test
    @DisplayName("GET /admin/deployment/status returns current JSON status")
    void status_ReturnsJson() throws Exception {
        when(deploymentService.currentStatus()).thenReturn(sampleStatus());

        mockMvc.perform(get("/admin/deployment/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("IDLE"))
                .andExpect(jsonPath("$.canStart").value(true));
    }

    @Test
    @DisplayName("GET /admin/deployment/log returns log chunk JSON")
    void log_ReturnsChunk() throws Exception {
        when(deploymentService.readLog(0)).thenReturn(DeploymentLogDto.builder()
                .offset(0)
                .nextOffset(5)
                .chunk("hello")
                .state(DeploymentState.RUNNING)
                .build());

        mockMvc.perform(get("/admin/deployment/log").param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunk").value("hello"))
                .andExpect(jsonPath("$.state").value("RUNNING"));
    }

    @Test
    @DisplayName("POST /admin/deployment/start returns 202 when accepted")
    void start_Accepted() throws Exception {
        when(deploymentService.startDeploy()).thenReturn(DeploymentStartResult.builder()
                .accepted(true)
                .state(DeploymentState.RUNNING)
                .message("started")
                .build());

        mockMvc.perform(post("/admin/deployment/start").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    @DisplayName("POST /admin/deployment/start returns 409 when a deploy is already running")
    void start_ConflictWhenRunning() throws Exception {
        when(deploymentService.startDeploy()).thenReturn(DeploymentStartResult.builder()
                .accepted(false)
                .state(DeploymentState.RUNNING)
                .message("already running")
                .build());

        mockMvc.perform(post("/admin/deployment/start"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.accepted").value(false));
    }

    private static DeploymentStatusDto sampleStatus() {
        return DeploymentStatusDto.builder()
                .state(DeploymentState.IDLE)
                .canStart(true)
                .linuxHost(true)
                .enabled(true)
                .commandAvailable(true)
                .sudoReady(true)
                .branch("main")
                .headCommit("abc1234")
                .headMessage("deploy ui")
                .message("ready")
                .blockingReasons(List.of())
                .build();
    }
}
