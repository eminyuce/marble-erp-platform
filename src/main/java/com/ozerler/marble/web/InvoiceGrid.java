package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.InvoiceView;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;

/**
 * Fatura listesi. Status filter already exists. Type and department stay as query parameters.
 */
public final class InvoiceGrid {

    private InvoiceGrid() {
    }

    public static DataGridView view(Page<InvoiceView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "invoices-table", columns(), InvoiceGrid::row);
    }

    static GridRow row(InvoiceView invoice) {
        String viewUrl = "/invoices/" + invoice.getId();
        String rate = invoice.getTaxRate() == null ? "" : "%" + GridTexts.quantity(invoice.getTaxRate());
        return GridRow.of(viewUrl, null, null, null, List.of(
                GridCell.badge(invoice.getStatus() == null ? "Ödenmedi" : invoice.getStatus().getDisplayName()),
                GridCell.chip(invoice.getId()),
                GridCell.link(invoice.getInvoiceNumber(), viewUrl),
                GridCell.text(OperationGridSupport.day(invoice.getInvoiceDate())),
                GridCell.badge(invoice.getInvoiceType() == InvoiceType.SALES ? "↑ Satış" : "↓ Alış"),
                party(invoice),
                GridCell.text(invoice.getDepartment() == null ? "Genel" : invoice.getDepartment().getDisplayName()),
                GridCell.text(GridTexts.money(money(invoice.getSubtotalAmount()))),
                OperationGridSupport.lines(GridTexts.money(money(invoice.getTaxAmount())), rate),
                GridCell.text(GridTexts.money(money(invoice.getTotalAmount())))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, 160, "status",
                        OperationGridSupport.choices(InvoiceStatus.values(), InvoiceStatus::getDisplayName)),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("invoiceNumber", "Fatura No", false, 160),
                GridColumn.text("invoiceDate", "Tarih", false, 130),
                GridColumn.text("invoiceTypeLabel", "Tür", false, 120),
                GridColumn.text("partyName", "Cari / Hedef Birim", false, 220),
                GridColumn.text("departmentLabel", "Çıkış Birimi", false, 140),
                GridColumn.text("subtotalAmount", "Ara Toplam", false, 140),
                GridColumn.text("taxAmount", "KDV", false, 140),
                GridColumn.text("totalAmount", "Genel Toplam", false, 150)
        );
    }

    private static GridCell party(InvoiceView invoice) {
        if (invoice.getTargetDepartment() != null) {
            return GridCell.badge("Dahili: " + invoice.getTargetDepartment().getDisplayName());
        }
        String name = partyName(invoice);
        String project = invoice.getProject() == null ? "" : invoice.getProject().getName();
        return OperationGridSupport.lines(name, project);
    }

    private static String partyName(InvoiceView invoice) {
        if (invoice.getCustomer() != null) {
            return invoice.getCustomer().getCompanyName();
        }
        if (invoice.getSupplier() != null) {
            return invoice.getSupplier().getCompanyName();
        }
        return invoice.getPartyName();
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
