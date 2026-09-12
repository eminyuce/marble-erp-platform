package com.ozerler.marble.logging;

import com.ozerler.marble.common.Constants;

final class AsyncLoggingDefaults {

    static final int QUEUE_CAPACITY = Constants.ASYNC_LOGGING_QUEUE_CAPACITY;
    static final int HIGH_WATER_MARK_PERCENT = Constants.ASYNC_LOGGING_HIGH_WATER_MARK_PERCENT;
    static final int BATCH_SIZE = Constants.ASYNC_LOGGING_BATCH_SIZE;
    static final long DRAIN_TIMEOUT_MILLIS = Constants.ASYNC_LOGGING_DRAIN_TIMEOUT_MILLIS;
    static final long POLL_TIMEOUT_MILLIS = Constants.ASYNC_LOGGING_POLL_TIMEOUT_MILLIS;
    static final boolean INCLUDE_CALLER_DATA = Constants.ASYNC_LOGGING_INCLUDE_CALLER_DATA;

    private AsyncLoggingDefaults() {
    }
}
