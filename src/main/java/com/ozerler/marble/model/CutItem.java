package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

import com.ozerler.marble.model.enums.CutItemStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cut_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CutItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, unique = true, length = 60)
    private String itemCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_order_id", nullable = false)
    private CutOrder cutOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_slab_id", nullable = false)
    private Slab sourceSlab;

    @Column(name = "width_cm", nullable = false, precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", nullable = false, precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "thickness_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal thicknessCm;

    @Column(name = "area_m2", nullable = false, precision = 10, scale = 4)
    private BigDecimal areaM2;

    @Column(name = "edge_finish", length = 100)
    private String edgeFinish;

    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "target_location", length = 150)
    private String targetLocation;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "READY";

    @Transient
    public String getStatusLabel() {
        return CutItemStatus.labelOf(status);
    }

    public void calculateArea() {
        if (widthCm != null && lengthCm != null) {
            this.areaM2 = widthCm.multiply(lengthCm)
                    .divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP);
        }
    }
}
