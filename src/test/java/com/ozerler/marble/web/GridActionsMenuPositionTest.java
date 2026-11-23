package com.ozerler.marble.web;

import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GridActionsMenuPositionTest {

    @Test
    @DisplayName("A left-column trigger opens the menu to the right, not past the left edge")
    void leftColumnOpensDownAndRight() {
        int left = GridActionsMenuPosition.left(16, 240, 1280);
        int top = GridActionsMenuPosition.top(80, 32, 260, 800);

        Assertions.assertThat(left).isEqualTo(16);
        Assertions.assertThat(top).isEqualTo(80 + 32 + GridActionsMenuPosition.GAP);
    }

    @Test
    @DisplayName("The menu flips up when there is not enough space below the trigger")
    void flipsUpWhenShortOfSpaceBelow() {
        int top = GridActionsMenuPosition.top(700, 32, 260, 800);

        Assertions.assertThat(top).isEqualTo(700 - 260 - GridActionsMenuPosition.GAP);
        Assertions.assertThat(top + 260).isLessThanOrEqualTo(800 - GridActionsMenuPosition.MARGIN);
    }

    @Test
    @DisplayName("The menu shifts left only when it would overflow the right edge")
    void shiftsLeftOnlyWhenOverflowingRight() {
        int aligned = GridActionsMenuPosition.left(100, 180, 1280);
        int shifted = GridActionsMenuPosition.left(1100, 240, 1280);

        Assertions.assertThat(aligned).isEqualTo(100);
        Assertions.assertThat(shifted).isEqualTo(1280 - 240 - GridActionsMenuPosition.MARGIN);
        Assertions.assertThat(shifted + 240).isLessThanOrEqualTo(1280 - GridActionsMenuPosition.MARGIN);
    }

    @Test
    @DisplayName("app.js uses the shared placer and does not right-align the desktop portal")
    void appScriptOpensToTheRight() throws Exception {
        String script = Files.readString(Path.of("src/main/resources/static/js/app.js"));

        Assertions.assertThat(script).contains("function positionGridActionsMenu(");
        Assertions.assertThat(script).contains("positionGridActionsMenu(");
        Assertions.assertThat(script).contains("menu.style.right = 'auto'");
        Assertions.assertThat(script).doesNotContain("window.innerWidth - rect.right");
        Assertions.assertThat(script).doesNotContain("right:' + right + 'px");
    }
}
