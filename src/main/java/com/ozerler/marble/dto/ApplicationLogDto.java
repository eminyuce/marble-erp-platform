package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationLogDto {

    private Long id;
    private LocalDateTime timestamp;
    private String formattedTimestamp;
    private String level;
    private String loggerName;
    private String shortLoggerName;
    private String message;
    private String exceptionClass;
    private String exceptionMessage;
    private String stackTrace;
    private String username;
    private String clientIp;
    private String httpMethod;
    private String requestUri;
    private String correlationId;
    private String threadName;
    private boolean hasException;
}
