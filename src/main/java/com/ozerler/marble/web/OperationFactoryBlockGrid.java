package com.ozerler.marble.web;

import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.view.BlockView;

/**
 * Fabrika kesilmemiş blok sahası. Sort is not supported by the stock query. There is no status filter.
 */
public final class OperationFactoryBlockGrid {

    private OperationFactoryBlockGrid() {
    }

    public static DataGridView view(Page<BlockView> page, int number, int size, String sort, String direction) {
        return OperationGridSupport.pageOf(page, number, size, sort, direction,
                "factory-blocks-table", columns(), OperationFactoryBlockGrid::row);
    }

    static GridRow row(BlockView block) {
        String viewUrl = "/blocks/" + block.getId();
        String value;
        if (block.getActualTonnage() == null) {
            value = "Girilmedi";
        } else {
            value = OperationGridSupport.amount(block.getActualTonnage(), "t");
        }
        String customer;
        if (block.getAssignedCustomer() == null) {
            customer = null;
        } else {
            customer = block.getAssignedCustomer().getCompanyName();
        }
        return GridRow.of(viewUrl, null, null, null, List.of(
                GridCell.badge("Kesilmemiş Blok"),
                GridCell.chip(block.getId()),
                GridCell.link(block.getBlockCode(), viewUrl),
                GridCell.text(block.getStoneType()),
                GridCell.text(quarry(block)),
                GridCell.text(arrived(block)),
                GridCell.text(OperationGridSupport.amount(block.getApproximateTonnage(), "t")),
                GridCell.text(value),
                GridCell.text(block.getDimensions()),
                GridCell.text(OperationGridSupport.customer(customer)),
                GridCell.text(GridTexts.money(Objects.requireNonNullElse(block.getTotalCost(), BigDecimal.ZERO))),
                GridCell.text(GridTexts.money(saleValue(block)))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("status", "Durum", false, 160),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("blockCode", "Blok No", false, 140),
                GridColumn.text("stoneType", "Taş Türü", false, 130),
                GridColumn.text("quarryName", "Kaynak Ocak", false, 140),
                GridColumn.text("arrivalDate", "Geliş Tarihi", false, 130),
                GridColumn.text("estimatedTonnage", "Tahmini Tonaj", false, 140),
                GridColumn.text("actualTonnage", "Gerçek Tonaj", false, 140),
                GridColumn.text("dimensions", "Ölçüler", false, 140),
                GridColumn.text("customerName", "Müşteri Durumu", false, 180),
                GridColumn.text("totalCost", "Maliyet Değeri", false, 150),
                GridColumn.text("salePrice", "Satış / Değer", false, 150)
        );
    }

    private static String quarry(BlockView block) {
        if (block.getQuarry() != null && block.getQuarry().getName() != null) {
            return block.getQuarry().getName();
        }
        return OperationGridSupport.named(block.getQuarryName(), "Merkez Ocak");
    }

    private static String arrived(BlockView block) {
        LocalDate arrival = block.getArrivalDate();
        if (arrival != null) {
            return GridTexts.date(arrival);
        }
        return OperationGridSupport.day(block.getCreatedDate());
    }

    private static BigDecimal saleValue(BlockView block) {
        BigDecimal cost;
        if (block.getTotalCost() == null) {
            cost = BigDecimal.ZERO;
        } else {
            cost = block.getTotalCost();
        }
        BigDecimal sale = block.getSalePrice();
        if ((sale == null || sale.compareTo(BigDecimal.ZERO) <= 0) && block.getUnitMarketValuePerTon() != null) {
            BigDecimal ton = block.getActualTonnage();
            if (ton == null) {
                if (block.getApproximateTonnage() == null) {
                    ton = BigDecimal.ZERO;
                } else {
                    ton = block.getApproximateTonnage();
                }
            }
            sale = block.getUnitMarketValuePerTon().multiply(ton).setScale(2, RoundingMode.HALF_UP);
        }
        if (sale == null || sale.compareTo(BigDecimal.ZERO) <= 0) {
            return cost;
        }
        return sale;
    }
}
