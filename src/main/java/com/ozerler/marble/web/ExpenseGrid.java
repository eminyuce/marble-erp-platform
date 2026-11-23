package com.ozerler.marble.web;

import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.ExpenseType;

import java.util.ArrayList;
import java.util.List;

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
        String id = expense.getId() == null ? "" : expense.getId().toString();
        String viewUrl = "/expenses/" + id;
        String editUrl = expense.isCanEdit() ? viewUrl + "/edit" : "";
        Object deleteId = expense.isCanDelete() ? expense.getId() : null;
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
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("documentNo", "Belge / Fatura", true, 160),
                GridColumn.text("costCenterName", "Masraf Merkezi", true, 180),
                GridColumn.text("entryDate", "Tarih", true, 120),
                GridColumn.text("businessUnit", "Birim", true, 130),
                GridColumn.text("", "İlgili Yer", false, 150),
                GridColumn.filtered("expenseType", "Gider Kalemi", true, 160, "expenseType", types()),
                GridColumn.text("expensePeriod", "Dönem", true, 110),
                GridColumn.text("amount", "Tutar", true, 150),
                GridColumn.text("", "Açıklama", false, 200)
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
