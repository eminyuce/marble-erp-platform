package com.ozerler.marble.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GridColumnFilterPanelPositionTest {

    @Test
    @DisplayName("A mid-table column icon opens the panel directly below, left-aligned")
    void midTableColumnStaysLeftAlignedToTheIcon() {
        int left = GridActionsMenuPosition.left(420, 260, 1280);
        int top = GridActionsMenuPosition.top(160, 22, 280, 800);

        Assertions.assertThat(left).isEqualTo(420);
        Assertions.assertThat(top).isEqualTo(160 + 22 + GridActionsMenuPosition.GAP);
    }

    @Test
    @DisplayName("The panel is clamped to an 8px viewport margin and never goes negative")
    void clampedToViewportAndNeverNegative() {
        int shifted = GridActionsMenuPosition.left(1100, 260, 1280);
        int narrow = GridActionsMenuPosition.left(16, 260, 240);

        Assertions.assertThat(shifted).isEqualTo(1280 - 260 - GridActionsMenuPosition.MARGIN);
        Assertions.assertThat(shifted).isGreaterThanOrEqualTo(GridActionsMenuPosition.MARGIN);
        Assertions.assertThat(narrow).isEqualTo(GridActionsMenuPosition.MARGIN);
        Assertions.assertThat(narrow).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("data-grid.js uses the shared İşlemler placer, fixes the panel, and follows scroll")
    void scriptUsesSharedPlacerAndTracksScroll() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/data-grid.js"));

        Assertions.assertThat(script).contains("positionGridActionsMenu");
        Assertions.assertThat(script).contains("function placeOpenGridColumnFilter(");
        Assertions.assertThat(script).contains("position:fixed");
        Assertions.assertThat(script).contains("right:auto");
        Assertions.assertThat(script).contains("button.getBoundingClientRect()");
        Assertions.assertThat(script)
                .contains("document.addEventListener(\"scroll\", onGridColumnFilterReposition, true)");
        Assertions.assertThat(script)
                .contains("window.addEventListener(\"resize\", onGridColumnFilterReposition)");
    }
}
