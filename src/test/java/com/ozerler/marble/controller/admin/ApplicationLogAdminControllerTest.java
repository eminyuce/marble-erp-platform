package com.ozerler.marble.controller.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.ApplicationLogKpiDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.service.ApplicationLogService;

@ExtendWith(MockitoExtension.class)
class ApplicationLogAdminControllerTest {

    @Mock
    private ApplicationLogService applicationLogService;

    @InjectMocks
    private ApplicationLogAdminController controller;

    @Test
    @DisplayName("logsPage should populate model and return view name")
    void shouldReturnLogsPageView() {
        ApplicationLogKpiDto kpis = ApplicationLogKpiDto.builder()
                .totalLast24Hours(100)
                .errorLast24Hours(2)
                .warnLast24Hours(5)
                .infoLast24Hours(93)
                .build();
        Mockito.when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        Model model = new ExtendedModelMap();
        String view = controller.logsPage(model);

        Assertions.assertThat(view).isEqualTo("admin/settings/logs");
        Assertions.assertThat(model.getAttribute("kpis")).isEqualTo(kpis);
        Assertions.assertThat(model.getAttribute("availableLevels")).isEqualTo(List.of("ERROR", "WARN", "INFO", "DEBUG"));
        Assertions.assertThat(model.getAttribute("currentSection")).isEqualTo("settings");
    }

    @Test
    @DisplayName("getLogsData should return TabulatorResponse from service")
    void shouldReturnLogsData() {
        TabulatorResponse<ApplicationLogDto> expected = TabulatorResponse.of(List.of(), 1, 0);
        Mockito.when(applicationLogService.getLogs(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(expected);

        TabulatorResponse<ApplicationLogDto> response = controller.getLogsData(
                1, 50, "test", "timestamp", "desc", "ERROR", List.of("ERROR", "WARN"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        );

        Assertions.assertThat(response).isSameAs(expected);
        Mockito.verify(applicationLogService).getLogs(
                ArgumentMatchers.eq(1), ArgumentMatchers.eq(50), ArgumentMatchers.eq("test"), ArgumentMatchers.eq("timestamp"), ArgumentMatchers.eq("desc"),
                ArgumentMatchers.eq(List.of("ERROR", "WARN")), ArgumentMatchers.eq(LocalDate.of(2026, 9, 1)), ArgumentMatchers.eq(LocalDate.of(2026, 9, 30))
        );
    }

    @Test
    @DisplayName("getLogDetail should return log dto")
    void shouldReturnLogDetail() {
        ApplicationLogDto dto = ApplicationLogDto.builder().id(12L).message("Detay").build();
        Mockito.when(applicationLogService.getLogById(12L)).thenReturn(dto);

        ResponseEntity<ApplicationLogDto> response = controller.getLogDetail(12L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    @DisplayName("getKpiMetrics should return current KPIs from service")
    void shouldReturnKpiMetrics() {
        ApplicationLogKpiDto kpis = ApplicationLogKpiDto.builder().totalLast24Hours(50).build();
        Mockito.when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        ApplicationLogKpiDto response = controller.getKpiMetrics();

        Assertions.assertThat(response).isSameAs(kpis);
        Mockito.verify(applicationLogService).getKpiMetrics();
    }

    @Test
    @DisplayName("cleanupOldLogs should trigger cleanup and return result with kpis")
    void shouldTriggerCleanup() {
        ApplicationLogKpiDto kpis = ApplicationLogKpiDto.builder().totalLast24Hours(10).build();
        Mockito.when(applicationLogService.cleanupOldLogs(15)).thenReturn(35);
        Mockito.when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        ResponseEntity<Map<String, Object>> response = controller.cleanupOldLogs(15);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).containsEntry("success", true);
        Assertions.assertThat(response.getBody()).containsEntry("deletedCount", 35);
        Assertions.assertThat(response.getBody()).containsEntry("kpis", kpis);
        Mockito.verify(applicationLogService).cleanupOldLogs(15);
    }

    @Test
    @DisplayName("cleanupOldLogs with 0 should trigger all logs cleanup and return all logs message")
    void shouldTriggerCleanupAllLogs() {
        ApplicationLogKpiDto kpis = ApplicationLogKpiDto.builder().totalLast24Hours(0).build();
        Mockito.when(applicationLogService.cleanupOldLogs(0)).thenReturn(75);
        Mockito.when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        ResponseEntity<Map<String, Object>> response = controller.cleanupOldLogs(0);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).containsEntry("success", true);
        Assertions.assertThat(response.getBody()).containsEntry("deletedCount", 75);
        Assertions.assertThat(response.getBody()).containsEntry("message", "Tüm log kayıtları başarıyla temizlendi (75 adet).");
        Assertions.assertThat(response.getBody()).containsEntry("kpis", kpis);
        Mockito.verify(applicationLogService).cleanupOldLogs(0);
    }
}
