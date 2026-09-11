package com.ozerler.marble.logging;

final class AsyncLoggingDefaults {

    static final int QUEUE_CAPACITY = 8192;
    static final int HIGH_WATER_MARK_PERCENT = 90;
    static final int BATCH_SIZE = 256;
    static final long DRAIN_TIMEOUT_MILLIS = 5_000L;
    static final long POLL_TIMEOUT_MILLIS = 100L;
    static final boolean INCLUDE_CALLER_DATA = false;

    private AsyncLoggingDefaults() {
    }
}
