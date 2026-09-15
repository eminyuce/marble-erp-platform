package com.ozerler.marble.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shipment_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class ShipmentItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id")
    private Pallet pallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_lot_id")
    private MaterialLot materialLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "area_m2", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal areaM2 = BigDecimal.ZERO;
}
