package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportDropdownPagesTest {

    private static final Map<String, String> LIST_PAGES = Map.of(
            "/templates/erp/blocks/index.html", "exportDropdown('blocks-table', 'bloklar')",
            "/templates/erp/production/index.html", "exportDropdown('production-table', 'uretim_emirleri')",
            "/templates/erp/production/slabs.html", "exportDropdown('slabs-table', 'plakalar')",
            "/templates/erp/workshop/index.html", "exportDropdown('workshop-table', 'kesim_emirleri')",
            "/templates/erp/projects/index.html", "exportDropdown('projects-table', 'projeler')",
            "/templates/erp/procurement/index.html", "exportDropdown('procurement-table', 'satinalma')",
            "/templates/erp/sales/index.html", "exportDropdown('sales-table', 'satislar')",
            "/templates/admin/users/index.html", "exportDropdown('users-table', 'kullanicilar')"
    );

    @Test
    @DisplayName("Every Dışa Aktar list page uses the shared dropdown with a Turkish entity stem")
    void everyListPageUsesSharedExportDropdown() throws Exception {
        for (Map.Entry<String, String> page : LIST_PAGES.entrySet()) {
            String source = readResource(page.getKey());
            assertThat(source)
                    .as("%s should use the shared export dropdown", page.getKey())
                    .contains("fragments/list-chrome :: " + page.getValue());
            assertThat(source)
                    .as("%s should not hardcode a download filename", page.getKey())
                    .doesNotContain("downloadTable(");
        }
    }

    @Test
    @DisplayName("Shared dropdown generates timestamped filenames through downloadTable")
    void sharedDropdownDelegatesToDownloadTable() throws Exception {
        String fragment = readResource("/templates/fragments/list-chrome.html");
        String appJs = readResource("/static/js/app.js");

        assertThat(fragment).contains("Dışa Aktar");
        assertThat(fragment).contains("downloadTable($root.dataset.exportTable, $root.dataset.exportEntity, 'csv')");
        assertThat(fragment).contains("downloadTable($root.dataset.exportTable, $root.dataset.exportEntity, 'xlsx')");
        assertThat(appJs).contains("function buildExportFilename(");
        assertThat(appJs).contains("function downloadTable(");
        assertThat(appJs).contains("buildExportFilename(baseName, \"xlsx\")");
        assertThat(appJs).contains("buildExportFilename(baseName, \"csv\")");
    }

    private static String readResource(String resource) throws Exception {
        try (var in = ExportDropdownPagesTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
