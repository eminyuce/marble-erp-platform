package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pallet_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class PalletItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pallet_id", nullable = false)
    private Pallet pallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_lot_id")
    private MaterialLot materialLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id")
    private StockItem stockItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id")
    private Slab slab;

    @Column(name = "product_name", length = 150)
    private String productName;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "thickness_cm", precision = 8, scale = 2)
    private BigDecimal thicknessCm;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal areaM2 = BigDecimal.ZERO;

    @Transient
    public BigDecimal getEffectiveWidthCm() {
        if (widthCm != null) {
            return widthCm;
        }
        if (materialLot != null && materialLot.getWidthCm() != null) {
            return materialLot.getWidthCm();
        }
        if (stockItem != null && stockItem.getWidthCm() != null) {
            return stockItem.getWidthCm();
        }
        if (slab != null && slab.getWidthCm() != null) {
            return slab.getWidthCm();
        }
        return null;
    }

    @Transient
    public BigDecimal getEffectiveLengthCm() {
        if (lengthCm != null) {
            return lengthCm;
        }
        if (materialLot != null && materialLot.getLengthCm() != null) {
            return materialLot.getLengthCm();
        }
        if (stockItem != null && stockItem.getLengthCm() != null) {
            return stockItem.getLengthCm();
        }
        if (slab != null && slab.getLengthCm() != null) {
            return slab.getLengthCm();
        }
        return null;
    }

    @Transient
    public BigDecimal getEffectiveThicknessCm() {
        if (thicknessCm != null) {
            return thicknessCm;
        }
        if (materialLot != null && materialLot.getThicknessCm() != null) {
            return materialLot.getThicknessCm();
        }
        if (stockItem != null && stockItem.getThicknessCm() != null) {
            return stockItem.getThicknessCm();
        }
        if (slab != null && slab.getThicknessCm() != null) {
            return slab.getThicknessCm();
        }
        return null;
    }

    @Transient
    public String getDisplayName() {
        if (productName != null && !productName.isBlank()) {
            return productName;
        }
        if (materialLot != null) {
            String form;
            if (materialLot.getProductForm() != null) {
                form = materialLot.getProductForm().getLabel();
            } else {
                form = "";
            }
            String value;
            if (form.isEmpty()) {
                value = "";
            } else {
                value = " — " + form;
            }
            return materialLot.getLotCode() + value;
        }
        if (stockItem != null) {
            return stockItem.getItemCode()
                    + " ("
                    + Objects.requireNonNullElse(stockItem.getStoneType(), "Ebatlı")
                    + ")";
        }
        if (slab != null) {
            return slab.getSlabCode() + " (Plaka)";
        }
        return "Palet Ürünü #" + id;
    }
}
