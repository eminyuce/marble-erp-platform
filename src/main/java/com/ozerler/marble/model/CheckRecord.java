package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CheckStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CheckRecord extends AuditableEntity {

    private static final int APPROACHING_DAYS = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private CollectionRecord collection;

    @Column(name = "check_no", nullable = false, length = 60)
    private String checkNo;

    @Column(name = "check_date", nullable = false)
    @Builder.Default
    private LocalDate checkDate = LocalDate.now();

    /**
     * Çek vade tarihi
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CheckStatus status = CheckStatus.PORTFOLIO;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_unit", length = 30)
    private BusinessUnit businessUnit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Transient
    public BusinessUnit getEffectiveBusinessUnit() {
        if (businessUnit != null) {
            return businessUnit;
        }
        if (customer != null) {
            return customer.getBusinessUnit();
        }
        return BusinessUnit.FACTORY;
    }

    @Transient
    public boolean isOverdue() {
        return status == CheckStatus.PORTFOLIO && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    @Transient
    public boolean isApproaching() {
        if (status != CheckStatus.PORTFOLIO || dueDate == null) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        return days >= 0 && days <= APPROACHING_DAYS;
    }

    @Transient
    public long getDaysRemaining() {
        if (dueDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    @Transient
    public LocalDate getIssueDate() {
        return checkDate;
    }

    @Transient
    public CollectionRecord getCollectionRecord() {
        return collection;
    }

    @Transient
    public String getCheckNumber() {
        return checkNo;
    }
}
