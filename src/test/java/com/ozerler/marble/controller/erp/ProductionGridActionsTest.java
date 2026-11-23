package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductionGridActionsTest {

    @Test
    @DisplayName("Production grid operations menu includes an edit link for every order")
    void operationsColumnIncludesEditLink() throws Exception {
        String script;
        try (var in = getClass().getResourceAsStream("/static/js/production-grid.js")) {
            Assertions.assertThat(in).isNotNull();
            script = new String(in.readAllBytes());
        }

        Assertions.assertThat(script).contains("{icon: 'edit-3', label: 'Düzenle', href: '/production/orders/' + row.id + '/edit'}");
        Assertions.assertThat(script).contains("erpStatusBadge(row.status, row.statusLabel)");
    }

    @Test
    @DisplayName("Production list toolbar links to the block-accept page instead of posting there")
    void indexLinksToAcceptPage() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/index.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("th:href=\"@{/production/accept}\"");
        Assertions.assertThat(html).contains("Blok kabul");
        Assertions.assertThat(html).doesNotContain("th:action=\"@{/production/accept}\"");
    }

    @Test
    @DisplayName("Block-accept page posts to /production/accept and returns to factory work orders")
    void acceptPageLinksBackToProductionList() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/accept.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("th:action=\"@{/production/accept}\"");
        Assertions.assertThat(html).contains("th:href=\"@{/production}\"");
        Assertions.assertThat(html).contains("Fabrika iş emirleri");
        Assertions.assertThat(html).contains("exportDropdown('factory-accept-table', 'fabrika_blok_kabulleri')");
        Assertions.assertThat(html).contains("name=\"blockId\"");
        Assertions.assertThat(html).contains("name=\"responsibleName\"");
        Assertions.assertThat(html).contains("class=\"erp-form-actions\"");
        Assertions.assertThat(html).contains("type=\"submit\"");
        Assertions.assertThat(html).doesNotContain("form=\"factory-accept-form\"");
        Assertions.assertThat(html).contains("b.stoneType != null");
    }

    @Test
    @DisplayName("Production order detail shows the Turkish status label")
    void detailPageUsesStatusLabel() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/detail.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("th:text=\"${order.statusLabel}\"");
        Assertions.assertThat(html).doesNotContain("th:text=\"${order.status}\"");
    }
}
