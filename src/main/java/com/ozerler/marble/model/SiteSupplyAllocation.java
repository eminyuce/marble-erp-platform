package com.ozerler.marble.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "site_supply_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class SiteSupplyAllocation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stone_plan_id", nullable = false)
    private ConstructionSiteStonePlan stonePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_work_order_id")
    private FactoryWorkOrder factoryWorkOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_order_id")
    private CutOrder cutOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_item_id")
    private PurchaseOrderItem purchaseOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_reservation_id")
    private StockReservation stockReservation;

    @Column(name = "allocated_area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal allocatedAreaM2 = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
