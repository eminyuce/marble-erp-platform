package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.ProcessType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "production_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class ProductionOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @Column(name = "machine_name", nullable = false, length = 100)
    private String machineName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_work_order_id")
    private FactoryWorkOrder factoryWorkOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_operation_id")
    private FactoryOperation factoryOperation;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false, length = 50)
    private ProcessType processType;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_hours", precision = 6, scale = 2)
    private BigDecimal durationHours;

    @Column(name = "electricity_kwh", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal electricityKwh = BigDecimal.ZERO;

    @Column(name = "blade_wear_mm", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal bladeWearMm = BigDecimal.ZERO;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Slab> slabs = new ArrayList<>();

    @OneToMany(mappedBy = "productionOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ScrapLog> scrapLogs = new ArrayList<>();

    @Transient
    public String getStatusLabel() {
        return OperationStatus.labelOf(status);
    }

}
