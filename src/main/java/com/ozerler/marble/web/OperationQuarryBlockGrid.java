package com.ozerler.marble.web;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.model.enums.BlockStatus;

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
        String quarry;
        if (block.getQuarry() == null) {
            quarry = "";
        } else {
            quarry = OperationGridSupport.named(block.getQuarry().getName(), "");
        }
        String section = block.getQuarrySection();
        if (quarry.isBlank()) {
            if (section == null) {
                quarry = "";
            } else {
                quarry = section;
            }
            section = "";
        }
        String actual;
        if (block.getActualTonnage() == null) {
            actual = "Kantar Gir";
        } else {
            actual = OperationGridSupport.amount(block.getActualTonnage(), "ton");
        }
        String badge;
        if (block.getStatus() == null) {
            badge = "";
        } else {
            badge = block.getStatus().getDisplayName();
        }
        return GridRow.of(viewUrl, dispatchUrl, null, null, List.of(
                GridCell.badge(badge),
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
                GridColumn.filtered("status", "Durum", false, GridWidth.PX_160, "status",
                        OperationGridSupport.choices(BlockStatus.values(), BlockStatus::getDisplayName)),
                GridColumn.text("id", "Id", false, GridWidth.PX_110),
                GridColumn.text("blockCode", "Blok No", false, GridWidth.PX_160),
                GridColumn.text("quarryName", "Ocak & Lokasyon", false, GridWidth.PX_180),
                GridColumn.text("stoneType", "Taş Türü", false, GridWidth.PX_140),
                GridColumn.text("dimensions", "Ölçüler", false, GridWidth.PX_140),
                GridColumn.text("approximateTonnage", "Tahmini Tonaj", false, GridWidth.PX_140),
                GridColumn.text("actualTonnage", "Gerçek Tonaj", false, GridWidth.PX_140)
        );
    }
}
