package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectGridActionsTest {

    @Test
    @DisplayName("Projects grid operations menu includes an edit link for every project")
    void operationsColumnIncludesEditLink() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/projects/index.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("{icon: 'edit-3', label: 'Düzenle', href: '/projects/' + row.id + '/edit'}");
    }

    @Test
    @DisplayName("Project form hosts şantiye notes as an HTML editor hidden field")
    void projectFormUsesHtmlNotesEditor() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/projects/form.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("data-html-notes");
        assertThat(html).contains("id=\"project-notes-hidden\"");
        assertThat(html).contains("name=\"notes\"");
        assertThat(html).doesNotContain("<textarea id=\"notes\"");
    }
}
