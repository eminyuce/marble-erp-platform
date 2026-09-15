package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BusinessUnit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cost_centers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CostCenter extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "monthly_budget", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal monthlyBudget = BigDecimal.ZERO;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_unit", length = 30)
    private BusinessUnit businessUnit;
}
