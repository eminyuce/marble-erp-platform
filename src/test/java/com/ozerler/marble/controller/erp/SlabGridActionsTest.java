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

        assertThat(html).contains("{icon: 'edit-3', label: 'Düzenle', href: '/production/slabs/' + row.id + '/edit'}");
        assertThat(html).contains("{icon: 'eye', label: 'Detay', href: '/production/slabs/' + row.id}");
        assertThat(html).contains("erpStatusBadge(row.status, row.statusLabel)");
        assertThat(html).contains("row.surfaceFinishLabel");
        assertThat(html).contains("row.qualityGradeLabel");
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
