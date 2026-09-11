package com.ozerler.marble.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncLoggingAppenderTest {

    private AsyncLoggingAppender async;
    private LoggerContext context;

    @AfterEach
    void tearDown() {
        if (async != null && async.isStarted()) {
            async.stop();
        }
        if (context != null) {
            context.stop();
        }
    }

    @Test
    @DisplayName("enqueue delivers events to the nested appender")
    void enqueueDeliversEventsToNestedAppender() {
        ListAppender<ILoggingEvent> nested = startedListAppender("nested");
        async = startedAsync("enqueue-test", 32, 90, 16, nested);
        assertThat(async.isStarted())
                .as(() -> String.valueOf(context.getStatusManager().getCopyOfStatusList()))
                .isTrue();

        async.doAppend(event(Level.INFO, "order-created"));
        async.stop();

        assertThat(nested.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .containsExactly("order-created");
        assertThat(async.metrics().droppedTotal()).isZero();
    }

    @Test
    @DisplayName("TRACE/DEBUG are dropped at high-water mark and INFO is dropped when full; ERROR evicts oldest")
    void dropsLowPriorityAtHighWaterMarkAndPreservesError() throws Exception {
        BlockingNestedAppender nested = new BlockingNestedAppender();
        nested.setContext(newContext());
        nested.setName("blocking");
        nested.start();

        async = new AsyncLoggingAppender();
        async.setContext(context);
        async.setName("drop-test");
        async.setQueueCapacity(10);
        async.setHighWaterMarkPercent(50);
        async.setOverflowPolicy(OverflowPolicy.DROP_LOW_PRIORITY);
        async.setBatchSize(1);
        async.setDrainTimeoutMillis(2_000L);
        async.addAppender(nested);
        async.start();

        async.doAppend(event(Level.INFO, "blocker"));
        assertThat(nested.entered.await(2, TimeUnit.SECONDS)).isTrue();

        for (int i = 0; i < 5; i++) {
            async.doAppend(event(Level.INFO, "fill-" + i));
        }
        assertThat(async.queueDepth()).isGreaterThanOrEqualTo(5);

        async.doAppend(event(Level.DEBUG, "debug-at-high-water"));
        async.doAppend(event(Level.TRACE, "trace-at-high-water"));

        assertThat(async.metrics().droppedLowPriorityCount()).isGreaterThanOrEqualTo(2);

        for (int i = 0; i < 20; i++) {
            async.doAppend(event(Level.INFO, "overflow-" + i));
        }
        assertThat(async.metrics().droppedQueueFullCount()).isPositive();

        long droppedBeforeError = async.metrics().droppedTotal();
        async.doAppend(event(Level.ERROR, "must-preserve-error"));
        assertThat(async.metrics().droppedEvictedCount()).isPositive();
        assertThat(async.metrics().droppedTotal()).isGreaterThan(droppedBeforeError);

        nested.release.countDown();
        async.stop();

        assertThat(nested.recorded)
                .extracting(ILoggingEvent::getFormattedMessage)
                .contains("must-preserve-error");
    }

    @Test
    @DisplayName("stop() flushes remaining queued events without dropping them")
    void stopFlushesRemainingQueuedEvents() {
        ListAppender<ILoggingEvent> nested = startedListAppender("flush-nested");
        async = startedAsync("flush-test", 256, 90, 32, nested);

        int eventCount = 80;
        for (int i = 0; i < eventCount; i++) {
            async.doAppend(event(Level.INFO, "flush-" + i));
        }
        async.stop();

        assertThat(nested.list).hasSize(eventCount);
        assertThat(async.metrics().droppedShutdownCount()).isZero();
        assertThat(async.queueDepth()).isZero();
    }

    private LoggerContext newContext() {
        context = new LoggerContext();
        context.setName("async-logging-test");
        context.start();
        return context;
    }

    private ListAppender<ILoggingEvent> startedListAppender(String name) {
        ListAppender<ILoggingEvent> nested = new ListAppender<>();
        nested.setContext(newContext());
        nested.setName(name);
        nested.start();
        return nested;
    }

    private AsyncLoggingAppender startedAsync(
            String name,
            int capacity,
            int highWaterMarkPercent,
            int batchSize,
            ch.qos.logback.core.Appender<ILoggingEvent> nested
    ) {
        AsyncLoggingAppender appender = new AsyncLoggingAppender();
        appender.setContext(context);
        appender.setName(name);
        appender.setQueueCapacity(capacity);
        appender.setHighWaterMarkPercent(highWaterMarkPercent);
        appender.setOverflowPolicy(OverflowPolicy.DROP_LOW_PRIORITY);
        appender.setBatchSize(batchSize);
        appender.setDrainTimeoutMillis(2_000L);
        appender.addAppender(nested);
        appender.start();
        return appender;
    }

    private LoggingEvent event(Level level, String message) {
        Logger logger = context.getLogger("com.ozerler.marble.test.async");
        return new LoggingEvent(Logger.FQCN, logger, level, message, null, null);
    }

    private static final class BlockingNestedAppender extends AppenderBase<ILoggingEvent> {

        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final List<ILoggingEvent> recorded = new CopyOnWriteArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            recorded.add(eventObject);
            entered.countDown();
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    addError("Timed out waiting to release blocking nested appender");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
