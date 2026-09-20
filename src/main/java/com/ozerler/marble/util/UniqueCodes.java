package com.ozerler.marble.util;

import java.time.Year;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Unique business-code allocator with retry when a candidate already exists.
 */
public final class UniqueCodes {

    private static final int MAX_ATTEMPTS = 10_000;
    private static final int DEFAULT_TIME_SUFFIX_MODULUS = 100_000;

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
        throw new IllegalStateException("Could not allocate a unique document code after " + MAX_ATTEMPTS + " attempts");
    }

    public static String yearly(String prefix, Predicate<String> exists) {
        return yearly(prefix, DEFAULT_TIME_SUFFIX_MODULUS, exists);
    }

    public static String yearly(String prefix, int modulus, Predicate<String> exists) {
        int year = Year.now().getValue();
        int safeModulus = Math.max(modulus, 1);
        return allocate(() -> String.format("%s-%d-%d", prefix, year, System.nanoTime() % safeModulus), exists);
    }

    public static String sequential(String prefix, int year, int startSequence, int width, Predicate<String> exists) {
        int[] sequence = {Math.max(startSequence, 1)};
        int paddedWidth = Math.max(width, 1);
        String format = "%s-%d-%0" + paddedWidth + "d";
        return allocate(() -> String.format(format, prefix, year, sequence[0]++), exists);
    }
}
