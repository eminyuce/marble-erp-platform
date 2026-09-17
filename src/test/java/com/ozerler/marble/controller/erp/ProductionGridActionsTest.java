package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionGridActionsTest {

    @Test
    @DisplayName("Production grid operations menu includes an edit link for every order")
    void operationsColumnIncludesEditLink() throws Exception {
        String script;
        try (var in = getClass().getResourceAsStream("/static/js/production-grid.js")) {
            assertThat(in).isNotNull();
            script = new String(in.readAllBytes());
        }

        assertThat(script).contains("{icon: 'edit-3', label: 'Düzenle', href: '/production/orders/' + row.id + '/edit'}");
        assertThat(script).contains("erpStatusBadge(row.status, row.statusLabel)");
    }

    @Test
    @DisplayName("Production list toolbar links to the block-accept page instead of posting there")
    void indexLinksToAcceptPage() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/index.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("th:href=\"@{/production/accept}\"");
        assertThat(html).contains("Blok kabul");
        assertThat(html).doesNotContain("th:action=\"@{/production/accept}\"");
    }

    @Test
    @DisplayName("Block-accept page posts to /production/accept and returns to factory work orders")
    void acceptPageLinksBackToProductionList() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/accept.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("th:action=\"@{/production/accept}\"");
        assertThat(html).contains("th:href=\"@{/production}\"");
        assertThat(html).contains("Fabrika iş emirleri");
        assertThat(html).contains("exportDropdown('factory-accept-table', 'fabrika_blok_kabulleri')");
        assertThat(html).contains("name=\"blockId\"");
        assertThat(html).contains("name=\"responsibleName\"");
    }

    @Test
    @DisplayName("Production order detail shows the Turkish status label")
    void detailPageUsesStatusLabel() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/detail.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("th:text=\"${order.statusLabel}\"");
        assertThat(html).doesNotContain("th:text=\"${order.status}\"");
    }
}
