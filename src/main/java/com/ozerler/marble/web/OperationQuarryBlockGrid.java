package com.ozerler.marble.web;

import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.model.enums.BlockStatus;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Ocak blok listesi. Status filter already exists. Detail and dispatch stay as the two row links.
 */
public final class OperationQuarryBlockGrid {

    private OperationQuarryBlockGrid() {
    }

    public static DataGridView view(Page<BlockView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "quarry-blocks-table", columns(), OperationQuarryBlockGrid::row);
    }

    static GridRow row(BlockView block) {
        String viewUrl = "/blocks/" + block.getId();
        String dispatchUrl = "/operations/quarry/" + block.getId() + "/dispatch";
        String quarry = block.getQuarry() == null ? "" : OperationGridSupport.named(block.getQuarry().getName(), "");
        String section = block.getQuarrySection();
        if (quarry.isBlank()) {
            quarry = section == null ? "" : section;
            section = "";
        }
        String actual = block.getActualTonnage() == null
                ? "Kantar Gir"
                : OperationGridSupport.amount(block.getActualTonnage(), "ton");
        return GridRow.of(viewUrl, dispatchUrl, null, null, List.of(
                GridCell.badge(block.getStatus() == null ? "" : block.getStatus().getDisplayName()),
                GridCell.chip(block.getId()),
                GridCell.link(block.getBlockCode(), viewUrl),
                OperationGridSupport.lines(quarry, section),
                GridCell.text(block.getStoneType()),
                GridCell.text(block.getDimensions()),
                GridCell.text(OperationGridSupport.amount(block.getApproximateTonnage(), "ton")),
                GridCell.text(actual)
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, 160, "status",
                        OperationGridSupport.choices(BlockStatus.values(), BlockStatus::getDisplayName)),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("blockCode", "Blok No", false, 160),
                GridColumn.text("quarryName", "Ocak & Lokasyon", false, 180),
                GridColumn.text("stoneType", "Taş Türü", false, 140),
                GridColumn.text("dimensions", "Ölçüler", false, 140),
                GridColumn.text("approximateTonnage", "Tahmini Tonaj", false, 140),
                GridColumn.text("actualTonnage", "Gerçek Tonaj", false, 140)
        );
    }
}
