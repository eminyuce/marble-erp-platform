package com.ozerler.marble.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pallet_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class PalletItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pallet_id", nullable = false)
    private Pallet pallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_lot_id", nullable = false)
    private MaterialLot materialLot;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal areaM2 = BigDecimal.ZERO;
}
