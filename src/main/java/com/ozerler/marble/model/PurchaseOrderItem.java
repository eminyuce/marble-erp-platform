package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.PurchaseItemType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class PurchaseOrderItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "item_name", nullable = false, length = 300)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    @Builder.Default
    private PurchaseItemType itemType = PurchaseItemType.CONSUMABLE;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String unit = "ADET";

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Column(name = "delivery_status", nullable = false, length = 30)
    @Builder.Default
    private String deliveryStatus = "PENDING";


    public void calculateLineTotal() {
        this.lineTotal = this.quantity.multiply(this.unitPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
