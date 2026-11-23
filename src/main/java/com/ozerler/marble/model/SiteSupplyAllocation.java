package com.ozerler.marble.model;

import java.math.BigDecimal;

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
