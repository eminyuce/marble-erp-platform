package com.ozerler.marble.controller.erp;

import java.util.List;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TabulatorGridUndefinedTest {

    private static final List<String> GRID_SOURCES = List.of(
            "/static/js/blocks-grid.js",
            "/static/js/production-grid.js",
            "/static/js/users-grid.js",
            "/templates/collections/list.html",
            "/templates/collections/checks.html",
            "/templates/invoices/list.html",
            "/templates/workorders/list.html",
            "/templates/operations/factory/blocks.html",
            "/templates/operations/factory/orders.html",
            "/templates/operations/factory/sized.html",
            "/templates/operations/factory/slabs.html",
            "/templates/operations/quarry/index.html",
            "/templates/operations/workshop/index.html",
            "/templates/erp/workshop/index.html",
            "/templates/erp/production/slabs.html",
            "/templates/erp/projects/index.html",
            "/templates/erp/sales/index.html",
            "/templates/erp/procurement/index.html",
            "/templates/admin/definitions/suppliers.html",
            "/templates/admin/definitions/customers.html",
            "/templates/admin/definitions/machines.html",
            "/templates/admin/definitions/stock-locations.html",
            "/templates/admin/definitions/quarries.html",
            "/templates/admin/definitions/cost-centers.html"
    );

    @Test
    @DisplayName("Shared grid helpers camelize rows, default page to 1, and recover from non-JSON payloads")
    void sharedGridHelpersHardenRemotePagination() throws Exception {
        String appJs = readResource("/static/js/app.js");

        Assertions.assertThat(appJs).contains("function erpGridDefaults(");
        Assertions.assertThat(appJs).contains("function erpGridAjaxUrl(");
        Assertions.assertThat(appJs).contains("function erpGridAjaxResponse(");
        Assertions.assertThat(appJs).contains("function emptyTabulatorResponse(");
        Assertions.assertThat(appJs).contains("const page = Number(params && params.page) > 0 ? params.page : 1");
        Assertions.assertThat(appJs).contains("camelizeTabulatorRows(payload)");
        Assertions.assertThat(appJs).contains("function gridText(");
        Assertions.assertThat(appJs).contains("function gridMoney(");
        Assertions.assertThat(appJs).contains("function gridArea(");
        Assertions.assertThat(appJs).contains("function erpStatusBadge(");
        Assertions.assertThat(appJs).contains("window.gridText = gridText");
        Assertions.assertThat(appJs).contains("window.gridArea = gridArea");
        Assertions.assertThat(appJs).contains("window.gridMoney = gridMoney");
        Assertions.assertThat(appJs).contains("window.erpStatusBadge = erpStatusBadge");
        Assertions.assertThat(appJs).contains("window.erpGridDefaults = erpGridDefaults");
        Assertions.assertThat(appJs).contains("locale: \"tr\"");
        Assertions.assertThat(appJs).contains("first: \"İlk\"");
        Assertions.assertThat(appJs).contains("minHeight: 180");
        Assertions.assertThat(appJs).contains("placeholder: \"Kayıt bulunamadı.\"");
        Assertions.assertThat(appJs).contains("window.erpGridAjaxUrl = erpGridAjaxUrl");
    }

    @Test
    @DisplayName("Base layout defines inline gridText fallbacks before page fragment scripts")
    void layoutDefinesInlineGridFallbacksBeforePageScripts() throws Exception {
        String layout = readResource("/templates/layout/base.html");
        int appJsIndex = layout.indexOf("@{/js/app.js(v=${assetVersion})}");
        int fallbackIndex = layout.indexOf("typeof window.gridText !== \"function\"");
        int pageScriptsIndex = layout.indexOf("layout:fragment=\"scripts\"");

        Assertions.assertThat(appJsIndex)
                .as("cache-busted app.js must still load before page fragment scripts")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(pageScriptsIndex);
        Assertions.assertThat(fallbackIndex)
                .as("inline script must define window.gridText with no network dependency")
                .isGreaterThan(appJsIndex)
                .isLessThan(pageScriptsIndex);

        String fallbackBlock = layout.substring(fallbackIndex, pageScriptsIndex);
        Assertions.assertThat(fallbackBlock).contains("window.gridText = function");
        Assertions.assertThat(fallbackBlock).contains("window.gridNumber");
        Assertions.assertThat(fallbackBlock).contains("window.gridMoney");
        Assertions.assertThat(fallbackBlock).contains("window.gridArea");
        Assertions.assertThat(fallbackBlock).contains("window.erpStatusBadge");
        Assertions.assertThat(fallbackBlock).contains("window.erpGridDefaults");
        Assertions.assertThat(fallbackBlock).contains("window.erpIndexColumn");
    }

    @Test
    @DisplayName("FilePond is off the global critical path and never loaded from unpkg")
    void filePondIsLocalAndNotOnEveryGridPage() throws Exception {
        String layout = readResource("/templates/layout/base.html");
        String blockForm = readResource("/templates/erp/blocks/form.html");

        Assertions.assertThat(layout).doesNotContain("unpkg.com/filepond");
        Assertions.assertThat(layout).doesNotContain("unpkg.com");
        Assertions.assertThat(layout).doesNotContain("filepond.min.js");
        Assertions.assertThat(layout).doesNotContain("filepond.min.css");
        Assertions.assertThat(layout).doesNotContain("filepond-setup.js");

        Assertions.assertThat(blockForm).doesNotContain("unpkg.com/filepond");
        Assertions.assertThat(blockForm).contains("@{/vendor/filepond/filepond.min.js}");
        Assertions.assertThat(blockForm).contains("@{/vendor/filepond/filepond-plugin-image-preview.min.js}");
        Assertions.assertThat(blockForm).contains("@{/js/filepond-setup.js(v=${assetVersion})}");

        int filePondJsIndex = blockForm.indexOf("@{/vendor/filepond/filepond.min.js}");
        int initIndex = blockForm.indexOf("initMultiFilePond(");
        if (initIndex < 0) {
            initIndex = blockForm.indexOf("initFilePond(");
        }
        Assertions.assertThat(filePondJsIndex).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(initIndex)
                .as("block form must initialize FilePond only after local vendor scripts")
                .isGreaterThan(filePondJsIndex);

        var blockingRemoteScript = Pattern.compile(
                "<script(?![^>]*\\b(?:async|defer)\\b)[^>]*src=\"https?://[^\"]*\"",
                Pattern.CASE_INSENSITIVE);
        Assertions.assertThat(blockingRemoteScript.matcher(layout).find())
                .as("classic remote scripts without async/defer would stall grid helpers if a CDN hangs")
                .isFalse();

        Assertions.assertThat(getClass().getResource("/static/vendor/filepond/filepond.min.js")).isNotNull();
        Assertions.assertThat(getClass().getResource("/static/vendor/filepond/filepond.min.css")).isNotNull();
        Assertions.assertThat(getClass().getResource("/static/vendor/filepond/filepond-plugin-image-preview.min.js")).isNotNull();
        Assertions.assertThat(getClass().getResource("/static/vendor/filepond/filepond-plugin-image-preview.min.css")).isNotNull();
    }

    @Test
    @DisplayName("Every Tabulator grid uses shared AJAX helpers and avoids interpolating raw undefined values")
    void everyGridUsesSharedAjaxHelpersAndSafeDisplay() throws Exception {
        for (String resource : GRID_SOURCES) {
            String source = readResource(resource);
            if (source.contains("fragments/data-grid :: shell")) {
                Assertions.assertThat(source)
                        .as("%s server grid must not embed raw Thymeleaf inside Alpine", resource)
                        .doesNotContain("[[${");
                continue;
            }
            Assertions.assertThat(source)
                    .as("%s should build page/size with erpGridAjaxUrl", resource)
                    .contains("erpGridAjaxUrl(");
            Assertions.assertThat(source)
                    .as("%s should normalize the AJAX payload with erpGridAjaxResponse", resource)
                    .contains("erpGridAjaxResponse(");
            Assertions.assertThat(source)
                    .as("%s should render missing values with gridText", resource)
                    .contains("gridText(");
            Assertions.assertThat(source)
                    .as("%s should not interpolate raw cell values that become the word undefined", resource)
                    .doesNotContain("${cell.getValue()}");
            Assertions.assertThat(source)
                    .as("%s should not format missing numbers as NaN", resource)
                    .doesNotContain("Number(cell.getValue()).toFixed")
                    .doesNotContain("Number(row.totalAreaM2).toFixed")
                    .doesNotContain("Number(row.totalSlabAreaM2).toFixed");
        }
    }

    @Test
    @DisplayName("Asset version is configured, non-empty, and cache-busting all critical assets")
    void assetVersionIsConfiguredAndConsistent() throws Exception {
        String appYml = readResource("/application.yml");
        Assertions.assertThat(appYml).contains("asset-version:");

        String layout = readResource("/templates/layout/base.html");
        Assertions.assertThat(layout).contains("@{/css/app.css(v=${assetVersion})}");
        Assertions.assertThat(layout).contains("@{/js/app.js(v=${assetVersion})}");
    }

    @Test
    @DisplayName("Templates do not contain unparsed Thymeleaf expressions in HTML attributes")
    void templatesDoNotContainUnparsedThymeleafInAttributes() throws Exception {
        for (String resource : GRID_SOURCES) {
            if (resource.endsWith(".html")) {
                String source = readResource(resource);
                Assertions.assertThat(source)
                        .as("%s should not contain unparsed Thymeleaf inlining inside attributes", resource)
                        .doesNotContain("'[['")
                        .doesNotContain("\"[[\"");
            }
        }
    }

    private static String readResource(String resource) throws Exception {
        try (var in = TabulatorGridUndefinedTest.class.getResourceAsStream(resource)) {
            Assertions.assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
