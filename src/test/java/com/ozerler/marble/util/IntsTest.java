package com.ozerler.marble.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntsTest {

    @Test
    @DisplayName("parseOrDefault treats blank and Tabulator undefined tokens as the fallback")
    void parseOrDefaultTreatsUndefinedAsFallback() {
        assertThat(Ints.parseOrDefault(null, 1)).isEqualTo(1);
        assertThat(Ints.parseOrDefault("  ", 25)).isEqualTo(25);
        assertThat(Ints.parseOrDefault("undefined", 1)).isEqualTo(1);
        assertThat(Ints.parseOrDefault("null", 10)).isEqualTo(10);
        assertThat(Ints.parseOrDefault("abc", 7)).isEqualTo(7);
        assertThat(Ints.parseOrDefault("3", 1)).isEqualTo(3);
    }

    @Test
    @DisplayName("parseLenientOrNull returns null for Tabulator placeholder tokens")
    void parseLenientOrNullReturnsNullForPlaceholders() {
        assertThat(Ints.parseLenientOrNull(null)).isNull();
        assertThat(Ints.parseLenientOrNull("")).isNull();
        assertThat(Ints.parseLenientOrNull("undefined")).isNull();
        assertThat(Ints.parseLenientOrNull("NULL")).isNull();
        assertThat(Ints.parseLenientOrNull("12")).isEqualTo(12);
        assertThatThrownBy(() -> Ints.parseLenientOrNull("nope"))
                .isInstanceOf(NumberFormatException.class);
    }
}
