package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.TabulatorResponse;

class BlockGridActionTest {

    @Test
    @DisplayName("A quarry stock block keeps the historical İŞLEMLER menu entries")
    void quarryBlockExposesTransferSellAndDeleteActions() {
        BlockDto block = BlockDto.builder()
                .id(18L)
                .blockCode("BLK-18")
                .canonicalStatus("PRODUCED")
                .status("PRODUCED")
                .locationType("DISPATCH_YARD")
                .approximateTonnage(new BigDecimal("12.5"))
                .canDelete(true)
                .build();

        GridRow row = BlockGrid.row(block);
        List<String> labels = row.getActions().stream().map(GridAction::getLabel).toList();

        Assertions.assertThat(labels).containsExactly(
                "Detay ve işlemler",
                "Düzenle",
                "Kantar Tartımı Gir",
                "Üretim Sahasına taşı",
                "Markala (Rezerve Et)",
                "Fabrikaya sevk",
                "Sat (Fatura Kes)",
                "Bloğu Sil");
        Assertions.assertThat(row.getActions().get(0).getHref()).isEqualTo("/blocks/18");
        Assertions.assertThat(row.getActions().get(5).getHref()).isEqualTo("/blocks/18/transfer-to-factory");
        Assertions.assertThat(row.getActions().get(7).isDeletable()).isTrue();
    }

    @Test
    @DisplayName("A sold block only offers detail, edit and optional delete")
    void soldBlockOmitsQuarryOnlyActions() {
        BlockDto block = BlockDto.builder()
                .id(19L)
                .blockCode("BLK-19")
                .canonicalStatus("SOLD")
                .status("SOLD")
                .canDelete(false)
                .build();

        Assertions.assertThat(BlockGrid.row(block).getActions())
                .extracting(GridAction::getLabel)
                .containsExactly("Detay ve işlemler", "Düzenle");
    }

    @Test
    @DisplayName("The blocks grid is assembled with no frozen columns")
    void blocksGridIsNotFrozen() {
        BlockDto block = BlockDto.builder().id(1L).blockCode("BLK-1").status("SOLD").canonicalStatus("SOLD").build();
        DataGridView grid = BlockGrid.view(TabulatorResponse.of(List.of(block), 1, 1), 1, 20, "blockCode", "asc");

        Assertions.assertThat(grid.getFrozenCount()).isEqualTo(0);
        Assertions.assertThat(grid.getRows().get(0).isActionable()).isTrue();
    }
}
