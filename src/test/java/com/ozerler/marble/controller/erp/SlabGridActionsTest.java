package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlabGridActionsTest {

    @Test
    @DisplayName("Slabs grid operations menu includes an edit link for every slab")
    void operationsColumnIncludesEditLink() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/slabs.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("fragments/data-grid :: shell");
        com.ozerler.marble.dto.SlabDto slab = com.ozerler.marble.dto.SlabDto.builder()
                .id(9L)
                .slabCode("SL-9")
                .statusLabel("Onaylı")
                .surfaceFinishLabel("Cilalı")
                .qualityGradeLabel("A")
                .build();
        com.ozerler.marble.web.GridRow row = com.ozerler.marble.web.SlabGrid.row(slab);
        assertThat(row.getViewUrl()).isEqualTo("/production/slabs/9");
        assertThat(row.getEditUrl()).isEqualTo("/production/slabs/9/edit");
        assertThat(row.getCells()).anyMatch(cell -> "Cilalı".equals(cell.getText()));
        assertThat(row.getCells()).anyMatch(cell -> "A".equals(cell.getText()));
    }

    @Test
    @DisplayName("Slab label printout uses localized quality and surface labels")
    void labelPageUsesLocalizedEnumLabels() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/slab-label.html")) {
            assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        assertThat(html).contains("slab.qualityGrade.label");
        assertThat(html).contains("slab.surfaceFinish.label");
        assertThat(html).doesNotContain("th:text=\"${slab.qualityGrade}\"");
        assertThat(html).doesNotContain("th:text=\"${slab.surfaceFinish}\"");
    }
}
