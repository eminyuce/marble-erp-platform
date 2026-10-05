package com.ozerler.marble.service;

import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.ApplicationLogKpiDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.exception.ResourceNotFoundException;
import com.ozerler.marble.model.ApplicationLog;
import com.ozerler.marble.repository.ApplicationLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final int DEFAULT_RETENTION_DAYS = 30;

    private final ApplicationLogRepository applicationLogRepository;

    @Transactional(readOnly = true)
    public TabulatorResponse<ApplicationLogDto> getLogs(
            int page,
            int size,
            String search,
            String sortField,
            String sortDir,
            List<String> levels,
            LocalDate startDate,
            LocalDate endDate) {

        int pageIndex = Math.max(0, page - 1);
        int pageSize = (size > 0 && size <= 500) ? size : 50;

        String safeSortField = resolveSortField(sortField);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, safeSortField));

        Specification<ApplicationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate searchPred = cb.or(
                        cb.like(cb.lower(root.get("message")), pattern),
                        cb.like(cb.lower(root.get("loggerName")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("clientIp")), pattern),
                        cb.like(cb.lower(root.get("exceptionClass")), pattern)
                );
                predicates.add(searchPred);
            }

            if (levels != null && !levels.isEmpty()) {
                List<String> upperLevels = levels.stream()
                        .filter(l -> l != null && !l.isBlank())
                        .map(l -> l.trim().toUpperCase(Locale.ROOT))
                        .toList();
                if (!upperLevels.isEmpty()) {
                    predicates.add(root.get("level").in(upperLevels));
                }
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate.atStartOfDay()));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ApplicationLog> pageResult = applicationLogRepository.findAll(spec, pageable);
        List<ApplicationLogDto> dtoList = pageResult.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return TabulatorResponse.of(dtoList, pageResult.getTotalPages(), pageResult.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ApplicationLogDto getLogById(Long id) {
        ApplicationLog entity = applicationLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationLog", id));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public ApplicationLogKpiDto getKpiMetrics() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long total = applicationLogRepository.countByTimestampAfter(since);
        long errors = applicationLogRepository.countByLevelAndTimestampAfter("ERROR", since);
        long warns = applicationLogRepository.countByLevelAndTimestampAfter("WARN", since);
        long infos = applicationLogRepository.countByLevelAndTimestampAfter("INFO", since);

        return ApplicationLogKpiDto.builder()
                .totalLast24Hours(total)
                .errorLast24Hours(errors)
                .warnLast24Hours(warns)
                .infoLast24Hours(infos)
                .build();
    }

    @Transactional
    public int cleanupOldLogs(int retentionDays) {
        int days = retentionDays > 0 ? retentionDays : DEFAULT_RETENTION_DAYS;
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = applicationLogRepository.deleteLogsOlderThan(cutoff);
        log.info("Cleaned up {} application logs older than {} days (cutoff: {})", deleted, days, cutoff);
        return deleted;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledRetentionCleanup() {
        cleanupOldLogs(DEFAULT_RETENTION_DAYS);
    }

    private ApplicationLogDto mapToDto(ApplicationLog entity) {
        String formatted = entity.getTimestamp() != null ? entity.getTimestamp().format(FORMATTER) : "";
        String shortLogger = shortenLogger(entity.getLoggerName());
        boolean hasEx = (entity.getExceptionClass() != null && !entity.getExceptionClass().isBlank())
                || (entity.getStackTrace() != null && !entity.getStackTrace().isBlank());

        return ApplicationLogDto.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp())
                .formattedTimestamp(formatted)
                .level(entity.getLevel())
                .loggerName(entity.getLoggerName())
                .shortLoggerName(shortLogger)
                .message(entity.getMessage())
                .exceptionClass(entity.getExceptionClass())
                .exceptionMessage(entity.getExceptionMessage())
                .stackTrace(entity.getStackTrace())
                .username(entity.getUsername())
                .clientIp(entity.getClientIp())
                .httpMethod(entity.getHttpMethod())
                .requestUri(entity.getRequestUri())
                .correlationId(entity.getCorrelationId())
                .threadName(entity.getThreadName())
                .hasException(hasEx)
                .build();
    }

    private String shortenLogger(String fullLogger) {
        if (fullLogger == null || fullLogger.isBlank()) {
            return "";
        }
        int lastDot = fullLogger.lastIndexOf('.');
        if (lastDot < 0) {
            return fullLogger;
        }
        int prevDot = fullLogger.lastIndexOf('.', lastDot - 1);
        if (prevDot < 0) {
            return fullLogger;
        }
        return fullLogger.substring(prevDot + 1);
    }

    private String resolveSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return "timestamp";
        }
        return switch (sortField) {
            case "id" -> "id";
            case "level" -> "level";
            case "loggerName", "shortLoggerName" -> "loggerName";
            case "username" -> "username";
            case "clientIp" -> "clientIp";
            default -> "timestamp";
        };
    }
}
