package com.ozerler.marble.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.util.ReentryGuard;
import ch.qos.logback.core.util.ReentryGuardFactory;
import com.ozerler.marble.common.Constants;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-blocking (by default) Logback appender that enqueues events on application
 * threads and writes them in batches on a dedicated worker.
 *
 * <p>Overflow: TRACE/DEBUG are discarded at the high-water mark so business threads
 * are not stalled. WARN/ERROR are preserved by evicting the oldest queued event
 * when the buffer is full ({@link OverflowPolicy#DROP_LOW_PRIORITY}).
 */
public class AsyncLoggingAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
        implements AppenderAttachable<ILoggingEvent> {

    private static final CopyOnWriteArrayList<AsyncLoggingAppender> ACTIVE = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<Appender<ILoggingEvent>> nestedAppenders = new CopyOnWriteArrayList<>();
    private final AsyncLoggingMetrics metrics = new AsyncLoggingMetrics();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();

    private int queueCapacity = Constants.ASYNC_LOGGING_QUEUE_CAPACITY;
    private int highWaterMarkPercent = Constants.ASYNC_LOGGING_HIGH_WATER_MARK_PERCENT;
    private OverflowPolicy overflowPolicy = OverflowPolicy.DROP_LOW_PRIORITY;
    private int batchSize = Constants.ASYNC_LOGGING_BATCH_SIZE;
    private long drainTimeoutMillis = Constants.ASYNC_LOGGING_DRAIN_TIMEOUT_MILLIS;
    private long pollTimeoutMillis = Constants.ASYNC_LOGGING_POLL_TIMEOUT_MILLIS;
    private boolean includeCallerData = Constants.ASYNC_LOGGING_INCLUDE_CALLER_DATA;

    private volatile BlockingQueue<ILoggingEvent> queue;
    private volatile int highWaterMarkSize;
    private Thread worker;
    private Thread shutdownHook;

    static List<AsyncLoggingAppender> activeAppenders() {
        return List.copyOf(ACTIVE);
    }

    static void flushAllActive() {
        for (AsyncLoggingAppender appender : ACTIVE) {
            appender.flushQueuedEvents();
        }
    }

    static int totalQueueDepth() {
        int depth = 0;
        for (AsyncLoggingAppender appender : ACTIVE) {
            depth += appender.queueDepth();
        }
        return depth;
    }

    static int totalQueueCapacity() {
        int capacity = 0;
        for (AsyncLoggingAppender appender : ACTIVE) {
            capacity += appender.queueCapacity();
        }
        return capacity;
    }

    static double aggregateBufferUtilization() {
        int capacity = totalQueueCapacity();
        if (capacity <= 0) {
            return 0.0d;
        }
        return (double) totalQueueDepth() / (double) capacity;
    }

    static long totalDroppedLowPriority() {
        long total = 0L;
        for (AsyncLoggingAppender appender : ACTIVE) {
            total += appender.metrics.droppedLowPriorityCount();
        }
        return total;
    }

    static long totalDroppedQueueFull() {
        long total = 0L;
        for (AsyncLoggingAppender appender : ACTIVE) {
            total += appender.metrics.droppedQueueFullCount();
        }
        return total;
    }

    static long totalDroppedEvicted() {
        long total = 0L;
        for (AsyncLoggingAppender appender : ACTIVE) {
            total += appender.metrics.droppedEvictedCount();
        }
        return total;
    }

    static long totalDroppedShutdown() {
        long total = 0L;
        for (AsyncLoggingAppender appender : ACTIVE) {
            total += appender.metrics.droppedShutdownCount();
        }
        return total;
    }

    static long totalDropped() {
        long total = 0L;
        for (AsyncLoggingAppender appender : ACTIVE) {
            total += appender.metrics.droppedTotal();
        }
        return total;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public void setHighWaterMarkPercent(int highWaterMarkPercent) {
        this.highWaterMarkPercent = highWaterMarkPercent;
    }

    public void setOverflowPolicy(OverflowPolicy overflowPolicy) {
        if (overflowPolicy != null) {
            this.overflowPolicy = overflowPolicy;
        }
    }

    public void setOverflowPolicy(String overflowPolicy) {
        try {
            this.overflowPolicy = OverflowPolicy.fromConfiguredValue(overflowPolicy);
        } catch (IllegalArgumentException ex) {
            addError("Unknown overflow policy '" + overflowPolicy + "', using DROP_LOW_PRIORITY");
            this.overflowPolicy = OverflowPolicy.DROP_LOW_PRIORITY;
        }
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setDrainTimeoutMillis(long drainTimeoutMillis) {
        this.drainTimeoutMillis = drainTimeoutMillis;
    }

    public void setPollTimeoutMillis(long pollTimeoutMillis) {
        this.pollTimeoutMillis = pollTimeoutMillis;
    }

    public void setIncludeCallerData(boolean includeCallerData) {
        this.includeCallerData = includeCallerData;
    }

    public int queueDepth() {
        BlockingQueue<ILoggingEvent> current = queue;
        return current == null ? 0 : current.size();
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    public double bufferUtilization() {
        int capacity = queueCapacity;
        if (capacity <= 0) {
            return 0.0d;
        }
        return (double) queueDepth() / (double) capacity;
    }

    public AsyncLoggingMetrics metrics() {
        return metrics;
    }

    @Override
    public void start() {
        synchronized (lifecycleLock) {
            if (isStarted()) {
                return;
            }
            if (!hasNestedAppender()) {
                addError("AsyncLoggingAppender '" + name + "' has no nested appender to wrap");
                return;
            }
            if (queueCapacity < 1) {
                addError("queueCapacity must be >= 1");
                return;
            }
            batchSize = Math.clamp(batchSize, 1, queueCapacity);
            highWaterMarkPercent = Math.clamp(highWaterMarkPercent, 1, 100);
            drainTimeoutMillis = Math.max(0L, drainTimeoutMillis);
            pollTimeoutMillis = Math.max(1L, pollTimeoutMillis);

            this.queue = new ArrayBlockingQueue<>(queueCapacity);
            this.highWaterMarkSize = computeHighWaterMarkSize();
            this.running.set(true);
            super.start();

            this.worker = Thread.ofPlatform()
                    .name("async-logging-" + name)
                    .daemon(false)
                    .unstarted(this::runWorker);
            this.shutdownHook = new Thread(this::stopQuietly, "async-logging-shutdown-" + name);
            this.shutdownHook.setDaemon(true);
            try {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                addWarn("JVM is already shutting down; async logging shutdown hook was not registered");
            }

            this.worker.start();
            ACTIVE.addIfAbsent(this);
        }
    }

    @Override
    protected ReentryGuard buildReentryGuard() {
        return ReentryGuardFactory.makeGuard(ReentryGuardFactory.GuardType.THREAD_LOCAL);
    }

    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            if (!isStarted()) {
                return;
            }
            running.set(false);
            interruptWorker();
            joinWorker(Math.max(1L, drainTimeoutMillis));
            flushQueuedEvents();
            flushQueuedEvents();
            detachAndStopAllAppenders();
            ACTIVE.remove(this);
            removeShutdownHook();
            super.stop();
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null || !running.get()) {
            return;
        }
        if (isSelfGeneratedEvent(eventObject)) {
            return;
        }
        try {
            if (shouldDropTraceAndDebugAtHighWaterMark(eventObject)) {
                metrics.recordDroppedLowPriority();
                return;
            }
            snapshotEventForBackgroundThread(eventObject);
            enqueueWithoutFailingCaller(eventObject);
        } catch (Exception ex) {
            metrics.recordDroppedQueueFull();
            addError("Failed to enqueue logging event", ex);
        }
    }

    private void enqueueWithoutFailingCaller(ILoggingEvent event) {
        try {
            if (overflowPolicy == OverflowPolicy.BLOCK) {
                enqueueBlocking(event);
                return;
            }
            enqueueDroppingLowPriority(event);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            metrics.recordDroppedQueueFull();
        }
    }

    private void enqueueBlocking(ILoggingEvent event) throws InterruptedException {
        BlockingQueue<ILoggingEvent> current = queue;
        if (current == null || !running.get()) {
            metrics.recordDroppedQueueFull();
            return;
        }
        current.put(event);
    }

    private void enqueueDroppingLowPriority(ILoggingEvent event) {
        BlockingQueue<ILoggingEvent> current = queue;
        if (current == null) {
            metrics.recordDroppedQueueFull();
            return;
        }
        if (current.offer(event)) {
            return;
        }
        if (shouldPreserveWarnAndErrorByEvictingOldest(event) && evictOldestThenEnqueue(current, event)) {
            return;
        }
        metrics.recordDroppedQueueFull();
    }

    private boolean evictOldestThenEnqueue(BlockingQueue<ILoggingEvent> current, ILoggingEvent event) {
        ILoggingEvent discarded = current.poll();
        if (discarded != null) {
            metrics.recordDroppedEvicted();
        }
        return current.offer(event);
    }

    boolean shouldDropTraceAndDebugAtHighWaterMark(ILoggingEvent event) {
        if (!isQueueAtOrAboveHighWaterMark()) {
            return false;
        }
        return isTraceOrDebug(event);
    }

    boolean shouldPreserveWarnAndErrorByEvictingOldest(ILoggingEvent event) {
        Level level = event.getLevel();
        return level != null && level.toInt() >= Level.WARN_INT;
    }

    private boolean isQueueAtOrAboveHighWaterMark() {
        BlockingQueue<ILoggingEvent> current = queue;
        return current != null && current.size() >= highWaterMarkSize;
    }

    private static boolean isTraceOrDebug(ILoggingEvent event) {
        Level level = event.getLevel();
        return level != null && level.toInt() <= Level.DEBUG_INT;
    }

    private static boolean isSelfGeneratedEvent(ILoggingEvent event) {
        String loggerName = event.getLoggerName();
        return loggerName != null && loggerName.startsWith("com.ozerler.marble.logging");
    }

    private void snapshotEventForBackgroundThread(ILoggingEvent event) {
        try {
            event.prepareForDeferredProcessing();
            if (includeCallerData) {
                event.getCallerData();
            }
        } catch (RuntimeException ex) {
            addError("Could not snapshot MDC/caller data for async logging", ex);
        }
    }

    private void runWorker() {
        List<ILoggingEvent> batch = new ArrayList<>(batchSize);
        while (running.get()) {
            try {
                drainBatchThenAppend(batch);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                addError("Async logging worker failed while draining", ex);
            }
        }
        flushQueuedEvents();
    }

    private void drainBatchThenAppend(List<ILoggingEvent> batch) throws InterruptedException {
        BlockingQueue<ILoggingEvent> current = queue;
        if (current == null) {
            return;
        }
        ILoggingEvent first = current.poll(pollTimeoutMillis, TimeUnit.MILLISECONDS);
        if (first == null) {
            return;
        }
        batch.add(first);
        current.drainTo(batch, batchSize - 1);
        appendBatch(batch);
        batch.clear();
    }

    void flushQueuedEvents() {
        BlockingQueue<ILoggingEvent> current = queue;
        if (current == null || current.isEmpty()) {
            return;
        }
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(drainTimeoutMillis);
        List<ILoggingEvent> batch = new ArrayList<>(batchSize);
        while (!current.isEmpty() && System.nanoTime() < deadlineNanos) {
            current.drainTo(batch, batchSize);
            if (batch.isEmpty()) {
                break;
            }
            appendBatch(batch);
            batch.clear();
        }
        int leftover = current.size();
        if (leftover > 0) {
            current.clear();
            metrics.recordDroppedOnShutdown(leftover);
            addWarn("Dropped " + leftover + " log events after drain timeout of " + drainTimeoutMillis + "ms");
        }
    }

    private void appendBatch(List<ILoggingEvent> events) {
        for (ILoggingEvent event : events) {
            for (Appender<ILoggingEvent> destination : nestedAppenders) {
                try {
                    destination.doAppend(event);
                } catch (Exception ex) {
                    addError("Nested appender failed for logger " + event.getLoggerName(), ex);
                }
            }
        }
    }

    private int computeHighWaterMarkSize() {
        return Math.max(1, queueCapacity * highWaterMarkPercent / 100);
    }

    private boolean hasNestedAppender() {
        return !nestedAppenders.isEmpty();
    }

    private void interruptWorker() {
        Thread currentWorker = this.worker;
        if (currentWorker != null) {
            currentWorker.interrupt();
        }
    }

    private void joinWorker(long timeoutMillis) {
        Thread currentWorker = this.worker;
        if (currentWorker == null || !currentWorker.isAlive()) {
            return;
        }
        try {
            currentWorker.join(timeoutMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void removeShutdownHook() {
        Thread hook = this.shutdownHook;
        this.shutdownHook = null;
        if (hook == null) {
            return;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException ignored) {
            // JVM is already shutting down; the hook is running or has run.
        }
    }

    private void stopQuietly() {
        try {
            stop();
        } catch (Exception ignored) {
            // Shutdown hooks must not throw.
        }
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        if (newAppender != null) {
            nestedAppenders.addIfAbsent(newAppender);
        }
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return nestedAppenders.iterator();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String appenderName) {
        if (appenderName == null) {
            return null;
        }
        for (Appender<ILoggingEvent> nested : nestedAppenders) {
            if (appenderName.equals(nested.getName())) {
                return nested;
            }
        }
        return null;
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return appender != null && nestedAppenders.contains(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        for (Appender<ILoggingEvent> nested : nestedAppenders) {
            nested.stop();
        }
        nestedAppenders.clear();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return appender != null && nestedAppenders.remove(appender);
    }

    @Override
    public boolean detachAppender(String appenderName) {
        Appender<ILoggingEvent> nested = getAppender(appenderName);
        return nested != null && nestedAppenders.remove(nested);
    }
}
