package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.dto.SlabDto;
import com.ozerler.marble.web.GridRow;
import com.ozerler.marble.web.SlabGrid;

class SlabGridActionsTest {

    @Test
    @DisplayName("Slabs grid operations menu includes an edit link for every slab")
    void operationsColumnIncludesEditLink() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/slabs.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("fragments/data-grid :: shell");
        SlabDto slab = SlabDto.builder()
                .id(9L)
                .slabCode("SL-9")
                .statusLabel("Onaylı")
                .surfaceFinishLabel("Cilalı")
                .qualityGradeLabel("A")
                .build();
        GridRow row = SlabGrid.row(slab);
        Assertions.assertThat(row.getViewUrl()).isEqualTo("/production/slabs/9");
        Assertions.assertThat(row.getEditUrl()).isEqualTo("/production/slabs/9/edit");
        Assertions.assertThat(row.getCells()).anyMatch(cell -> "Cilalı".equals(cell.getText()));
        Assertions.assertThat(row.getCells()).anyMatch(cell -> "A".equals(cell.getText()));
    }

    @Test
    @DisplayName("Slab label printout uses localized quality and surface labels")
    void labelPageUsesLocalizedEnumLabels() throws Exception {
        String html;
        try (var in = getClass().getResourceAsStream("/templates/erp/production/slab-label.html")) {
            Assertions.assertThat(in).isNotNull();
            html = new String(in.readAllBytes());
        }

        Assertions.assertThat(html).contains("slab.qualityGrade.label");
        Assertions.assertThat(html).contains("slab.surfaceFinish.label");
        Assertions.assertThat(html).doesNotContain("th:text=\"${slab.qualityGrade}\"");
        Assertions.assertThat(html).doesNotContain("th:text=\"${slab.surfaceFinish}\"");
    }
}
