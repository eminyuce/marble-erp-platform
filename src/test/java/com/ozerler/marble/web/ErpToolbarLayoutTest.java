package com.ozerler.marble.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        String css = Files.readString(Path.of("frontend/src/input.css"));
        assertThat(css).contains("grid-template-columns: auto minmax(8rem, 1fr) minmax(0, max-content)");
        assertThat(css).contains(".erp-toolbar-filter");
        assertThat(css).contains("min-width: 16rem");
        assertThat(css).contains(".erp-search-form {\n  min-width: 0;");
        assertThat(css).doesNotContain("flex: 1 1 220px");
        assertThat(css).doesNotContain(".erp-toolbar-actions {\n    width: 100%;");
    }

    @Test
    @DisplayName("Search control is compact and definition filters share toolbar styling")
    void searchAndFiltersStayCompact() throws Exception {
        String chrome = Files.readString(Path.of("src/main/resources/templates/fragments/list-chrome.html"));
        assertThat(chrome).contains("aria-label=\"Ara\"");
        assertThat(chrome).contains("class=\"sr-only\">Arama<");
        assertThat(chrome).doesNotContain("<span>Arama</span>");

        for (String page : FILTER_PAGES) {
            String source = Files.readString(Path.of(page));
            assertThat(source)
                    .as("%s should use the compact toolbar filter", page)
                    .contains("class=\"erp-toolbar-filter\"");
        }

        String searchableCss = Files.readString(Path.of("src/main/resources/static/css/searchable-select.css"));
        assertThat(searchableCss).contains(".erp-ss--toolbar");
        assertThat(searchableCss).contains("min-width: 16rem");
        String searchableJs = Files.readString(Path.of("src/main/resources/static/js/searchable-select.js"));
        assertThat(searchableJs).contains("erp-ss--toolbar");
        assertThat(searchableJs).contains("erp-toolbar-filter");
    }

    @Test
    @DisplayName("Expenses toolbar keeps create, search, period filter and export on one row")
    void expensesToolbarStaysOnSingleRow() throws Exception {
        String expensesHtml = Files.readString(Path.of("src/main/resources/templates/erp/expenses/index.html"));
        assertThat(expensesHtml).contains("class=\"erp-toolbar-actions\"");
        assertThat(expensesHtml).contains("class=\"erp-toolbar-date\"");
        assertThat(expensesHtml).contains("id=\"expenses-period-filter\"");
        assertThat(expensesHtml).contains("exportDropdown('expenses-table', 'giderler')");

        String css = Files.readString(Path.of("frontend/src/input.css"));
        assertThat(css).contains(".erp-toolbar-date");
    }
}
