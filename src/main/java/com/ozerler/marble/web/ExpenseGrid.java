package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.ExpenseType;

/**
 * Gider listesi. Sort keys match {@code GridPages.EXPENSE_SORTS}. Type filter already exists.
 */
public final class ExpenseGrid {

    private ExpenseGrid() {
    }

    public static DataGridView view(TabulatorResponse<ExpenseDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (ExpenseDto expense : response.getData()) {
                rows.add(row(expense));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "expenses-table", 4, columns(), rows);
    }

    public static GridRow row(ExpenseDto expense) {
        String id;
        if (expense.getId() == null) {
            id = "";
        } else {
            id = expense.getId().toString();
        }
        String viewUrl = "/expenses/" + id;
        String editUrl;
        if (expense.isCanEdit()) {
            editUrl = viewUrl + "/edit";
        } else {
            editUrl = "";
        }
        Object deleteId;
        if (expense.isCanDelete()) {
            deleteId = expense.getId();
        } else {
            deleteId = null;
        }
        String amount = GridTexts.money(expense.getAmount());
        String currency = GridWindow.clean(expense.getCurrency());
        if (!amount.isBlank() && !currency.isBlank()) {
            amount = amount + " " + currency;
        }
        return GridRow.of(viewUrl, editUrl, deleteId, expense.getDocumentNo(), List.of(
                GridCell.chip(expense.getId()),
                GridCell.link(GridWindow.clean(expense.getDocumentNo()), viewUrl, GridTexts.date(expense.getInvoiceDate()), null),
                GridCell.link(GridWindow.clean(expense.getCostCenterName()), viewUrl, GridWindow.clean(expense.getCostCenterCode()), null),
                GridCell.text(GridTexts.date(expense.getEntryDate())),
                GridCell.text(GridWindow.clean(expense.getBusinessUnitLabel())),
                GridCell.text(GridWindow.clean(expense.getTargetName())),
                GridCell.text(GridWindow.clean(expense.getExpenseTypeLabel())),
                GridCell.text(GridWindow.clean(expense.getExpensePeriod())),
                GridCell.text(amount),
                GridCell.text(GridWindow.clean(expense.getDescription()))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("documentNo", "Belge / Fatura", true, GridWidth.PX_160),
                GridColumn.text("costCenterName", "Masraf Merkezi", true, GridWidth.PX_180),
                GridColumn.text("entryDate", "Tarih", true, GridWidth.PX_120),
                GridColumn.text("businessUnit", "Birim", true, GridWidth.PX_130),
                GridColumn.text("", "İlgili Yer", false, GridWidth.PX_150),
                GridColumn.filtered("expenseType", "Gider Kalemi", true, GridWidth.PX_160, "expenseType", types()),
                GridColumn.text("expensePeriod", "Dönem", true, GridWidth.PX_110),
                GridColumn.text("amount", "Tutar", true, GridWidth.PX_150),
                GridColumn.text("", "Açıklama", false, GridWidth.PX_200)
        );
    }

    private static List<GridColumn.FilterChoice> types() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (ExpenseType type : ExpenseType.values()) {
            choices.add(new GridColumn.FilterChoice(type.name(), type.getLabel()));
        }
        return choices;
    }
}
