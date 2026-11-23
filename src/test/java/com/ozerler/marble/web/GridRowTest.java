package com.ozerler.marble.web;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GridRowTest {

    @Test
    @DisplayName("Standard row actions are Detay, Düzenle and a confirmed Sil")
    void standardActionsIncludeViewEditAndDelete() {
        GridRow row = GridRow.of("/projects/1", "/projects/1/edit", 1L, "Ada", List.of());

        Assertions.assertThat(row.getActions())
                .extracting(GridAction::getLabel)
                .containsExactly("Detay", "Düzenle", "Sil");
        Assertions.assertThat(row.getActions().get(2).isDeletable()).isTrue();
    }

    @Test
    @DisplayName("When view and edit share a URL only Düzenle is shown")
    void sameViewAndEditUrlShowsOnlyEdit() {
        GridRow row = GridRow.of("/admin/definitions/machines/3/edit",
                "/admin/definitions/machines/3/edit", 3L, "Makine", List.of());

        Assertions.assertThat(row.getActions())
                .extracting(GridAction::getLabel)
                .containsExactly("Düzenle", "Sil");
    }

    @Test
    @DisplayName("A row without actions is not actionable")
    void emptyActionsAreNotActionable() {
        GridRow row = GridRow.of(List.of(), List.of());

        Assertions.assertThat(row.isActionable()).isFalse();
        Assertions.assertThat(row.getActions()).isEmpty();
    }
}
