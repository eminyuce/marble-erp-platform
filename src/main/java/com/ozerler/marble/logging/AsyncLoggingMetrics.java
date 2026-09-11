package com.ozerler.marble.logging;

import java.util.concurrent.atomic.LongAdder;

/**
 * Thread-safe counters updated on the application-thread enqueue path
 * and read by Micrometer gauges/function counters.
 */
public final class AsyncLoggingMetrics {

    public static final String QUEUE_DEPTH = "logging.async.queue.depth";
    public static final String QUEUE_CAPACITY = "logging.async.queue.capacity";
    public static final String BUFFER_UTILIZATION = "logging.async.buffer.utilization";
    public static final String DROPPED = "logging.async.dropped";

    public static final String TAG_APPENDER = "appender";
    public static final String TAG_REASON = "reason";

    public static final String REASON_LOW_PRIORITY = "low_priority";
    public static final String REASON_QUEUE_FULL = "queue_full";
    public static final String REASON_EVICTED = "evicted";
    public static final String REASON_SHUTDOWN = "shutdown";

    private final LongAdder droppedLowPriority = new LongAdder();
    private final LongAdder droppedQueueFull = new LongAdder();
    private final LongAdder droppedEvicted = new LongAdder();
    private final LongAdder droppedShutdown = new LongAdder();

    void recordDroppedLowPriority() {
        droppedLowPriority.increment();
    }

    void recordDroppedQueueFull() {
        droppedQueueFull.increment();
    }

    void recordDroppedEvicted() {
        droppedEvicted.increment();
    }

    void recordDroppedOnShutdown(int count) {
        if (count > 0) {
            droppedShutdown.add(count);
        }
    }

    public long droppedLowPriorityCount() {
        return droppedLowPriority.sum();
    }

    public long droppedQueueFullCount() {
        return droppedQueueFull.sum();
    }

    public long droppedEvictedCount() {
        return droppedEvicted.sum();
    }

    public long droppedShutdownCount() {
        return droppedShutdown.sum();
    }

    public long droppedTotal() {
        return droppedLowPriority.sum()
                + droppedQueueFull.sum()
                + droppedEvicted.sum()
                + droppedShutdown.sum();
    }
}
