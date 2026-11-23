package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkshopGridActionsTest {

    @Test
    @DisplayName("Workshop grid keeps the cut-order detail and edit links")
    void gridCamelizesApiRows() throws Exception {
        String html = readWorkshopIndex();
        com.ozerler.marble.dto.CutOrderDto order = com.ozerler.marble.dto.CutOrderDto.builder()
                .id(5L)
                .cutOrderNo("IE-5")
                .projectName("Ada")
                .statusLabel("Onaylı")
                .itemCount(2)
                .build();

        Assertions.assertThat(html).contains("fragments/data-grid :: shell");
        com.ozerler.marble.web.GridRow row = com.ozerler.marble.web.WorkshopGrid.row(order);
        Assertions.assertThat(row.getViewUrl()).isEqualTo("/workshop/5");
        Assertions.assertThat(row.getEditUrl()).isEqualTo("/workshop/5/edit");
        Assertions.assertThat(row.getCells()).anyMatch(cell -> "IE-5".equals(cell.getText()));
        Assertions.assertThat(row.getCells()).anyMatch(cell -> "Ada".equals(cell.getText()));
    }

    @Test
    @DisplayName("Workshop grid operations menu includes an edit link for every cut order")
    void operationsColumnIncludesEditLink() throws Exception {
        String html = readWorkshopIndex();

        Assertions.assertThat(html).contains("fragments/data-grid :: shell");
        Assertions.assertThat(html).contains("th:href=\"@{/workshop/receipts}\"");
        Assertions.assertThat(html).contains("Malzeme kabulü");
        Assertions.assertThat(html).doesNotContain("th:action=\"@{/workshop/receipts}\"");
    }

    @Test
    @DisplayName("Workshop edit form posts updates to the existing cut order")
    void editFormPostsToCutOrderEdit() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/workshop/cut-order-form.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("@{/workshop/{id}/edit(id=${record.id})}");
        Assertions.assertThat(html).contains("th:value=\"${isEdit and record != null ? record.machineName : ''}\"");
        Assertions.assertThat(html).contains("s.surfaceFinish.label");
        Assertions.assertThat(html).contains("s.qualityGrade.label");
    }

    @Test
    @DisplayName("Workshop material receipt page posts to /workshop/receipts and returns to work orders")
    void receiptsPageLinksBackToWorkshopList() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/workshop/receipts.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("th:action=\"@{/workshop/receipts}\"");
        Assertions.assertThat(html).contains("th:href=\"@{/workshop}\"");
        Assertions.assertThat(html).contains("Atölye iş emirleri");
        Assertions.assertThat(html).contains("exportDropdown('workshop-receipts-table', 'atelye_malzeme_kabulleri')");
        Assertions.assertThat(html).contains("name=\"stoneType\"");
    }

    @Test
    @DisplayName("Workshop order detail shows Turkish status labels")
    void detailPageUsesStatusLabels() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/workshop/detail.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("th:text=\"${order.statusLabel}\"");
        Assertions.assertThat(html).contains("th:text=\"${item.statusLabel}\"");
        Assertions.assertThat(html).doesNotContain("th:text=\"${order.status}\"");
        Assertions.assertThat(html).doesNotContain("th:text=\"${item.status}\"");
    }

    private static String readWorkshopIndex() throws Exception {
        try (var in = WorkshopGridActionsTest.class.getResourceAsStream("/templates/erp/workshop/index.html")) {
            Assertions.assertThat(in).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
