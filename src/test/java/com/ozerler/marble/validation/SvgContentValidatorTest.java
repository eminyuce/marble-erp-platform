package com.ozerler.marble.validation;

import com.ozerler.marble.exception.FileValidationException;
import com.ozerler.marble.support.TestFiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SvgContentValidatorTest {

    private final SvgContentValidator validator = new SvgContentValidator();

    @Test
    @DisplayName("valid SVG without script or external references is accepted")
    void validSvg_Accepted() {
        validator.validate(TestFiles.svg());
    }

    @Test
    @DisplayName("SVG with embedded script is rejected")
    void scriptSvg_Rejected() {
        assertThatThrownBy(() -> validator.validate(TestFiles.maliciousSvg()))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("forbidden element");
    }

    @Test
    @DisplayName("SVG with onload handler is rejected")
    void eventHandlerSvg_Rejected() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>".getBytes();
        assertThatThrownBy(() -> validator.validate(svg))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("SVG with external image href is rejected")
    void externalHref_Rejected() {
        byte[] svg = """
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                  <image xlink:href="https://evil.example/x.png"/>
                </svg>
                """.getBytes();
        assertThatThrownBy(() -> validator.validate(svg))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("SVG with javascript URL is rejected")
    void javascriptUrl_Rejected() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><a href=\"javascript:alert(1)\"/></svg>".getBytes();
        assertThatThrownBy(() -> validator.validate(svg))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("non-XML content is rejected")
    void malformed_Rejected() {
        assertThatThrownBy(() -> validator.validate("not-xml".getBytes()))
                .isInstanceOf(FileValidationException.class);
    }
}
