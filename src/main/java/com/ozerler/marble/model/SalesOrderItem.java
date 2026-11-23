package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
@Table(name = "sales_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class SalesOrderItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id")
    private Slab slab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(nullable = false, precision = 14, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String unit = "m2";

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;


    public void calculateLineTotal() {
        this.lineTotal = this.quantity.multiply(this.unitPrice)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
