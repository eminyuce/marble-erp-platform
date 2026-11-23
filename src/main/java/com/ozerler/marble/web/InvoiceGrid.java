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
        String rate;
        if (invoice.getTaxRate() == null) {
            rate = "";
        } else {
            rate = "%" + GridTexts.quantity(invoice.getTaxRate());
        }
        String badge;
        if (invoice.getStatus() == null) {
            badge = "Ödenmedi";
        } else {
            badge = invoice.getStatus().getDisplayName();
        }
        String badgeValue;
        if (invoice.getInvoiceType() == InvoiceType.SALES) {
            badgeValue = "↑ Satış";
        } else {
            badgeValue = "↓ Alış";
        }
        String value;
        if (invoice.getDepartment() == null) {
            value = "Genel";
        } else {
            value = invoice.getDepartment().getDisplayName();
        }
        return GridRow.of(viewUrl, null, null, null, List.of(
                GridCell.badge(badge),
                GridCell.chip(invoice.getId()),
                GridCell.link(invoice.getInvoiceNumber(), viewUrl),
                GridCell.text(OperationGridSupport.day(invoice.getInvoiceDate())),
                GridCell.badge(badgeValue),
                party(invoice),
                GridCell.text(value),
                GridCell.text(GridTexts.money(money(invoice.getSubtotalAmount()))),
                OperationGridSupport.lines(GridTexts.money(money(invoice.getTaxAmount())), rate),
                GridCell.text(GridTexts.money(money(invoice.getTotalAmount())))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("status", "Durum", false, GridWidth.PX_160, "status",
                        OperationGridSupport.choices(InvoiceStatus.values(), InvoiceStatus::getDisplayName)),
                GridColumn.text("id", "Id", false, GridWidth.PX_110),
                GridColumn.text("invoiceNumber", "Fatura No", false, GridWidth.PX_160),
                GridColumn.text("invoiceDate", "Tarih", false, GridWidth.PX_130),
                GridColumn.text("invoiceTypeLabel", "Tür", false, GridWidth.PX_120),
                GridColumn.text("partyName", "Cari / Hedef Birim", false, GridWidth.PX_220),
                GridColumn.text("departmentLabel", "Çıkış Birimi", false, GridWidth.PX_140),
                GridColumn.text("subtotalAmount", "Ara Toplam", false, GridWidth.PX_140),
                GridColumn.text("taxAmount", "KDV", false, GridWidth.PX_140),
                GridColumn.text("totalAmount", "Genel Toplam", false, GridWidth.PX_150)
        );
    }

    private static GridCell party(InvoiceView invoice) {
        if (invoice.getTargetDepartment() != null) {
            return GridCell.badge("Dahili: " + invoice.getTargetDepartment().getDisplayName());
        }
        String name = partyName(invoice);
        String project;
        if (invoice.getProject() == null) {
            project = "";
        } else {
            project = invoice.getProject().getName();
        }
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
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
