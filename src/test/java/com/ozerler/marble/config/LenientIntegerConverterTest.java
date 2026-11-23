package com.ozerler.marble.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LenientIntegerConverterTest {

    private final LenientIntegerConverter converter = new LenientIntegerConverter();

    @Test
    @DisplayName("Tabulator undefined/null page tokens convert to null so defaultValue applies")
    void undefinedTokensBecomeNull() {
        Assertions.assertThat(converter.convert("undefined")).isNull();
        Assertions.assertThat(converter.convert("null")).isNull();
        Assertions.assertThat(converter.convert(" ")).isNull();
        Assertions.assertThat(converter.convert("2")).isEqualTo(2);
    }

    @Test
    @DisplayName("non-numeric values still fail conversion")
    void garbageStillFails() {
        Assertions.assertThatThrownBy(() -> converter.convert("nope"))
                .isInstanceOf(NumberFormatException.class);
    }
}
