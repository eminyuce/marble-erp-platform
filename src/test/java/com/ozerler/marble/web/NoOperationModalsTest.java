package com.ozerler.marble.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoOperationModalsTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path STATIC_JS = Path.of("src/main/resources/static/js");

    @Test
    @DisplayName("operation UIs must not use create/edit/preview/gallery modals")
    void templatesDoNotOpenOperationModals() throws Exception {
        List<String> hits = new ArrayList<>();
        for (Path file : htmlAndJsFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            for (String forbidden : List.of(
                    "previewModalOpen",
                    "openPreview(",
                    "galleryOpen",
                    "openGallery(",
                    "openCreateModal",
                    "openEditModal",
                    "x-show=\"modalOpen\"",
                    "id=\"move-block-dialog\"",
                    "id=\"transfer-block-dialog\"",
                    "id=\"sell-block-dialog\"",
                    "openMoveBlock",
                    "openTransferBlock"
            )) {
                if (source.contains(forbidden)) {
                    hits.add(file + " contains " + forbidden);
                }
            }
        }
        Assertions.assertThat(hits).isEmpty();
    }

    @Test
    @DisplayName("email preview and block photos are dedicated pages")
    void dedicatedPagesExistForFormerModals() throws Exception {
        String settings = Files.readString(TEMPLATES.resolve("admin/settings/index.html"), StandardCharsets.UTF_8);
        String preview =
                Files.readString(
                        TEMPLATES.resolve("admin/settings/template-preview.html"),
                        StandardCharsets.UTF_8);
        String detail = Files.readString(TEMPLATES.resolve("erp/blocks/detail.html"), StandardCharsets.UTF_8);
        String photos = Files.readString(TEMPLATES.resolve("erp/blocks/photos.html"), StandardCharsets.UTF_8);
        String usersIndex = Files.readString(TEMPLATES.resolve("admin/users/index.html"), StandardCharsets.UTF_8);
        String usersGrid = Files.readString(STATIC_JS.resolve("users-grid.js"), StandardCharsets.UTF_8);

        Assertions.assertThat(settings)
                .contains("/admin/settings/templates/{id}/preview")
                .doesNotContain("x-html=\"previewHtml\"");
        Assertions.assertThat(preview)
                .contains("layout:decorate=\"~{layout/base}\"")
                .contains("id=\"email-preview-frame\"")
                .contains("erp-ops-info-callout")
                .contains("Örnek değişkenler");
        Assertions.assertThat(detail)
                .contains("/blocks/' + ${block.id} + '/photos")
                .contains("deleteModalOpen")
                .doesNotContain("x-teleport");
        Assertions.assertThat(photos)
                .contains("id=\"photo-main\"")
                .contains("id=\"photo-prev\"")
                .contains("Blok detayına dön");
        Assertions.assertThat(usersIndex).contains("id=\"delete-user-dialog\"");
        Assertions.assertThat(usersGrid)
                .contains("openDeleteUser(")
                .doesNotContain("if (!confirm(");
    }

    private static List<Path> htmlAndJsFiles() throws Exception {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(TEMPLATES)) {
            walk.filter(path -> path.toString().endsWith(".html")).forEach(files::add);
        }
        try (Stream<Path> walk = Files.walk(STATIC_JS)) {
            walk.filter(path -> path.toString().endsWith(".js"))
                    .filter(path -> !path.getFileName().toString().contains("vendor"))
                    .forEach(files::add);
        }
        return files;
    }
}
