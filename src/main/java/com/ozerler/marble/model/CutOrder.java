package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.OperationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cut_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CutOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cut_order_no", nullable = false, unique = true, length = 50)
    private String cutOrderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private ProjectLocation location;

    @Column(name = "machine_name", nullable = false, length = 100)
    private String machineName;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "planned_start")
    private LocalDate plannedStart;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "cutOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CutItem> items = new ArrayList<>();

    @Transient
    public String getStatusLabel() {
        return OperationStatus.labelOf(status);
    }

}
