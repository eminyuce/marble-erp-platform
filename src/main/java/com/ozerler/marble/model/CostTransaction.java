package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.ExpenseType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cost_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CostTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "center_id", nullable = false)
    private CostCenter costCenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id")
    private Slab slab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 50)
    private ExpenseType expenseType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "allocation_key", length = 100)
    private String allocationKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_unit", length = 30)
    private BusinessUnit businessUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", length = 40)
    private ExpenseCategory expenseCategory;

    @Column(length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Column(name = "document_no", length = 60)
    private String documentNo;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "expense_period", length = 7)
    private String expensePeriod;

    @Column(name = "posting_period", length = 7)
    private String postingPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_operation_id")
    private FactoryOperation factoryOperation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_operation_id")
    private WorkshopOperation workshopOperation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "construction_site_id")
    private Project constructionSite;
}
