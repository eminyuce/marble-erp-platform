package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.CariTransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cari_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CariTransaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "transaction_date", nullable = false)
    @Builder.Default
    private LocalDate transactionDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    @Builder.Default
    private CariTransactionType transactionType = CariTransactionType.BORC_DEKONTU;

    @Column(name = "document_no", length = 60)
    private String documentNo;

    @Column(length = 255)
    private String description;

    @Column(name = "debit_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private CollectionRecord collection;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Transient
    public BigDecimal getNetImpact() {
        BigDecimal d = debitAmount != null ? debitAmount : BigDecimal.ZERO;
        BigDecimal c = creditAmount != null ? creditAmount : BigDecimal.ZERO;
        return d.subtract(c);
    }
}
