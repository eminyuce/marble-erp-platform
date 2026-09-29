package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.CollectionMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "collections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class CollectionRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_no", nullable = false, unique = true, length = 60)
    private String collectionNo;

    @Column(name = "collection_date", nullable = false)
    @Builder.Default
    private LocalDate collectionDate = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_method", nullable = false, length = 30)
    @Builder.Default
    private CollectionMethod collectionMethod = CollectionMethod.CASH;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToOne(mappedBy = "collection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CheckRecord checkRecord;

    @Transient
    public CollectionMethod getMethod() {
        return collectionMethod;
    }
}
