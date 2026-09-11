package com.ozerler.marble.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StringsTest {

    @Test
    void lowerTurkishHandlesNullAndDottedI() {
        assertThat(Strings.lowerTurkish(null)).isEmpty();
        assertThat(Strings.lowerTurkish("İMALAT")).isEqualTo("imalat");
    }

    @Test
    void joinDistinctSkipsBlanksAndDuplicates() {
        assertThat(Strings.joinDistinct(" · ", "PRJ-1", "Villa")).isEqualTo("PRJ-1 · Villa");
        assertThat(Strings.joinDistinct(" · ", "PRJ-1", "PRJ-1")).isEqualTo("PRJ-1");
        assertThat(Strings.joinDistinct(" · ", null, "Villa")).isEqualTo("Villa");
        assertThat(Strings.joinDistinct(" · ", "PRJ-1", "  ")).isEqualTo("PRJ-1");
    }

    @Test
    void replacePlaceholdersSubstitutesTokens() {
        assertThat(Strings.replacePlaceholders("Merhaba {{name}}", Map.of("name", "Emin")))
                .isEqualTo("Merhaba Emin");
        assertThat(Strings.replacePlaceholders(null, Map.of("name", "Emin"))).isEmpty();
        assertThat(Strings.replacePlaceholders("Sabit", null)).isEqualTo("Sabit");
    }
}
