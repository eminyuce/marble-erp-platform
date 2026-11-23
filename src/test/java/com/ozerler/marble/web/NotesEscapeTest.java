package com.ozerler.marble.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotesEscapeTest {

    @Test
    @DisplayName("order notes are escaped text, not raw HTML")
    void orderNotesAreNotRawHtml() throws Exception {
        String workshop = Files.readString(Path.of("src/main/resources/templates/erp/workshop/detail.html"));
        String production = Files.readString(Path.of("src/main/resources/templates/erp/production/detail.html"));
        Assertions.assertThat(workshop).contains("th:text=\"${order.notes}\"").doesNotContain("th:utext");
        Assertions.assertThat(production).contains("th:text=\"${order.notes}\"").doesNotContain("th:utext");
    }
}
