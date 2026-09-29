package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Invoice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_no", nullable = false, unique = true, length = 60)
    private String invoiceNo;

    @Column(name = "invoice_date", nullable = false)
    @Builder.Default
    private LocalDate invoiceDate = LocalDate.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.SALES;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private BusinessUnit department = BusinessUnit.FACTORY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "party_name", length = 150)
    private String partyName;

    /**
     * KDV kullanılmamaktadır. Ara toplam doğrudan kalemlerin toplamıdır.
     */
    @Column(name = "subtotal_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    /**
     * KDV kullanılmamaktadır. Genel toplam ara toplama eşittir.
     */
    @Column(name = "total_amount", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    @Builder.Default
    private List<CollectionRecord> collections = new ArrayList<>();

    public void recalculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (InvoiceItem item : items) {
                if (item != null) {
                    item.calculateLineTotal();
                    if (item.getLineTotal() != null) {
                        sum = sum.add(item.getLineTotal());
                    }
                }
            }
        }
        this.subtotalAmount = sum;
        this.totalAmount = sum;
    }

    @Transient
    public String getPartyDisplayName() {
        if (customer != null) {
            return customer.getCompanyName();
        }
        if (supplier != null) {
            return supplier.getCompanyName();
        }
        return partyName != null ? partyName : "—";
    }

    @Transient
    public String getInvoiceNumber() {
        return invoiceNo;
    }
}
