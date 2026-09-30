package com.ozerler.marble.logging.db;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseLogBatchWriter {

    private static final String INSERT_SQL =
            "INSERT INTO application_logs (" +
                    "timestamp, level, logger_name, message, exception_class, exception_message, " +
                    "stack_trace, username, client_ip, http_method, request_uri, correlation_id, thread_name" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final int BATCH_SIZE = 250;

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseLogQueue databaseLogQueue;

    private final AtomicBoolean appenderRegistered = new AtomicBoolean(false);
    private DatabaseLogAppender activeAppender;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        registerAppender();
    }

    public synchronized void registerAppender() {
        if (appenderRegistered.compareAndSet(false, true)) {
            try {
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                activeAppender = new DatabaseLogAppender();
                activeAppender.setContext(context);
                activeAppender.setName("DATABASE_LOG_APPENDER");
                activeAppender.start();

                Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
                rootLogger.addAppender(activeAppender);
                log.info("DatabaseLogAppender successfully registered to RootLogger.");
            } catch (Exception ex) {
                log.warn("Failed to register DatabaseLogAppender dynamically: {}", ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void processBatch() {
        if (databaseLogQueue.isEmpty()) {
            return;
        }

        List<DatabaseLogEntry> batch = new ArrayList<>(BATCH_SIZE);
        databaseLogQueue.drainTo(batch, BATCH_SIZE);

        if (!batch.isEmpty()) {
            writeBatch(batch);
        }
    }

    @PreDestroy
    public void flushOnShutdown() {
        log.info("Flushing remaining application logs to database on shutdown...");
        if (activeAppender != null && activeAppender.isStarted()) {
            activeAppender.stop();
        }

        while (!databaseLogQueue.isEmpty()) {
            List<DatabaseLogEntry> batch = new ArrayList<>(BATCH_SIZE);
            databaseLogQueue.drainTo(batch, BATCH_SIZE);
            if (!batch.isEmpty()) {
                writeBatch(batch);
            }
        }
    }

    private void writeBatch(List<DatabaseLogEntry> batch) {
        try {
            jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    DatabaseLogEntry entry = batch.get(i);
                    ps.setTimestamp(1, Timestamp.valueOf(entry.timestamp()));
                    ps.setString(2, entry.level());
                    ps.setString(3, entry.loggerName());
                    ps.setString(4, entry.message());
                    ps.setString(5, entry.exceptionClass());
                    ps.setString(6, entry.exceptionMessage());
                    ps.setString(7, entry.stackTrace());
                    ps.setString(8, entry.username());
                    ps.setString(9, entry.clientIp());
                    ps.setString(10, entry.httpMethod());
                    ps.setString(11, entry.requestUri());
                    ps.setString(12, entry.correlationId());
                    ps.setString(13, entry.threadName());
                }

                @Override
                public int getBatchSize() {
                    return batch.size();
                }
            });
        } catch (Exception ex) {
            // Internal safety log - avoid throw to not crash scheduled executor
            log.error("Failed to write batch of {} logs to database: {}", batch.size(), ex.getMessage());
        }
    }
}
