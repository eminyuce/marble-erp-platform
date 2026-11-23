package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HtmlNotesEditorTest {

    @Test
    @DisplayName("Notes editor auto-binds notes and saleNotes fields and exports window helpers")
    void editorSetupAutoBindsNotesFields() throws Exception {
        String script = read("/static/js/editor-setup.js");

        Assertions.assertThat(script).contains("function isNotesFieldName(");
        Assertions.assertThat(script).contains("name === \"notes\" || name === \"saleNotes\"");
        Assertions.assertThat(script).contains("function enhanceHtmlNotes(");
        Assertions.assertThat(script).contains("window.initDualEditor = initDualEditor");
        Assertions.assertThat(script).contains("window.enhanceHtmlNotes = enhanceHtmlNotes");
        Assertions.assertThat(script).contains("contenteditable");
    }

    @Test
    @DisplayName("Project create/edit and block sell pages host HTML note editors")
    void projectAndSellPagesHostHtmlNotesEditors() throws Exception {
        String projectForm = read("/templates/erp/projects/form.html");
        String sellForm = read("/templates/erp/blocks/sell.html");
        String blockForm = read("/templates/erp/blocks/form.html");
        String layout = read("/templates/layout/base.html");

        Assertions.assertThat(projectForm).contains("data-html-notes");
        Assertions.assertThat(projectForm).contains("name=\"notes\"");
        Assertions.assertThat(sellForm).contains("data-html-notes");
        Assertions.assertThat(sellForm).contains("name=\"saleNotes\"");
        Assertions.assertThat(sellForm).doesNotContain("<textarea id=\"saleNotes\"");
        Assertions.assertThat(blockForm).contains("data-html-notes");
        Assertions.assertThat(blockForm).contains("data-hidden-id=\"notes-hidden-input\"");
        Assertions.assertThat(layout).contains("@{/js/editor-setup.js(v=${assetVersion})}");
    }

    private static String read(String resource) throws Exception {
        try (var in = HtmlNotesEditorTest.class.getResourceAsStream(resource)) {
            Assertions.assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
