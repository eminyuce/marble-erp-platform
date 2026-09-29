package com.ozerler.marble.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class InvoiceItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    /**
     * Birim seçenekleri: m.t., m², m³, ton, adet
     */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String unit = "m2";

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /**
     * Kalem Toplamı = Miktar x Birim Fiyat. KDV kesinlikle yoktur.
     */
    @Column(name = "line_total", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id")
    private StockItem stockItem;

    public void calculateLineTotal() {
        if (quantity != null && unitPrice != null) {
            this.lineTotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.lineTotal = BigDecimal.ZERO;
        }
    }
}
