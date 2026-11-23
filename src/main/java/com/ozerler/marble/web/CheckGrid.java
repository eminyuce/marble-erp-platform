package com.ozerler.marble.web;

import com.ozerler.marble.dto.view.CheckRef;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CheckStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * Çek portföyü. Status filter already exists. Unit stays a query parameter.
 */
public final class CheckGrid {

    private CheckGrid() {
    }

    public static DataGridView view(Page<CheckRef> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "checks-table", columns(), CheckGrid::row);
    }

    static GridRow row(CheckRef check) {
        BusinessUnit unit = check.getEffectiveBusinessUnit();
        return GridRow.of(null, null, null, null, List.of(
                GridCell.badge(check.getStatus() == null ? "" : check.getStatus().getLabel()),
                GridCell.chip(check.getId()),
                GridCell.text(check.getCheckNo()),
                GridCell.text(check.getCustomer() == null ? "" : check.getCustomer().getCompanyName()),
                GridCell.text(unit == null ? "Genel" : unit.getDisplayName()),
                GridCell.text(check.getBankName()),
                GridCell.text(OperationGridSupport.day(check.getDueDate())),
                GridCell.text(GridTexts.money(check.getAmount() == null ? BigDecimal.ZERO : check.getAmount()))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, 150, "status",
                        OperationGridSupport.choices(CheckStatus.values(), CheckStatus::getLabel)),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("checkNo", "Çek No", false, 160),
                GridColumn.text("customerName", "Müşteri (Keşideci)", false, 200),
                GridColumn.text("businessUnitLabel", "İşletme Birimi", false, 150),
                GridColumn.text("bankName", "Banka", false, 160),
                GridColumn.text("dueDate", "Vade Tarihi", false, 140),
                GridColumn.text("amount", "Tutar", false, 140)
        );
    }
}
