package com.ozerler.marble.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.ApplicationLogKpiDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.exception.ResourceNotFoundException;
import com.ozerler.marble.model.ApplicationLog;
import com.ozerler.marble.repository.ApplicationLogRepository;

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
        Mockito.when(
                        applicationLogRepository.findAll(
                                ArgumentMatchers.any(Specification.class),
                                ArgumentMatchers.any(Pageable.class)))
                .thenReturn(mockPage);

        TabulatorResponse<ApplicationLogDto> response = applicationLogService.getLogs(
                1, 50, "hata", "timestamp", "desc", List.of("ERROR"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        );

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getTotal()).isEqualTo(1);
        Assertions.assertThat(response.getData()).hasSize(1);

        ApplicationLogDto dto = response.getData().get(0);
        Assertions.assertThat(dto.getId()).isEqualTo(101L);
        Assertions.assertThat(dto.getLevel()).isEqualTo("ERROR");
        Assertions.assertThat(dto.getShortLoggerName()).isEqualTo("service.OrderService");
        Assertions.assertThat(dto.isHasException()).isTrue();
        Assertions.assertThat(dto.getFormattedTimestamp()).isEqualTo("30.09.2026 10:15:30");
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

        Mockito.when(applicationLogRepository.findById(50L)).thenReturn(Optional.of(logEntity));

        ApplicationLogDto dto = applicationLogService.getLogById(50L);

        Assertions.assertThat(dto).isNotNull();
        Assertions.assertThat(dto.getId()).isEqualTo(50L);
        Assertions.assertThat(dto.getMessage()).isEqualTo("Test tamamlandı");
    }

    @Test
    @DisplayName("getLogById should throw ResourceNotFoundException when not found")
    void shouldThrowExceptionWhenNotFound() {
        Mockito.when(applicationLogRepository.findById(999L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> applicationLogService.getLogById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getKpiMetrics counts every stored row and the last 24 hours separately")
    void shouldAggregateKpis() {
        Mockito.when(applicationLogRepository.count()).thenReturn(400L);
        Mockito.when(
                        applicationLogRepository.countByTimestampAfter(
                                ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(150L);
        Mockito.when(applicationLogRepository.countByLevel(ArgumentMatchers.any(String.class)))
                .thenAnswer(inv -> switch ((String) inv.getArgument(0)) {
                    case "ERROR" -> 28L;
                    case "WARN" -> 40L;
                    case "INFO" -> 300L;
                    default -> 0L;
                });
        Mockito.when(
                        applicationLogRepository.countByLevelAndTimestampAfter(
                                ArgumentMatchers.any(String.class),
                                ArgumentMatchers.any(LocalDateTime.class)))
                .thenAnswer(
                        inv -> {
                            String lvl = inv.getArgument(0);
                            return switch (lvl) {
                                case "ERROR" -> 5L;
                                case "WARN" -> 12L;
                                case "INFO" -> 133L;
                                default -> 0L;
                            };
                        });

        ApplicationLogKpiDto kpis = applicationLogService.getKpiMetrics();

        Assertions.assertThat(kpis.getTotalCount()).isEqualTo(400L);
        Assertions.assertThat(kpis.getErrorCount()).isEqualTo(28L);
        Assertions.assertThat(kpis.getWarnCount()).isEqualTo(40L);
        Assertions.assertThat(kpis.getInfoCount()).isEqualTo(300L);
        Assertions.assertThat(kpis.getTotalLast24Hours()).isEqualTo(150L);
        Assertions.assertThat(kpis.getErrorLast24Hours()).isEqualTo(5L);
        Assertions.assertThat(kpis.getWarnLast24Hours()).isEqualTo(12L);
        Assertions.assertThat(kpis.getInfoLast24Hours()).isEqualTo(133L);
    }

    @Test
    @DisplayName("cleanupOldLogs should delete logs older than retention days")
    void shouldCleanupOldLogs() {
        Mockito.when(
                        applicationLogRepository.deleteLogsOlderThan(
                                ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(42);

        int deleted = applicationLogService.cleanupOldLogs(30);

        Assertions.assertThat(deleted).isEqualTo(42);
        Mockito.verify(applicationLogRepository).deleteLogsOlderThan(ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    @DisplayName("scheduledRetentionCleanup should invoke cleanupOldLogs with default retention days")
    void shouldRunScheduledRetentionCleanup() {
        Mockito.when(
                        applicationLogRepository.deleteLogsOlderThan(
                                ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(15);

        applicationLogService.scheduledRetentionCleanup();

        Mockito.verify(applicationLogRepository).deleteLogsOlderThan(ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    @DisplayName("cleanupOldLogs with 0 should delete all logs")
    void shouldCleanupAllLogsWhenRetentionDaysIsZero() {
        Mockito.when(applicationLogRepository.deleteAllLogs()).thenReturn(100);

        int deleted = applicationLogService.cleanupOldLogs(0);

        Assertions.assertThat(deleted).isEqualTo(100);
        Mockito.verify(applicationLogRepository).deleteAllLogs();
    }
}
