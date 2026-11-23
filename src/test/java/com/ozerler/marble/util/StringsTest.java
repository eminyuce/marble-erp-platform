package com.ozerler.marble.util;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class StringsTest {

    @Test
    void lowerTurkishHandlesNullAndDottedI() {
        Assertions.assertThat(Strings.lowerTurkish(null)).isEmpty();
        Assertions.assertThat(Strings.lowerTurkish("İMALAT")).isEqualTo("imalat");
    }

    @Test
    void joinDistinctSkipsBlanksAndDuplicates() {
        Assertions.assertThat(Strings.joinDistinct(" · ", "PRJ-1", "Villa")).isEqualTo("PRJ-1 · Villa");
        Assertions.assertThat(Strings.joinDistinct(" · ", "PRJ-1", "PRJ-1")).isEqualTo("PRJ-1");
        Assertions.assertThat(Strings.joinDistinct(" · ", null, "Villa")).isEqualTo("Villa");
        Assertions.assertThat(Strings.joinDistinct(" · ", "PRJ-1", "  ")).isEqualTo("PRJ-1");
    }

    @Test
    void replacePlaceholdersSubstitutesTokens() {
        Assertions.assertThat(Strings.replacePlaceholders("Merhaba {{name}}", Map.of("name", "Emin")))
                .isEqualTo("Merhaba Emin");
        Assertions.assertThat(Strings.replacePlaceholders(null, Map.of("name", "Emin"))).isEmpty();
        Assertions.assertThat(Strings.replacePlaceholders("Sabit", null)).isEqualTo("Sabit");
    }
}
