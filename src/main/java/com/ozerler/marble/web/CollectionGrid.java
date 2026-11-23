package com.ozerler.marble.web;

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
        String invoiceNo = record.getInvoice() == null ? "" : record.getInvoice().getInvoiceNo();
        String invoiceUrl = record.getInvoice() == null ? null : "/invoices/" + record.getInvoice().getId();
        return GridRow.of(null, null, null, null, List.of(
                GridCell.badge(record.getCollectionMethod() == null ? "" : record.getCollectionMethod().getDisplayName()),
                GridCell.chip(record.getId()),
                GridCell.text(record.getCollectionNo()),
                GridCell.text(OperationGridSupport.day(record.getCollectionDate())),
                GridCell.text(record.getCustomer() == null ? "" : record.getCustomer().getCompanyName()),
                GridCell.text(record.getBankName()),
                invoiceUrl == null ? GridCell.text(invoiceNo) : GridCell.link(invoiceNo, invoiceUrl),
                GridCell.text(GridTexts.money(record.getAmount() == null ? BigDecimal.ZERO : record.getAmount())),
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
