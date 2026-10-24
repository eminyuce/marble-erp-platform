package com.ozerler.marble.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "operation_work_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class OperationWorkOrderItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private OperationWorkOrder workOrder;

    @Column(name = "thickness_cm", precision = 8, scale = 2)
    private BigDecimal thicknessCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "piece_count", nullable = false)
    @Builder.Default
    private Integer pieceCount = 1;

    @Column(name = "calculated_m2", precision = 10, scale = 4)
    private BigDecimal calculatedM2;

    @Column(name = "calculated_mt", precision = 10, scale = 4)
    private BigDecimal calculatedMt;

    @Column(name = "surface_operation", length = 80)
    private String surfaceOperation;

    @Column(name = "surface_unit", length = 20)
    @Builder.Default
    private String surfaceUnit = "m2";

    @Column(name = "edge_operation", length = 80)
    private String edgeOperation;

    @Column(name = "edge_unit", length = 20)
    @Builder.Default
    private String edgeUnit = "m.t.";

    @Column(length = 255)
    private String notes;

    @Column(name = "item_order")
    @Builder.Default
    private Integer itemOrder = 0;

    @PrePersist
    @PreUpdate
    public void computeMetrics() {
        int count = pieceCount != null ? Math.max(1, pieceCount) : 1;
        if (widthCm != null && lengthCm != null) {
            this.calculatedM2 = widthCm.multiply(lengthCm)
                    .multiply(BigDecimal.valueOf(count))
                    .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
        }
        if (lengthCm != null) {
            this.calculatedMt = lengthCm.multiply(BigDecimal.valueOf(count))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
    }
}
