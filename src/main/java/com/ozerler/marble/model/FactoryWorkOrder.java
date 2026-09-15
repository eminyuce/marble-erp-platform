package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.FactoryWorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factory_work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class FactoryWorkOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @Column(name = "accepted_at")
    private LocalDate acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_location_id")
    private StockLocation stockLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_machine_id")
    private Machine assignedMachine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private FactoryWorkOrderStatus status = FactoryWorkOrderStatus.ACCEPTED;

    @Column(name = "responsible_name", length = 120)
    private String responsibleName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FactoryOperation> operations = new ArrayList<>();
}
