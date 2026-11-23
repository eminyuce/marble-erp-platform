package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.Version;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CollectionMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "business_unit", length = 30)
    private BusinessUnit businessUnit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToOne(mappedBy = "collection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CheckRecord checkRecord;

    @Transient
    public CollectionMethod getMethod() {
        return collectionMethod;
    }
}
