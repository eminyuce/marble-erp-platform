package com.ozerler.marble.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UniqueCodesTest {

    @Test
    @DisplayName("allocate retries until the candidate is free")
    void allocateRetriesWhenExists() {
        Set<String> taken = new HashSet<>();
        taken.add("A-1");
        AtomicInteger n = new AtomicInteger(1);
        String code = UniqueCodes.allocate(() -> "A-" + n.getAndIncrement(), taken::contains);
        Assertions.assertThat(code).isEqualTo("A-2");
    }

    @Test
    @DisplayName("sequential numbers increment per prefix and year")
    void sequentialCodes() {
        Set<String> taken = new HashSet<>();
        String first = UniqueCodes.sequential("A-BLOK", 2026, 1, 3, taken::contains);
        taken.add(first);
        String second = UniqueCodes.sequential("A-BLOK", 2026, 1, 3, taken::contains);
        Assertions.assertThat(first).isEqualTo("A-BLOK-2026-001");
        Assertions.assertThat(second).isEqualTo("A-BLOK-2026-002");
    }

    @Test
    @DisplayName("yearly codes keep prefix and current year")
    void yearlyCodes() {
        String code = UniqueCodes.yearly("FWO", candidate -> false);
        Assertions.assertThat(code).startsWith("FWO-" + java.time.Year.now().getValue() + "-");
    }
}
