package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class WorkOrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private OperationWorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private OperationWorkOrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private OperationWorkOrderStatus newStatus;

    @Column(name = "changed_by", length = 128)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String notes;
}
