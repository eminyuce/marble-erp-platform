package com.ozerler.marble.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileObjectKeyFactoryTest {

    private final FileObjectKeyFactory factory = new FileObjectKeyFactory();

    @Test
    @DisplayName("image keys use images/{entity}/{id}/{uuid}.ext")
    void imageKey() {
        String key = factory.create("BLOCK", 42L, "png");
        assertThat(key).startsWith("images/block/42/");
        assertThat(key).endsWith(".png");
        assertThat(key).doesNotContain("photo.png");
    }

    @Test
    @DisplayName("pending uploads use the pending folder when entityId is null")
    void pendingKey() {
        String key = factory.create("BLOCK", null, "pdf");
        assertThat(key).startsWith("documents/block/pending/");
        assertThat(key).endsWith(".pdf");
    }

    @Test
    @DisplayName("SVG keys use the svg/ prefix")
    void svgKey() {
        String key = factory.create("INVOICE", 9L, "svg");
        assertThat(key).startsWith("svg/invoice/9/");
        assertThat(key).endsWith(".svg");
    }
}
