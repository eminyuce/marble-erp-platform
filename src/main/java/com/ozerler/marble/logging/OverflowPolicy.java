package com.ozerler.marble.logging;

import java.util.Locale;

/**
 * Queue overflow strategy used when the async log buffer is under pressure.
 *
 * <ul>
 *   <li>{@link #DROP_LOW_PRIORITY} — never blocks callers. TRACE/DEBUG are discarded
 *       once the high-water mark is reached; INFO is discarded only when the queue
 *       is completely full; WARN/ERROR evict the oldest event rather than being lost.</li>
 *   <li>{@link #BLOCK} — TRACE/DEBUG are still dropped at the high-water mark so
 *       debug storms cannot stall business threads; INFO and above {@code put()}
 *       until a slot is free.</li>
 * </ul>
 */
public enum OverflowPolicy {

    DROP_LOW_PRIORITY,
    BLOCK;

    public static OverflowPolicy fromConfiguredValue(String value) {
        if (value == null || value.isBlank()) {
            return DROP_LOW_PRIORITY;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("DROP".equals(normalized) || "DROPPING".equals(normalized)) {
            return DROP_LOW_PRIORITY;
        }
        if ("BLOCKING".equals(normalized)) {
            return BLOCK;
        }
        return OverflowPolicy.valueOf(normalized);
    }
}
