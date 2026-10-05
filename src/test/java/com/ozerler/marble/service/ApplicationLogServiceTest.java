package com.ozerler.marble.service;

import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.ApplicationLogKpiDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.exception.ResourceNotFoundException;
import com.ozerler.marble.model.ApplicationLog;
import com.ozerler.marble.repository.ApplicationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationLogServiceTest {

    @Mock
    private ApplicationLogRepository applicationLogRepository;

    @InjectMocks
    private ApplicationLogService applicationLogService;

    @Test
    @DisplayName("getLogs should return formatted TabulatorResponse with dtos")
    void shouldReturnLogsWithTabulatorResponse() {
        ApplicationLog logEntity = ApplicationLog.builder()
                .id(101L)
                .timestamp(LocalDateTime.of(2026, 9, 30, 10, 15, 30))
                .level("ERROR")
                .loggerName("com.ozerler.marble.service.OrderService")
                .message("Sipariş işlenirken beklenmeyen hata")
                .exceptionClass("java.lang.IllegalStateException")
                .exceptionMessage("Geçersiz durum")
                .stackTrace("java.lang.IllegalStateException: Geçersiz durum\n\tat ...")
                .username("admin")
                .clientIp("127.0.0.1")
                .httpMethod("POST")
                .requestUri("/api/orders/create")
                .correlationId("test-corr-id")
                .threadName("http-nio-8080-exec-1")
                .build();

        Page<ApplicationLog> mockPage = new PageImpl<>(List.of(logEntity));
        when(applicationLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        TabulatorResponse<ApplicationLogDto> response = applicationLogService.getLogs(
                1, 50, "hata", "timestamp", "desc", List.of("ERROR"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        );

        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getData()).hasSize(1);

        ApplicationLogDto dto = response.getData().get(0);
        assertThat(dto.getId()).isEqualTo(101L);
        assertThat(dto.getLevel()).isEqualTo("ERROR");
        assertThat(dto.getShortLoggerName()).isEqualTo("service.OrderService");
        assertThat(dto.isHasException()).isTrue();
        assertThat(dto.getFormattedTimestamp()).isEqualTo("30.09.2026 10:15:30");
    }

    @Test
    @DisplayName("getLogById should return dto when found")
    void shouldReturnDtoWhenFound() {
        ApplicationLog logEntity = ApplicationLog.builder()
                .id(50L)
                .timestamp(LocalDateTime.now())
                .level("INFO")
                .loggerName("com.ozerler.marble.controller.TestController")
                .message("Test tamamlandı")
                .build();

        when(applicationLogRepository.findById(50L)).thenReturn(Optional.of(logEntity));

        ApplicationLogDto dto = applicationLogService.getLogById(50L);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(50L);
        assertThat(dto.getMessage()).isEqualTo("Test tamamlandı");
    }

    @Test
    @DisplayName("getLogById should throw ResourceNotFoundException when not found")
    void shouldThrowExceptionWhenNotFound() {
        when(applicationLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationLogService.getLogById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getKpiMetrics should aggregate 24 hour stats")
    void shouldAggregateKpis() {
        when(applicationLogRepository.countByTimestampAfter(any(LocalDateTime.class))).thenReturn(150L);
        when(applicationLogRepository.countByLevelAndTimestampAfter(any(String.class), any(LocalDateTime.class)))
                .thenAnswer(inv -> {
                    String lvl = inv.getArgument(0);
                    return switch (lvl) {
                        case "ERROR" -> 5L;
                        case "WARN" -> 12L;
                        case "INFO" -> 133L;
                        default -> 0L;
                    };
                });

        ApplicationLogKpiDto kpis = applicationLogService.getKpiMetrics();

        assertThat(kpis.getTotalLast24Hours()).isEqualTo(150L);
        assertThat(kpis.getErrorLast24Hours()).isEqualTo(5L);
        assertThat(kpis.getWarnLast24Hours()).isEqualTo(12L);
        assertThat(kpis.getInfoLast24Hours()).isEqualTo(133L);
    }

    @Test
    @DisplayName("cleanupOldLogs should delete logs older than retention days")
    void shouldCleanupOldLogs() {
        when(applicationLogRepository.deleteLogsOlderThan(any(LocalDateTime.class))).thenReturn(42);

        int deleted = applicationLogService.cleanupOldLogs(30);

        assertThat(deleted).isEqualTo(42);
        verify(applicationLogRepository).deleteLogsOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("scheduledRetentionCleanup should invoke cleanupOldLogs with default retention days")
    void shouldRunScheduledRetentionCleanup() {
        when(applicationLogRepository.deleteLogsOlderThan(any(LocalDateTime.class))).thenReturn(15);

        applicationLogService.scheduledRetentionCleanup();

        verify(applicationLogRepository).deleteLogsOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("cleanupOldLogs with 0 should delete all logs")
    void shouldCleanupAllLogsWhenRetentionDaysIsZero() {
        when(applicationLogRepository.deleteAllLogs()).thenReturn(100);

        int deleted = applicationLogService.cleanupOldLogs(0);

        assertThat(deleted).isEqualTo(100);
        verify(applicationLogRepository).deleteAllLogs();
    }
}
