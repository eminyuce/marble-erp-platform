package com.ozerler.marble.util;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Unique business-code allocator with retry when a candidate already exists.
 */
public final class UniqueCodes {

    private static final int MAX_ATTEMPTS = 10_000;

    private UniqueCodes() {
    }

    public static String allocate(Supplier<String> candidateFactory, Predicate<String> exists) {
        Objects.requireNonNull(candidateFactory, "candidateFactory");
        Objects.requireNonNull(exists, "exists");
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = candidateFactory.get();
            if (candidate != null && !candidate.isBlank() && !exists.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique document code");
    }

    public static String sequential(String prefix, int year, int startSequence, int width, Predicate<String> exists) {
        AtomicInteger sequence = new AtomicInteger(Math.max(startSequence, 1));
        String format = "%s-%d-%0" + Math.max(width, 1) + "d";
        return allocate(() -> String.format(format, prefix, year, sequence.getAndIncrement()), exists);
    }
}
