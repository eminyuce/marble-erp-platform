package com.ozerler.marble.web;

import com.ozerler.marble.dto.view.SlabView;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Atölye plaka sahası. The row action opens ebatlama with the selected slab.
 */
public final class WorkshopSlabGrid {

    private WorkshopSlabGrid() {
    }

    public static DataGridView view(Page<SlabView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "workshop-slabs-table", columns(), WorkshopSlabGrid::row);
    }

    static GridRow row(SlabView slab) {
        String cutUrl = "/operations/workshop/sized/new?slabId=" + slab.getId();
        return GridRow.of(cutUrl, null, null, null, List.of(
                SlabGridCells.status(slab),
                GridCell.chip(slab.getId()),
                SlabGridCells.code(slab),
                SlabGridCells.block(slab),
                SlabGridCells.stone(slab),
                SlabGridCells.finish(slab),
                SlabGridCells.thickness(slab),
                SlabGridCells.dimensions(slab),
                SlabGridCells.area(slab),
                SlabGridCells.customer(slab)
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                SlabGridCells.statusColumn(),
                SlabGridCells.idColumn(),
                SlabGridCells.codeColumn(),
                SlabGridCells.blockColumn(),
                SlabGridCells.stoneColumn(),
                SlabGridCells.finishColumn(),
                SlabGridCells.thicknessColumn(),
                SlabGridCells.dimensionsColumn(),
                SlabGridCells.areaColumn(),
                GridColumn.text("customerName", "Müşteri Durumu", false, 180)
        );
    }
}
