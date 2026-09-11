package com.ozerler.marble.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FilenamesTest {

    @Test
    void extensionExtractsFromLastDot() {
        assertThat(Filenames.extension("photo.JPEG")).isEqualTo(".JPEG");
        assertThat(Filenames.extension("archive.tar.gz")).isEqualTo(".gz");
        assertThat(Filenames.extension("noext")).isEmpty();
        assertThat(Filenames.extension(null)).isEmpty();
    }

    @Test
    void detectsPathTraversal() {
        assertThat(Filenames.containsPathTraversal("../secret.txt")).isTrue();
        assertThat(Filenames.containsPathTraversal("safe-name.png")).isFalse();
        assertThat(Filenames.containsPathTraversal(null)).isFalse();
    }
}
