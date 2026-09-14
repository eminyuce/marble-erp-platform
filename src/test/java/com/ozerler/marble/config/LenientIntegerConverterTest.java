package com.ozerler.marble.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LenientIntegerConverterTest {

    private final LenientIntegerConverter converter = new LenientIntegerConverter();

    @Test
    @DisplayName("Tabulator undefined/null page tokens convert to null so defaultValue applies")
    void undefinedTokensBecomeNull() {
        assertThat(converter.convert("undefined")).isNull();
        assertThat(converter.convert("null")).isNull();
        assertThat(converter.convert(" ")).isNull();
        assertThat(converter.convert("2")).isEqualTo(2);
    }

    @Test
    @DisplayName("non-numeric values still fail conversion")
    void garbageStillFails() {
        assertThatThrownBy(() -> converter.convert("nope"))
                .isInstanceOf(NumberFormatException.class);
    }
}
