package com.ozerler.marble.web;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataGridViewTest {

    @Test
    @DisplayName("Page window keeps the first page, four nearby pages, and the last page")
    void markersKeepFirstWindowAndLast() {
        List<PageMarker> markers = DataGridView.markers(12, 1677);

        Assertions.assertThat(labels(markers)).containsExactly("1", "…", "11", "12", "13", "14", "…", "1677");
        Assertions.assertThat(markers.get(3).isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Short lists show every page and the default size is 20")
    void shortListAndDefaultSize() {
        DataGridView grid = new DataGridView(1, 10, 1, 3, "", "", "sample", 4, List.of(), List.of());

        Assertions.assertThat(labels(grid.getMarkers())).containsExactly("1");
        Assertions.assertThat(grid.getSize()).isEqualTo(20);
        Assertions.assertThat(grid.getFrozenCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Id chip is a bordered monospace token and blank text is empty")
    void chipAndEmptyCell() {
        Assertions.assertThat(GridCell.chip(18067).getText()).isEqualTo("#18067");
        Assertions.assertThat(GridCell.chip(18067).isChip()).isTrue();
        Assertions.assertThat(GridCell.text(" ").isEmpty()).isTrue();
    }

    private static List<String> labels(List<PageMarker> markers) {
        return markers.stream()
                .map(marker -> {
                    if (marker.isEllipsis()) {
                        return "…";
                    }
                    return Integer.toString(marker.getNumber());
                })
                .toList();
    }
}
