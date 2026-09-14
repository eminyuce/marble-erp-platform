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
            "/templates/erp/procurement/index.html"
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
    @DisplayName("Layout serves FilePond locally so a CDN hang cannot block app.js globals")
    void layoutDoesNotLoadBlockingFilePondCdnBeforeAppJs() throws Exception {
        String layout = readResource("/templates/layout/base.html");

        assertThat(layout).doesNotContain("unpkg.com/filepond");
        assertThat(layout).contains("@{/vendor/filepond/filepond.min.js}");
        assertThat(layout).contains("@{/vendor/filepond/filepond-plugin-image-preview.min.js}");
        assertThat(layout).contains("@{/js/app.js(v=${assetVersion})}");

        int appJsIndex = layout.indexOf("@{/js/app.js");
        int filePondJsIndex = layout.indexOf("@{/vendor/filepond/filepond.min.js}");
        int fallbackIndex = layout.indexOf("typeof window.gridText !== \"function\"");
        int pageScriptsIndex = layout.indexOf("layout:fragment=\"scripts\"");
        assertThat(appJsIndex).isGreaterThanOrEqualTo(0);
        assertThat(filePondJsIndex).isGreaterThan(appJsIndex);
        assertThat(fallbackIndex)
                .as("inline script must define window.gridText with no network dependency")
                .isGreaterThan(appJsIndex);
        assertThat(pageScriptsIndex)
                .as("page grid scripts must load after cache-busted app.js and the gridText fallback")
                .isGreaterThan(appJsIndex)
                .isGreaterThan(fallbackIndex);

        var blockingRemoteScript = Pattern.compile(
                "<script(?![^>]*\\b(?:async|defer)\\b)[^>]*src=\"https?://[^\"]*\"",
                Pattern.CASE_INSENSITIVE);
        assertThat(blockingRemoteScript.matcher(layout).find())
                .as("classic remote scripts without async/defer would stall app.js if the CDN hangs")
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

    private static String readResource(String resource) throws Exception {
        try (var in = TabulatorGridUndefinedTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
