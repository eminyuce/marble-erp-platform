package com.ozerler.marble.model;

import com.ozerler.marble.domain.PalletCostAccumulator;
import com.ozerler.marble.model.enums.PackagingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Pallet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pallet_code", nullable = false, unique = true, length = 50)
    private String palletCode;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id")
    private StockLocation currentLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_type", nullable = false, length = 50)
    @Builder.Default
    private PackagingType packagingType = PackagingType.A_FRAME;

    @Column(name = "qr_code_hash", length = 100)
    private String qrCodeHash;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PREPARING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "gross_weight_kg", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grossWeightKg = BigDecimal.ZERO;

    @OneToMany(mappedBy = "pallet", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Slab> slabs = new ArrayList<>();

    @OneToMany(mappedBy = "pallet", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PalletItem> items = new ArrayList<>();

    @Transient
    public String getStatusLabel() {
        return com.ozerler.marble.model.enums.PalletStatus.labelOf(status);
    }

    @Transient
    public int getTotalLotCount() {
        return items != null ? items.size() : 0;
    }

    @Transient
    public BigDecimal getTotalAreaM2() {
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (PalletItem item : items) {
                if (item != null && item.getAreaM2() != null) {
                    total = total.add(item.getAreaM2());
                }
            }
        }
        if (slabs != null) {
            for (Slab slab : slabs) {
                if (slab != null && slab.getSurfaceAreaM2() != null) {
                    total = total.add(slab.getSurfaceAreaM2());
                }
            }
        }
        return total;
    }

    @Transient
    public int getTotalQuantity() {
        int qty = 0;
        if (items != null) {
            for (PalletItem item : items) {
                if (item != null && item.getQuantity() != null) {
                    qty += item.getQuantity();
                }
            }
        }
        if (slabs != null) {
            qty += slabs.size();
        }
        return qty;
    }

    @Transient
    public boolean isShippable() {
        return !"SHIPPED".equalsIgnoreCase(status) && (getTotalQuantity() > 0 || getTotalAreaM2().compareTo(BigDecimal.ZERO) > 0);
    }

    @Transient
    public BigDecimal getCurrentCostPerM2() {
        return PalletCostAccumulator.weightedAverageCostPerM2(areaCosts());
    }

    private List<PalletCostAccumulator.AreaCost> areaCosts() {
        List<PalletCostAccumulator.AreaCost> lines = new ArrayList<>();
        if (slabs != null && !slabs.isEmpty()) {
            for (Slab slab : slabs) {
                if (slab == null || slab.getSurfaceAreaM2() == null) {
                    continue;
                }
                lines.add(PalletCostAccumulator.line(slab.getSurfaceAreaM2(), slab.getCostPerM2()));
            }
        }
        if (items != null && !items.isEmpty()) {
            for (PalletItem item : items) {
                if (item == null || item.getAreaM2() == null) {
                    continue;
                }
                BigDecimal unitCost = BigDecimal.ZERO;
                if (item.getMaterialLot() != null && item.getMaterialLot().getUnitCost() != null) {
                    unitCost = item.getMaterialLot().getUnitCost();
                }
                lines.add(PalletCostAccumulator.line(item.getAreaM2(), unitCost));
            }
        }
        return lines;
    }
}
