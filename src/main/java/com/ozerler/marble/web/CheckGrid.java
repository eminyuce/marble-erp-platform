package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.CheckRef;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CheckStatus;

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
        String badge;
        if (check.getStatus() == null) {
            badge = "";
        } else {
            badge = check.getStatus().getLabel();
        }
        String value;
        if (check.getCustomer() == null) {
            value = "";
        } else {
            value = check.getCustomer().getCompanyName();
        }
        String valueValue;
        if (unit == null) {
            valueValue = "Genel";
        } else {
            valueValue = unit.getDisplayName();
        }
        return GridRow.of(null, null, null, null, List.of(
                GridCell.badge(badge),
                GridCell.chip(check.getId()),
                GridCell.text(check.getCheckNo()),
                GridCell.text(value),
                GridCell.text(valueValue),
                GridCell.text(check.getBankName()),
                GridCell.text(OperationGridSupport.day(check.getDueDate())),
                GridCell.text(GridTexts.money(Objects.requireNonNullElse(check.getAmount(), BigDecimal.ZERO)))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, GridWidth.PX_150, "status",
                        OperationGridSupport.choices(CheckStatus.values(), CheckStatus::getLabel)),
                GridColumn.text("id", "Id", false, GridWidth.PX_110),
                GridColumn.text("checkNo", "Çek No", false, GridWidth.PX_160),
                GridColumn.text("customerName", "Müşteri (Keşideci)", false, GridWidth.PX_200),
                GridColumn.text("businessUnitLabel", "İşletme Birimi", false, GridWidth.PX_150),
                GridColumn.text("bankName", "Banka", false, GridWidth.PX_160),
                GridColumn.text("dueDate", "Vade Tarihi", false, GridWidth.PX_140),
                GridColumn.text("amount", "Tutar", false, GridWidth.PX_140)
        );
    }
}
