package com.ozerler.marble.logging;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * Publishes live async-appender gauges and drop counters to the Actuator
 * {@link MeterRegistry}. Reads the Logback-managed appender instances so
 * metrics stay correct even though Logback constructs the appender, not Spring.
 */
@Component
public class AsyncLoggingMetricsBinder implements MeterBinder {

    private static final Object HOLDER = new Object();

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(AsyncLoggingMetrics.QUEUE_DEPTH, HOLDER, ignored -> AsyncLoggingAppender.totalQueueDepth())
                .description("Log events waiting in the async logging queue")
                .tag(AsyncLoggingMetrics.TAG_APPENDER, "async")
                .register(registry);

        Gauge.builder(AsyncLoggingMetrics.QUEUE_CAPACITY, HOLDER, ignored -> AsyncLoggingAppender.totalQueueCapacity())
                .description("Configured async logging queue capacity")
                .tag(AsyncLoggingMetrics.TAG_APPENDER, "async")
                .register(registry);

        Gauge.builder(AsyncLoggingMetrics.BUFFER_UTILIZATION, HOLDER, ignored -> AsyncLoggingAppender.aggregateBufferUtilization())
                .description("Async logging buffer utilization (depth / capacity)")
                .tag(AsyncLoggingMetrics.TAG_APPENDER, "async")
                .register(registry);

        bindDropped(registry, AsyncLoggingMetrics.REASON_LOW_PRIORITY, ignored -> AsyncLoggingAppender.totalDroppedLowPriority());
        bindDropped(registry, AsyncLoggingMetrics.REASON_QUEUE_FULL, ignored -> AsyncLoggingAppender.totalDroppedQueueFull());
        bindDropped(registry, AsyncLoggingMetrics.REASON_EVICTED, ignored -> AsyncLoggingAppender.totalDroppedEvicted());
        bindDropped(registry, AsyncLoggingMetrics.REASON_SHUTDOWN, ignored -> AsyncLoggingAppender.totalDroppedShutdown());
    }

    private static void bindDropped(
            MeterRegistry registry,
            String reason,
            java.util.function.ToDoubleFunction<Object> count
    ) {
        FunctionCounter.builder(AsyncLoggingMetrics.DROPPED, HOLDER, count)
                .tag(AsyncLoggingMetrics.TAG_APPENDER, "async")
                .tag(AsyncLoggingMetrics.TAG_REASON, reason)
                .description("Log events discarded by the async logging appender")
                .register(registry);
    }
}
