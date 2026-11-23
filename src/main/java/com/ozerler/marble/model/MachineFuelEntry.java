package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "machine_fuel_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class MachineFuelEntry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal litres;

    @Column(name = "price_per_litre", nullable = false, precision = 12, scale = 4)
    private BigDecimal pricePerLitre;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "working_hours_or_km", precision = 12, scale = 2)
    private BigDecimal workingHoursOrKm;

    @Column(name = "receipt_no", length = 60)
    private String receiptNo;

    @Column(name = "issued_by", length = 128)
    private String issuedBy;

    @Column(name = "received_by", length = 128)
    private String receivedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public void calculateTotal() {
        if (litres != null && pricePerLitre != null) {
            this.totalAmount = litres.multiply(pricePerLitre).setScale(2, RoundingMode.HALF_UP);
        }
    }
}
