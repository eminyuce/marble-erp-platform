package com.ozerler.marble.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueCodesTest {

    @Test
    @DisplayName("allocate retries until the candidate is free")
    void allocateRetriesWhenExists() {
        Set<String> taken = new HashSet<>();
        taken.add("A-1");
        AtomicInteger n = new AtomicInteger(1);
        String code = UniqueCodes.allocate(() -> "A-" + n.getAndIncrement(), taken::contains);
        assertThat(code).isEqualTo("A-2");
    }

    @Test
    @DisplayName("sequential numbers increment per prefix and year")
    void sequentialCodes() {
        Set<String> taken = new HashSet<>();
        String first = UniqueCodes.sequential("A-BLOK", 2026, 1, 3, taken::contains);
        taken.add(first);
        String second = UniqueCodes.sequential("A-BLOK", 2026, 1, 3, taken::contains);
        assertThat(first).isEqualTo("A-BLOK-2026-001");
        assertThat(second).isEqualTo("A-BLOK-2026-002");
    }
}
