package com.ozerler.marble.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlpinePageScriptTest {

    @Test
    @DisplayName("Alpine starts after page scripts, and x-data does not embed raw Thymeleaf")
    void alpineIsDeferredAndProjectFiltersAreAttributes() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/layout/base.html"));
        String projects = Files.readString(Path.of("src/main/resources/templates/erp/projects/index.html"));
        String form = Files.readString(Path.of("src/main/resources/templates/erp/projects/form.html"));
        String transactions = Files.readString(Path.of(
                "src/main/resources/templates/admin/definitions/customer-transactions.html"));

        Assertions.assertThat(layout).contains("defer th:src=\"@{/vendor/alpine-collapse.min.js}\"");
        Assertions.assertThat(layout).contains("defer th:src=\"@{/vendor/alpine.min.js}\"");
        Assertions.assertThat(projects).doesNotContain("[[${");
        Assertions.assertThat(form).doesNotContain("[[${");
        Assertions.assertThat(transactions).doesNotContain("[[${");
        Assertions.assertThat(projects).contains("data-customer-filter");
        Assertions.assertThat(form).contains("data-cari-mode");
    }
}
