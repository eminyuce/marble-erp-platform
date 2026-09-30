package com.ozerler.marble.logging.db;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseLogAppenderTest {

    @Mock
    private ILoggingEvent mockEvent;

    private DatabaseLogAppender appender;
    private DatabaseLogQueue queue;

    @BeforeEach
    void setUp() {
        queue = DatabaseLogQueue.getInstance();
        // Drain any leftover events
        List<DatabaseLogEntry> drain = new ArrayList<>();
        queue.drainTo(drain, 10000);

        appender = new DatabaseLogAppender();
        appender.start();
    }

    @Test
    @DisplayName("appender should enqueue application events")
    void shouldEnqueueApplicationEvents() {
        when(mockEvent.getLoggerName()).thenReturn("com.ozerler.marble.service.OrderService");
        when(mockEvent.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(mockEvent.getLevel()).thenReturn(Level.INFO);
        when(mockEvent.getFormattedMessage()).thenReturn("Yeni sipariş oluşturuldu: SIP-2026-001");
        when(mockEvent.getThreadName()).thenReturn("test-thread");
        when(mockEvent.getMDCPropertyMap()).thenReturn(Map.of("userId", "admin", "clientIp", "10.0.0.1"));

        appender.append(mockEvent);

        assertThat(queue.isEmpty()).isFalse();
        List<DatabaseLogEntry> drained = new ArrayList<>();
        queue.drainTo(drained, 10);
        assertThat(drained).hasSize(1);
        DatabaseLogEntry entry = drained.get(0);
        assertThat(entry.loggerName()).isEqualTo("com.ozerler.marble.service.OrderService");
        assertThat(entry.message()).isEqualTo("Yeni sipariş oluşturuldu: SIP-2026-001");
        assertThat(entry.username()).isEqualTo("admin");
        assertThat(entry.clientIp()).isEqualTo("10.0.0.1");
        assertThat(entry.level()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("appender should ignore database/hikari/hibernate loggers to prevent loops")
    void shouldIgnoreDatabaseLoggers() {
        when(mockEvent.getLoggerName()).thenReturn("org.hibernate.SQL");

        appender.append(mockEvent);

        assertThat(queue.isEmpty()).isTrue();
    }
}
