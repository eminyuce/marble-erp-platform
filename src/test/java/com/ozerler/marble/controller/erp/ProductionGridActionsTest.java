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
