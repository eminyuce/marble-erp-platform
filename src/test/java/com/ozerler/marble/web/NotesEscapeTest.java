package com.ozerler.marble.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NotesEscapeTest {

    @Test
    @DisplayName("order notes are escaped text, not raw HTML")
    void orderNotesAreNotRawHtml() throws Exception {
        String workshop = Files.readString(Path.of("src/main/resources/templates/erp/workshop/detail.html"));
        String production = Files.readString(Path.of("src/main/resources/templates/erp/production/detail.html"));
        assertThat(workshop).contains("th:text=\"${order.notes}\"").doesNotContain("th:utext");
        assertThat(production).contains("th:text=\"${order.notes}\"").doesNotContain("th:utext");
    }
}
