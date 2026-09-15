package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cost_period_closes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CostPeriodClose extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_unit", nullable = false, length = 30)
    private BusinessUnit businessUnit;

    @Column(name = "expense_period", nullable = false, length = 7)
    private String expensePeriod;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 128)
    private String closedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
