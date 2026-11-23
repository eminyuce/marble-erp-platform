package com.ozerler.marble.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class FilenamesTest {

    @Test
    void extensionExtractsFromLastDot() {
        Assertions.assertThat(Filenames.extension("photo.JPEG")).isEqualTo(".JPEG");
        Assertions.assertThat(Filenames.extension("archive.tar.gz")).isEqualTo(".gz");
        Assertions.assertThat(Filenames.extension("noext")).isEmpty();
        Assertions.assertThat(Filenames.extension(null)).isEmpty();
    }

    @Test
    void detectsPathTraversal() {
        Assertions.assertThat(Filenames.containsPathTraversal("../secret.txt")).isTrue();
        Assertions.assertThat(Filenames.containsPathTraversal("safe-name.png")).isFalse();
        Assertions.assertThat(Filenames.containsPathTraversal(null)).isFalse();
    }
}
