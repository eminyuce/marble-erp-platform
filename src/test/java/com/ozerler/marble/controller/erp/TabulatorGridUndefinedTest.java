package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    @DisplayName("Shared grid response handler camelizes snake_case API rows")
    void applyTabulatorTotalCamelizesRows() throws Exception {
        String appJs = readResource("/static/js/app.js");
        int applyStart = appJs.indexOf("function applyTabulatorTotal");
        int nextFunction = appJs.indexOf("\nfunction ", applyStart + 1);

        assertThat(applyStart).isGreaterThanOrEqualTo(0);
        assertThat(nextFunction).isGreaterThan(applyStart);

        String applyBody = appJs.substring(applyStart, nextFunction);
        assertThat(applyBody).contains("camelizeTabulatorRows(response)");
        assertThat(appJs).contains("function gridText(");
        assertThat(appJs).contains("function gridMoney(");
        assertThat(appJs).contains("function gridArea(");
    }

    @Test
    @DisplayName("Every Tabulator grid camelizes API rows and avoids interpolating raw undefined values")
    void everyGridCamelizesAndUsesSafeDisplayHelpers() throws Exception {
        for (String resource : GRID_SOURCES) {
            String source = readResource(resource);
            assertThat(source)
                    .as("%s should camelize Tabulator rows", resource)
                    .contains("camelizeTabulatorRows(response)");
            assertThat(source)
                    .as("%s should apply the shared grid total/normalize helper", resource)
                    .contains("applyTabulatorTotal(");
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
