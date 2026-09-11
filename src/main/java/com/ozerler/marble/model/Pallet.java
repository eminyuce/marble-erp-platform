package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.PackagingType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Pallet {

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
