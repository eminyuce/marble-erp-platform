package com.ozerler.marble.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErpToolbarLayoutTest {

    private static final List<String> FILTER_PAGES = List.of(
            "src/main/resources/templates/admin/definitions/machines.html",
            "src/main/resources/templates/admin/definitions/stock-locations.html",
            "src/main/resources/templates/admin/definitions/cost-centers.html",
            "src/main/resources/templates/admin/definitions/customers.html",
            "src/main/resources/templates/admin/definitions/suppliers.html"
    );

    @Test
    @DisplayName("List toolbar CSS keeps create, search, filter and export on one row")
    void toolbarCssUsesSingleRowGrid() throws Exception {
        String css = Files.readString(Path.of("frontend/src/input.css")).replace("\r\n", "\n");
        Assertions.assertThat(css).contains("grid-template-columns: auto minmax(8rem, 1fr) minmax(0, max-content)");
        Assertions.assertThat(css).contains(".erp-toolbar-filter");
        Assertions.assertThat(css).contains("min-width: 16rem");
        Assertions.assertThat(css).contains(".erp-search-form {\n  min-width: 0;");
        Assertions.assertThat(css).doesNotContain("flex: 1 1 220px");
        Assertions.assertThat(css).doesNotContain(".erp-toolbar-actions {\n    width: 100%;");
    }

    @Test
    @DisplayName("Search control is compact and definition filters share toolbar styling")
    void searchAndFiltersStayCompact() throws Exception {
        String chrome = Files.readString(Path.of("src/main/resources/templates/fragments/list-chrome.html"));
        Assertions.assertThat(chrome).contains("aria-label=\"Ara\"");
        Assertions.assertThat(chrome).contains("class=\"sr-only\">Arama<");
        Assertions.assertThat(chrome).doesNotContain("<span>Arama</span>");

        for (String page : FILTER_PAGES) {
            String source = Files.readString(Path.of(page));
            Assertions.assertThat(source)
                    .as("%s should use the compact toolbar filter", page)
                    .contains("class=\"erp-toolbar-filter\"");
        }

        String searchableCss = Files.readString(Path.of("src/main/resources/static/css/searchable-select.css"));
        Assertions.assertThat(searchableCss).contains(".erp-ss--toolbar");
        Assertions.assertThat(searchableCss).contains("min-width: 16rem");
        String searchableJs = Files.readString(Path.of("src/main/resources/static/js/searchable-select.js"));
        Assertions.assertThat(searchableJs).contains("erp-ss--toolbar");
        Assertions.assertThat(searchableJs).contains("erp-toolbar-filter");
    }

    @Test
    @DisplayName("Expenses toolbar keeps create, search, period filter and export on one row")
    void expensesToolbarStaysOnSingleRow() throws Exception {
        String expensesHtml = Files.readString(Path.of("src/main/resources/templates/erp/expenses/index.html"));
        Assertions.assertThat(expensesHtml).contains("class=\"erp-toolbar-actions\"");
        Assertions.assertThat(expensesHtml).contains("class=\"erp-toolbar-date\"");
        Assertions.assertThat(expensesHtml).contains("id=\"expenses-period-filter\"");
        Assertions.assertThat(expensesHtml).contains("exportDropdown('expenses-table', 'giderler')");

        String css = Files.readString(Path.of("frontend/src/input.css"));
        Assertions.assertThat(css).contains(".erp-toolbar-date");
    }
}
