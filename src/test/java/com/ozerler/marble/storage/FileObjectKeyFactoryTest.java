package com.ozerler.marble.storage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileObjectKeyFactoryTest {

    private final FileObjectKeyFactory factory = new FileObjectKeyFactory();

    @Test
    @DisplayName("image keys use images/{entity}/{id}/{uuid}.ext")
    void imageKey() {
        String key = factory.create("BLOCK", 42L, "png");
        Assertions.assertThat(key).startsWith("images/block/42/");
        Assertions.assertThat(key).endsWith(".png");
        Assertions.assertThat(key).doesNotContain("photo.png");
    }

    @Test
    @DisplayName("pending uploads use the pending folder when entityId is null")
    void pendingKey() {
        String key = factory.create("BLOCK", null, "pdf");
        Assertions.assertThat(key).startsWith("documents/block/pending/");
        Assertions.assertThat(key).endsWith(".pdf");
    }

    @Test
    @DisplayName("SVG keys use the svg/ prefix")
    void svgKey() {
        String key = factory.create("INVOICE", 9L, "svg");
        Assertions.assertThat(key).startsWith("svg/invoice/9/");
        Assertions.assertThat(key).endsWith(".svg");
    }
}
