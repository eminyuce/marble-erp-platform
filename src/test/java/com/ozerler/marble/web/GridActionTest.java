package com.ozerler.marble.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GridActionTest {

    @Test
    @DisplayName("jsCall escapes quotes and angle brackets in string arguments")
    void jsCallEscapesStringArguments() {
        String call = GridAction.jsCall("openMarkBlockModal", 9L, "BLK-O'HARA<x>", 12.5, null);

        Assertions.assertThat(call)
                .isEqualTo("openMarkBlockModal(9, 'BLK-O\\'HARA\\u003cx\\u003e', 12.5, null)");
    }

    @Test
    @DisplayName("Link, command, delete and divider flags are mutually exclusive")
    void actionKindsAreDistinct() {
        GridAction link = GridAction.link("Detay", "eye", "/blocks/1");
        GridAction command = GridAction.command("Kantar", "scale", "openActualTonnageModal(1)");
        GridAction delete = GridAction.delete("Sil", 4L, "BLK-1");
        GridAction divider = GridAction.divider();

        Assertions.assertThat(link.isLink()).isTrue();
        Assertions.assertThat(link.isCommand()).isFalse();
        Assertions.assertThat(command.isCommand()).isTrue();
        Assertions.assertThat(command.isLink()).isFalse();
        Assertions.assertThat(delete.isDeletable()).isTrue();
        Assertions.assertThat(delete.isDanger()).isTrue();
        Assertions.assertThat(divider.isDivider()).isTrue();
        Assertions.assertThat(divider.isLink()).isFalse();
    }
}
