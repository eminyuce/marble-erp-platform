package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.WorkshopReceiptSource;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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
