package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportDropdownPagesTest {

    private static final Map<String, String> LIST_PAGES = Map.ofEntries(
            Map.entry("/templates/erp/blocks/index.html", "exportDropdown('blocks-table', 'bloklar')"),
            Map.entry("/templates/erp/production/index.html", "exportDropdown('production-table', 'uretim_emirleri')"),
            Map.entry("/templates/erp/production/slabs.html", "exportDropdown('slabs-table', 'plakalar')"),
            Map.entry("/templates/erp/workshop/index.html", "exportDropdown('workshop-table', 'atelye_is_emirleri')"),
            Map.entry("/templates/erp/projects/index.html", "exportDropdown('projects-table', 'projeler')"),
            Map.entry("/templates/erp/procurement/index.html", "exportDropdown('procurement-table', 'satinalma')"),
            Map.entry("/templates/erp/sales/index.html", "exportDropdown('sales-table', 'satislar')"),
            Map.entry("/templates/admin/users/index.html", "exportDropdown('users-table', 'kullanicilar')"),
            Map.entry("/templates/admin/definitions/suppliers.html", "exportDropdown('suppliers-table', 'tedarikciler')"),
            Map.entry("/templates/admin/definitions/customers.html", "exportDropdown('customers-table', 'musteriler')"),
            Map.entry("/templates/admin/definitions/machines.html", "exportDropdown('machines-table', 'makineler')"),
            Map.entry("/templates/admin/definitions/stock-locations.html", "exportDropdown('stock-locations-table', 'stok_sahalari')"),
            Map.entry("/templates/admin/definitions/quarries.html", "exportDropdown('quarries-table', 'ocaklar')"),
            Map.entry("/templates/admin/definitions/cost-centers.html", "exportDropdown('cost-centers-table', 'masraf_merkezleri')")
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
