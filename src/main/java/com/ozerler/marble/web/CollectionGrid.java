package com.ozerler.marble.web;

import java.util.Objects;
import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.CollectionRef;
import com.ozerler.marble.model.enums.CollectionMethod;

/**
 * Tahsilat listesi. Method is the existing filter. There is no separate detail route.
 */
public final class CollectionGrid {

    private CollectionGrid() {
    }

    public static DataGridView view(Page<CollectionRef> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "collections-table", columns(), CollectionGrid::row);
    }

    static GridRow row(CollectionRef record) {
        String invoiceNo;
        if (record.getInvoice() == null) {
            invoiceNo = "";
        } else {
            invoiceNo = record.getInvoice().getInvoiceNo();
        }
        String invoiceUrl;
        if (record.getInvoice() == null) {
            invoiceUrl = null;
        } else {
            invoiceUrl = "/invoices/" + record.getInvoice().getId();
        }
        String badge;
        if (record.getCollectionMethod() == null) {
            badge = "";
        } else {
            badge = record.getCollectionMethod().getDisplayName();
        }
        String value;
        if (record.getCustomer() == null) {
            value = "";
        } else {
            value = record.getCustomer().getCompanyName();
        }
        GridCell valueValue;
        if (invoiceUrl == null) {
            valueValue = GridCell.text(invoiceNo);
        } else {
            valueValue = GridCell.link(invoiceNo, invoiceUrl);
        }
        return GridRow.of(null, null, null, null, List.of(
                GridCell.badge(badge),
                GridCell.chip(record.getId()),
                GridCell.text(record.getCollectionNo()),
                GridCell.text(OperationGridSupport.day(record.getCollectionDate())),
                GridCell.text(value),
                GridCell.text(record.getBankName()),
                valueValue,
                GridCell.text(GridTexts.money(Objects.requireNonNullElse(record.getAmount(), BigDecimal.ZERO))),
                GridCell.text(record.getNotes())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("method", "Yöntem", false, 150, "method",
                        OperationGridSupport.choices(CollectionMethod.values(), CollectionMethod::getDisplayName)),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("collectionNo", "Makbuz / Belge No", false, 180),
                GridColumn.text("collectionDate", "Tarih", false, 130),
                GridColumn.text("customerName", "Müşteri / Cari", false, 200),
                GridColumn.text("bankName", "Banka / Kasa", false, 160),
                GridColumn.text("invoiceNo", "İlişkili Fatura", false, 160),
                GridColumn.text("amount", "Tahsilat Tutarı", false, 150),
                GridColumn.text("notes", "Açıklama", false, 200)
        );
    }
}
