package com.ozerler.marble.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AlpinePageScriptTest {

    @Test
    @DisplayName("Alpine starts after page scripts, and x-data does not embed raw Thymeleaf")
    void alpineIsDeferredAndProjectFiltersAreAttributes() throws Exception {
        String layout = Files.readString(Path.of("src/main/resources/templates/layout/base.html"));
        String projects = Files.readString(Path.of("src/main/resources/templates/erp/projects/index.html"));
        String form = Files.readString(Path.of("src/main/resources/templates/erp/projects/form.html"));
        String transactions = Files.readString(Path.of(
                "src/main/resources/templates/admin/definitions/customer-transactions.html"));

        assertThat(layout).contains("defer th:src=\"@{/vendor/alpine-collapse.min.js}\"");
        assertThat(layout).contains("defer th:src=\"@{/vendor/alpine.min.js}\"");
        assertThat(projects).doesNotContain("[[${");
        assertThat(form).doesNotContain("[[${");
        assertThat(transactions).doesNotContain("[[${");
        assertThat(projects).contains("data-customer-filter");
        assertThat(form).contains("data-cari-mode");
    }
}
