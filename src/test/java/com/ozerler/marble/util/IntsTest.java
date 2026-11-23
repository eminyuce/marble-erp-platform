package com.ozerler.marble.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IntsTest {

    @Test
    @DisplayName("parseOrDefault treats blank and Tabulator undefined tokens as the fallback")
    void parseOrDefaultTreatsUndefinedAsFallback() {
        Assertions.assertThat(Ints.parseOrDefault(null, 1)).isEqualTo(1);
        Assertions.assertThat(Ints.parseOrDefault("  ", 25)).isEqualTo(25);
        Assertions.assertThat(Ints.parseOrDefault("undefined", 1)).isEqualTo(1);
        Assertions.assertThat(Ints.parseOrDefault("null", 10)).isEqualTo(10);
        Assertions.assertThat(Ints.parseOrDefault("abc", 7)).isEqualTo(7);
        Assertions.assertThat(Ints.parseOrDefault("3", 1)).isEqualTo(3);
    }

    @Test
    @DisplayName("parseLenientOrNull returns null for Tabulator placeholder tokens")
    void parseLenientOrNullReturnsNullForPlaceholders() {
        Assertions.assertThat(Ints.parseLenientOrNull(null)).isNull();
        Assertions.assertThat(Ints.parseLenientOrNull("")).isNull();
        Assertions.assertThat(Ints.parseLenientOrNull("undefined")).isNull();
        Assertions.assertThat(Ints.parseLenientOrNull("NULL")).isNull();
        Assertions.assertThat(Ints.parseLenientOrNull("12")).isEqualTo(12);
        Assertions.assertThatThrownBy(() -> Ints.parseLenientOrNull("nope"))
                .isInstanceOf(NumberFormatException.class);
    }
}
