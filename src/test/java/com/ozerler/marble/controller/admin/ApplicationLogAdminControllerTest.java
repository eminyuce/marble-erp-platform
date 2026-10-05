package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.ApplicationLogKpiDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.service.ApplicationLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        Model model = new ExtendedModelMap();
        String view = controller.logsPage(model);

        assertThat(view).isEqualTo("admin/settings/logs");
        assertThat(model.getAttribute("kpis")).isEqualTo(kpis);
        assertThat(model.getAttribute("availableLevels")).isEqualTo(List.of("ERROR", "WARN", "INFO", "DEBUG"));
        assertThat(model.getAttribute("currentSection")).isEqualTo("settings");
    }

    @Test
    @DisplayName("getLogsData should return TabulatorResponse from service")
    void shouldReturnLogsData() {
        TabulatorResponse<ApplicationLogDto> expected = TabulatorResponse.of(List.of(), 1, 0);
        when(applicationLogService.getLogs(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expected);

        TabulatorResponse<ApplicationLogDto> response = controller.getLogsData(
                1, 50, "test", "timestamp", "desc", "ERROR", List.of("ERROR", "WARN"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        );

        assertThat(response).isSameAs(expected);
        verify(applicationLogService).getLogs(
                eq(1), eq(50), eq("test"), eq("timestamp"), eq("desc"),
                eq(List.of("ERROR", "WARN")), eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 30))
        );
    }

    @Test
    @DisplayName("getLogDetail should return log dto")
    void shouldReturnLogDetail() {
        ApplicationLogDto dto = ApplicationLogDto.builder().id(12L).message("Detay").build();
        when(applicationLogService.getLogById(12L)).thenReturn(dto);

        ResponseEntity<ApplicationLogDto> response = controller.getLogDetail(12L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    @DisplayName("getKpiMetrics should return current KPIs from service")
    void shouldReturnKpiMetrics() {
        ApplicationLogKpiDto kpis = ApplicationLogKpiDto.builder().totalLast24Hours(50).build();
        when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        ApplicationLogKpiDto response = controller.getKpiMetrics();

        assertThat(response).isSameAs(kpis);
        verify(applicationLogService).getKpiMetrics();
    }

    @Test
    @DisplayName("cleanupOldLogs should trigger cleanup and return result with kpis")
    void shouldTriggerCleanup() {
        ApplicationLogKpiDto kpis = ApplicationLogKpiDto.builder().totalLast24Hours(10).build();
        when(applicationLogService.cleanupOldLogs(15)).thenReturn(35);
        when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        ResponseEntity<Map<String, Object>> response = controller.cleanupOldLogs(15);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat(response.getBody()).containsEntry("deletedCount", 35);
        assertThat(response.getBody()).containsEntry("kpis", kpis);
        verify(applicationLogService).cleanupOldLogs(15);
    }

    @Test
    @DisplayName("cleanupOldLogs with 0 should trigger all logs cleanup and return all logs message")
    void shouldTriggerCleanupAllLogs() {
        ApplicationLogKpiDto kpis = ApplicationLogKpiDto.builder().totalLast24Hours(0).build();
        when(applicationLogService.cleanupOldLogs(0)).thenReturn(75);
        when(applicationLogService.getKpiMetrics()).thenReturn(kpis);

        ResponseEntity<Map<String, Object>> response = controller.cleanupOldLogs(0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat(response.getBody()).containsEntry("deletedCount", 75);
        assertThat(response.getBody()).containsEntry("message", "Tüm log kayıtları başarıyla temizlendi (75 adet).");
        assertThat(response.getBody()).containsEntry("kpis", kpis);
        verify(applicationLogService).cleanupOldLogs(0);
    }
}
