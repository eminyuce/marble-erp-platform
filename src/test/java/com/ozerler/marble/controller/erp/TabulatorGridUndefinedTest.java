package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TabulatorGridUndefinedTest {

    private static final List<String> GRID_SOURCES = List.of(
            "/static/js/blocks-grid.js",
            "/static/js/production-grid.js",
            "/static/js/users-grid.js",
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

        assertThat(appJs).contains("function erpGridDefaults(");
        assertThat(appJs).contains("function erpGridAjaxUrl(");
        assertThat(appJs).contains("function erpGridAjaxResponse(");
        assertThat(appJs).contains("function emptyTabulatorResponse(");
        assertThat(appJs).contains("const page = Number(params && params.page) > 0 ? params.page : 1");
        assertThat(appJs).contains("camelizeTabulatorRows(payload)");
        assertThat(appJs).contains("function gridText(");
        assertThat(appJs).contains("function gridMoney(");
        assertThat(appJs).contains("function gridArea(");
        assertThat(appJs).contains("function erpStatusBadge(");
        assertThat(appJs).contains("window.gridText = gridText");
        assertThat(appJs).contains("window.gridArea = gridArea");
        assertThat(appJs).contains("window.gridMoney = gridMoney");
        assertThat(appJs).contains("window.erpStatusBadge = erpStatusBadge");
        assertThat(appJs).contains("window.erpGridDefaults = erpGridDefaults");
        assertThat(appJs).contains("locale: \"tr\"");
        assertThat(appJs).contains("first: \"İlk\"");
        assertThat(appJs).contains("minHeight: 180");
        assertThat(appJs).contains("placeholder: \"Kayıt bulunamadı.\"");
        assertThat(appJs).contains("window.erpGridAjaxUrl = erpGridAjaxUrl");
    }

    @Test
    @DisplayName("Base layout defines inline gridText fallbacks before page fragment scripts")
    void layoutDefinesInlineGridFallbacksBeforePageScripts() throws Exception {
        String layout = readResource("/templates/layout/base.html");
        int appJsIndex = layout.indexOf("@{/js/app.js(v=${assetVersion})}");
        int fallbackIndex = layout.indexOf("typeof window.gridText !== \"function\"");
        int pageScriptsIndex = layout.indexOf("layout:fragment=\"scripts\"");

        assertThat(appJsIndex)
                .as("cache-busted app.js must still load before page fragment scripts")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(pageScriptsIndex);
        assertThat(fallbackIndex)
                .as("inline script must define window.gridText with no network dependency")
                .isGreaterThan(appJsIndex)
                .isLessThan(pageScriptsIndex);

        String fallbackBlock = layout.substring(fallbackIndex, pageScriptsIndex);
        assertThat(fallbackBlock).contains("window.gridText = function");
        assertThat(fallbackBlock).contains("window.gridNumber");
        assertThat(fallbackBlock).contains("window.gridMoney");
        assertThat(fallbackBlock).contains("window.gridArea");
        assertThat(fallbackBlock).contains("window.erpStatusBadge");
        assertThat(fallbackBlock).contains("window.erpGridDefaults");
        assertThat(fallbackBlock).contains("window.erpIndexColumn");
    }

    @Test
    @DisplayName("FilePond is off the global critical path and never loaded from unpkg")
    void filePondIsLocalAndNotOnEveryGridPage() throws Exception {
        String layout = readResource("/templates/layout/base.html");
        String blockForm = readResource("/templates/erp/blocks/form.html");

        assertThat(layout).doesNotContain("unpkg.com/filepond");
        assertThat(layout).doesNotContain("unpkg.com");
        assertThat(layout).doesNotContain("filepond.min.js");
        assertThat(layout).doesNotContain("filepond.min.css");
        assertThat(layout).doesNotContain("filepond-setup.js");

        assertThat(blockForm).doesNotContain("unpkg.com/filepond");
        assertThat(blockForm).contains("@{/vendor/filepond/filepond.min.js}");
        assertThat(blockForm).contains("@{/vendor/filepond/filepond-plugin-image-preview.min.js}");
        assertThat(blockForm).contains("@{/js/filepond-setup.js(v=${assetVersion})}");

        int filePondJsIndex = blockForm.indexOf("@{/vendor/filepond/filepond.min.js}");
        int initIndex = blockForm.indexOf("initMultiFilePond(");
        if (initIndex < 0) {
            initIndex = blockForm.indexOf("initFilePond(");
        }
        assertThat(filePondJsIndex).isGreaterThanOrEqualTo(0);
        assertThat(initIndex)
                .as("block form must initialize FilePond only after local vendor scripts")
                .isGreaterThan(filePondJsIndex);

        var blockingRemoteScript = Pattern.compile(
                "<script(?![^>]*\\b(?:async|defer)\\b)[^>]*src=\"https?://[^\"]*\"",
                Pattern.CASE_INSENSITIVE);
        assertThat(blockingRemoteScript.matcher(layout).find())
                .as("classic remote scripts without async/defer would stall grid helpers if a CDN hangs")
                .isFalse();

        assertThat(getClass().getResource("/static/vendor/filepond/filepond.min.js")).isNotNull();
        assertThat(getClass().getResource("/static/vendor/filepond/filepond.min.css")).isNotNull();
        assertThat(getClass().getResource("/static/vendor/filepond/filepond-plugin-image-preview.min.js")).isNotNull();
        assertThat(getClass().getResource("/static/vendor/filepond/filepond-plugin-image-preview.min.css")).isNotNull();
    }

    @Test
    @DisplayName("Every Tabulator grid uses shared AJAX helpers and avoids interpolating raw undefined values")
    void everyGridUsesSharedAjaxHelpersAndSafeDisplay() throws Exception {
        for (String resource : GRID_SOURCES) {
            String source = readResource(resource);
            assertThat(source)
                    .as("%s should build page/size with erpGridAjaxUrl", resource)
                    .contains("erpGridAjaxUrl(");
            assertThat(source)
                    .as("%s should normalize the AJAX payload with erpGridAjaxResponse", resource)
                    .contains("erpGridAjaxResponse(");
            assertThat(source)
                    .as("%s should render missing values with gridText", resource)
                    .contains("gridText(");
            assertThat(source)
                    .as("%s should not interpolate raw cell values that become the word undefined", resource)
                    .doesNotContain("${cell.getValue()}");
            assertThat(source)
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
        assertThat(appYml).contains("asset-version:");

        String layout = readResource("/templates/layout/base.html");
        assertThat(layout).contains("@{/css/app.css(v=${assetVersion})}");
        assertThat(layout).contains("@{/js/app.js(v=${assetVersion})}");
    }

    private static String readResource(String resource) throws Exception {
        try (var in = TabulatorGridUndefinedTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
