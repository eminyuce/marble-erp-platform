package com.ozerler.marble.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataGridViewTest {

    @Test
    @DisplayName("Page window keeps the first page, four nearby pages, and the last page")
    void markersKeepFirstWindowAndLast() {
        List<PageMarker> markers = DataGridView.markers(12, 1677);

        assertThat(labels(markers)).containsExactly("1", "…", "11", "12", "13", "14", "…", "1677");
        assertThat(markers.get(3).isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Short lists show every page and the default size is 20")
    void shortListAndDefaultSize() {
        DataGridView grid = new DataGridView(1, 10, 1, 3, "", "", "sample", 4, List.of(), List.of());

        assertThat(labels(grid.getMarkers())).containsExactly("1");
        assertThat(grid.getSize()).isEqualTo(20);
        assertThat(grid.getFrozenCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Id chip is a bordered monospace token and blank text is empty")
    void chipAndEmptyCell() {
        assertThat(GridCell.chip(18067).getText()).isEqualTo("#18067");
        assertThat(GridCell.chip(18067).isChip()).isTrue();
        assertThat(GridCell.text(" ").isEmpty()).isTrue();
    }

    private static List<String> labels(List<PageMarker> markers) {
        return markers.stream()
                .map(marker -> marker.isEllipsis() ? "…" : Integer.toString(marker.getNumber()))
                .toList();
    }
}
