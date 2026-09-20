package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.ChamferStatus;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.QuantityUnit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "factory_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class FactoryOperation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private FactoryWorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_operation_id")
    private FactoryOperation previousOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false, length = 40)
    private FactoryProcessType processType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @Column(name = "operator_name", length = 120)
    private String operatorName;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "input_quantity", precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal inputQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_unit", length = 30)
    private QuantityUnit inputUnit;

    @Column(name = "output_quantity", precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal outputQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_unit", length = 30)
    private QuantityUnit outputUnit;

    @Column(name = "waste_quantity", precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal wasteQuantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_unit", length = 30)
    private QuantityUnit wasteUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "chamfer_status", length = 30)
    @Builder.Default
    private ChamferStatus chamferStatus = ChamferStatus.NOT_APPLICABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OperationStatus status = OperationStatus.PLANNED;

    @Column(name = "labor_cost", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal laborCost = BigDecimal.ZERO;

    @Column(name = "electricity_cost", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal electricityCost = BigDecimal.ZERO;

    @Column(name = "consumable_cost", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal consumableCost = BigDecimal.ZERO;

    @Column(name = "total_operation_cost", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalOperationCost = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public BigDecimal operationCostTotal() {
        return zero(laborCost).add(zero(electricityCost)).add(zero(consumableCost));
    }

    private static BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
