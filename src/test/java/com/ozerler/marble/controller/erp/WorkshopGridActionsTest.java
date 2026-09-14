package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkshopGridActionsTest {

    @Test
    @DisplayName("Workshop grid camelizes API rows so Tabulator can read cutOrderNo")
    void gridCamelizesApiRows() throws Exception {
        String html = readWorkshopIndex();

        assertThat(html).contains("camelizeTabulatorRows(response)");
        assertThat(html).contains("field: \"cutOrderNo\"");
        assertThat(html).contains("field: \"projectName\"");
        assertThat(html).contains("row.itemCount");
        assertThat(html).contains("row.totalAreaM2");
    }

    @Test
    @DisplayName("Workshop grid operations menu includes an edit link for every cut order")
    void operationsColumnIncludesEditLink() throws Exception {
        String html = readWorkshopIndex();

        assertThat(html).contains("{icon: 'edit-3', label: 'Düzenle', href: '/workshop/' + row.id + '/edit'}");
        assertThat(html).contains("erpStatusBadge(row.status, row.statusLabel)");
    }

    @Test
    @DisplayName("Workshop edit form posts updates to the existing cut order")
    void editFormPostsToCutOrderEdit() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/workshop/cut-order-form.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("@{/workshop/{id}/edit(id=${record.id})}");
        assertThat(html).contains("th:value=\"${isEdit and record != null ? record.machineName : ''}\"");
        assertThat(html).contains("s.surfaceFinish.label");
        assertThat(html).contains("s.qualityGrade.label");
    }

    @Test
    @DisplayName("Workshop order detail shows Turkish status labels")
    void detailPageUsesStatusLabels() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/workshop/detail.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("th:text=\"${order.statusLabel}\"");
        assertThat(html).contains("th:text=\"${item.statusLabel}\"");
        assertThat(html).doesNotContain("th:text=\"${order.status}\"");
        assertThat(html).doesNotContain("th:text=\"${item.status}\"");
    }

    private static String readWorkshopIndex() throws Exception {
        try (var in = WorkshopGridActionsTest.class.getResourceAsStream("/templates/erp/workshop/index.html")) {
            assertThat(in).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
