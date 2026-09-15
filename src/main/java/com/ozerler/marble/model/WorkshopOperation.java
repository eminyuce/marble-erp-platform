package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.QuantityUnit;
import com.ozerler.marble.model.enums.WorkshopProcessType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "workshop_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class WorkshopOperation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cut_order_id", nullable = false)
    private CutOrder cutOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false, length = 40)
    private WorkshopProcessType processType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @Column(name = "operator_name", length = 120)
    private String operatorName;

    @Column(name = "labor_hours", precision = 8, scale = 2)
    private BigDecimal laborHours;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "input_area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal inputAreaM2 = BigDecimal.ZERO;

    @Column(name = "output_area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal outputAreaM2 = BigDecimal.ZERO;

    @Column(name = "waste_area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal wasteAreaM2 = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_unit", length = 30)
    @Builder.Default
    private QuantityUnit areaUnit = QuantityUnit.SQUARE_METER;

    @Column(name = "extra_expense", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal extraExpense = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OperationStatus status = OperationStatus.COMPLETED;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
