package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.PackagingType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Pallet extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pallet_code", nullable = false, unique = true, length = 50)
    private String palletCode;

    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_type", nullable = false, length = 50)
    @Builder.Default
    private PackagingType packagingType = PackagingType.A_FRAME;

    @Column(name = "qr_code_hash", length = 100)
    private String qrCodeHash;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "OPEN"; // OPEN, PACKED, LOADED, SHIPPED, DELIVERED

    @Column(name = "gross_weight_kg", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grossWeightKg = BigDecimal.ZERO;

    @OneToMany(mappedBy = "pallet", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Slab> slabs = new ArrayList<>();

}
