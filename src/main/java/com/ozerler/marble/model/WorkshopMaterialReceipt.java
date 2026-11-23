package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import jakarta.persistence.Table;

import com.ozerler.marble.model.enums.WorkshopReceiptSource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workshop_material_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class WorkshopMaterialReceipt extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_no", nullable = false, unique = true, length = 50)
    private String receiptNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkshopReceiptSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id")
    private PurchaseOrderItem purchaseOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_lot_id")
    private MaterialLot materialLot;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(name = "area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal areaM2 = BigDecimal.ZERO;

    @Column(name = "purchase_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal purchaseCost = BigDecimal.ZERO;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
