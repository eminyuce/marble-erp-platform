package com.ozerler.marble.service;

import com.ozerler.marble.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Presenter component for displaying standardized audit information across ERP forms and views.
 * Formats:
 * - DateTime: "d.MM.yyyy HH:mm" (e.g. "22.07.2026 20:11" or "3.09.2026 12:57")
 * - User: "email (FullName)" (e.g. "xyz@email.com (Emin YUCE)")
 */
@Slf4j
@Component("auditPresenter")
@RequiredArgsConstructor
public class AuditPresenter {

    private final UserRepository userRepository;
    private final Map<String, String> userDisplayCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d.MM.yyyy HH:mm", Locale.forLanguageTag("tr-TR"));

    /**
     * Formats an auditor user id (email) to include their full name when found.
     * Example: "xyz@email.com (Emin YUCE)" or "system@ozerler.com (Sistem)".
     *
     * @param email The user email recorded in audit column
     * @return Formatted audit user string
     */
    public String formatUser(String email) {
        if (email == null || email.isBlank()) {
            return "-";
        }
        return userDisplayCache.computeIfAbsent(email, key -> {
            try {
                return userRepository.findByEmail(key)
                        .map(u -> {
                            String fullName = u.getFullName();
                            if (fullName != null && !fullName.isBlank()) {
                                return key + " (" + fullName.trim() + ")";
                            }
                            return key;
                        })
                        .orElseGet(() -> {
                            if (key.equalsIgnoreCase("system@ozerler.com")) {
                                return key + " (Sistem)";
                            }
                            return key;
                        });
            } catch (Exception e) {
                log.warn("Failed to lookup user for audit display: {}", key, e);
                return key;
            }
        });
    }

    /**
     * Formats a timestamp in Turkish ERP format "d.MM.yyyy HH:mm".
     * Example: "22.07.2026 20:11" or "3.09.2026 12:57".
     *
     * @param dateTime The audit timestamp (LocalDateTime, Date, Instant, etc.)
     * @return Formatted date-time string
     */
    public String formatDateTime(Object dateTime) {
        if (dateTime == null) {
            return "-";
        }
        if (dateTime instanceof LocalDateTime ldt) {
            return ldt.format(DATE_TIME_FORMATTER);
        }
        if (dateTime instanceof java.time.ZonedDateTime zdt) {
            return zdt.format(DATE_TIME_FORMATTER);
        }
        if (dateTime instanceof java.time.OffsetDateTime odt) {
            return odt.format(DATE_TIME_FORMATTER);
        }
        if (dateTime instanceof java.time.Instant inst) {
            return LocalDateTime.ofInstant(inst, java.time.ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
        }
        if (dateTime instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), java.time.ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
        }
        try {
            return LocalDateTime.parse(dateTime.toString()).format(DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return "-";
        }
    }
}
