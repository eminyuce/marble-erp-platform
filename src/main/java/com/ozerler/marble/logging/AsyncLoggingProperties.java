package com.ozerler.marble.logging;

import com.ozerler.marble.common.Constants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bindable counterpart of the Logback {@link AsyncLoggingAppender} settings.
 * Values are consumed by {@code logback-spring.xml} via {@code <springProperty>}
 * and therefore take effect when the logging system starts.
 */
@Configuration
@ConfigurationProperties(prefix = "logging.async")
@Getter
@Setter
public class AsyncLoggingProperties {

    /**
     * Bounded queue size. Application threads enqueue here; the worker drains it.
     */
    private int queueCapacity = Constants.ASYNC_LOGGING_QUEUE_CAPACITY;

    /**
     * Drop TRACE/DEBUG once occupancy reaches this percent of {@link #queueCapacity}.
     */
    private int highWaterMarkPercent = Constants.ASYNC_LOGGING_HIGH_WATER_MARK_PERCENT;

    /**
     * {@link OverflowPolicy#DROP_LOW_PRIORITY} (default) or {@link OverflowPolicy#BLOCK}.
     */
    private OverflowPolicy overflowPolicy = OverflowPolicy.DROP_LOW_PRIORITY;

    /**
     * Maximum events the worker writes to the nested appender per wakeup.
     */
    private int batchSize = Constants.ASYNC_LOGGING_BATCH_SIZE;

    /**
     * Best-effort flush budget when the appender (or JVM) stops.
     */
    private long drainTimeoutMillis = Constants.ASYNC_LOGGING_DRAIN_TIMEOUT_MILLIS;

    /**
     * Worker {@code poll} timeout so the thread parks instead of busy-spinning.
     */
    private long pollTimeoutMillis = Constants.ASYNC_LOGGING_POLL_TIMEOUT_MILLIS;

    /**
     * Capture caller stack on the application thread (expensive; off by default).
     */
    private boolean includeCallerData = Constants.ASYNC_LOGGING_INCLUDE_CALLER_DATA;
}
