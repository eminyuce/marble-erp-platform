package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectGridActionsTest {

    @Test
    @DisplayName("Projects grid operations menu includes an edit link for every project")
    void operationsColumnIncludesEditLink() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/projects/index.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("fragments/data-grid :: shell");
        Assertions.assertThat(html).contains("confirmDeleteProject");
    }

    @Test
    @DisplayName("Project form hosts şantiye notes as an HTML editor hidden field")
    void projectFormUsesHtmlNotesEditor() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/projects/form.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("data-html-notes");
        Assertions.assertThat(html).contains("id=\"project-notes-hidden\"");
        Assertions.assertThat(html).contains("name=\"notes\"");
        Assertions.assertThat(html).doesNotContain("<textarea id=\"notes\"");
    }
}
