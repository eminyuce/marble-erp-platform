package com.ozerler.marble.web;

import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.SlabView;

/**
 * Fabrika plaka sahası. Status filter already exists. The stock query has a fixed order.
 */
public final class FactorySlabGrid {

    private FactorySlabGrid() {
    }

    public static DataGridView view(Page<SlabView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "factory-slabs-table", columns(), FactorySlabGrid::row);
    }

    static GridRow row(SlabView slab) {
        return GridRow.of(null, null, null, null, List.of(
                SlabGridCells.status(slab),
                GridCell.chip(slab.getId()),
                SlabGridCells.code(slab),
                SlabGridCells.created(slab),
                SlabGridCells.block(slab),
                SlabGridCells.stone(slab),
                SlabGridCells.finish(slab),
                SlabGridCells.thickness(slab),
                SlabGridCells.dimensions(slab),
                SlabGridCells.area(slab),
                GridCell.text("1"),
                SlabGridCells.customer(slab)
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                SlabGridCells.statusColumn(),
                SlabGridCells.idColumn(),
                SlabGridCells.codeColumn(),
                GridColumn.text("createdDate", "Oluşturma Tarihi", false, 150),
                SlabGridCells.blockColumn(),
                SlabGridCells.stoneColumn(),
                SlabGridCells.finishColumn(),
                SlabGridCells.thicknessColumn(),
                SlabGridCells.dimensionsColumn(),
                SlabGridCells.areaColumn(),
                GridColumn.text("pieceCount", "Adet", false, 90),
                GridColumn.text("customerName", "Müşteri", false, 180)
        );
    }
}
