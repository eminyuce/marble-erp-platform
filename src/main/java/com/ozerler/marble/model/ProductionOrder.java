package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.ProcessType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @Column(name = "machine_name", nullable = false, length = 100)
    private String machineName;

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

}
