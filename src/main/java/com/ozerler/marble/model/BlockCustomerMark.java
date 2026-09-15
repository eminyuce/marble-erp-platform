package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BlockMarkStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "block_customer_marks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class BlockCustomerMark extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "marked_at", nullable = false)
    private LocalDate markedAt;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "offer_price", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal offerPrice = BigDecimal.ZERO;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "TRY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private BlockMarkStatus status = BlockMarkStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_location_id")
    private StockLocation stockLocationSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_item_id")
    private SalesOrderItem salesOrderItem;
}
