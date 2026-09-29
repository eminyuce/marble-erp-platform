package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.CheckStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CheckRecord extends AuditableEntity {

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

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Transient
    public boolean isOverdue() {
        return status == CheckStatus.PORTFOLIO && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    @Transient
    public boolean isApproaching() {
        if (status != CheckStatus.PORTFOLIO || dueDate == null) return false;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        return days >= 0 && days <= 15;
    }

    @Transient
    public long getDaysRemaining() {
        if (dueDate == null) return 0;
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
