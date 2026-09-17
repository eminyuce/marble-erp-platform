package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlNotesEditorTest {

    @Test
    @DisplayName("Notes editor auto-binds notes and saleNotes fields and exports window helpers")
    void editorSetupAutoBindsNotesFields() throws Exception {
        String script = read("/static/js/editor-setup.js");

        assertThat(script).contains("function isNotesFieldName(");
        assertThat(script).contains("name === \"notes\" || name === \"saleNotes\"");
        assertThat(script).contains("function enhanceHtmlNotes(");
        assertThat(script).contains("window.initDualEditor = initDualEditor");
        assertThat(script).contains("window.enhanceHtmlNotes = enhanceHtmlNotes");
        assertThat(script).contains("contenteditable");
    }

    @Test
    @DisplayName("Project create/edit and block sell pages host HTML note editors")
    void projectAndSellPagesHostHtmlNotesEditors() throws Exception {
        String projectForm = read("/templates/erp/projects/form.html");
        String sellForm = read("/templates/erp/blocks/sell.html");
        String blockForm = read("/templates/erp/blocks/form.html");
        String layout = read("/templates/layout/base.html");

        assertThat(projectForm).contains("data-html-notes");
        assertThat(projectForm).contains("name=\"notes\"");
        assertThat(sellForm).contains("data-html-notes");
        assertThat(sellForm).contains("name=\"saleNotes\"");
        assertThat(sellForm).doesNotContain("<textarea id=\"saleNotes\"");
        assertThat(blockForm).contains("data-html-notes");
        assertThat(blockForm).contains("data-hidden-id=\"notes-hidden-input\"");
        assertThat(layout).contains("@{/js/editor-setup.js(v=${assetVersion})}");
    }

    private static String read(String resource) throws Exception {
        try (var in = HtmlNotesEditorTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
