package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "slabs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Slab extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slab_code", nullable = false, unique = true, length = 60)
    private String slabCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id")
    private Pallet pallet;

    @Column(name = "thickness_cm", nullable = false, precision = 5, scale = 2)
    private BigDecimal thicknessCm;

    @Column(name = "width_cm", nullable = false, precision = 8, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", nullable = false, precision = 8, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "surface_area_m2", nullable = false, precision = 10, scale = 4)
    private BigDecimal surfaceAreaM2;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_finish", nullable = false, length = 50)
    @Builder.Default
    private SurfaceFinish surfaceFinish = SurfaceFinish.RAW;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_grade", nullable = false, length = 20)
    @Builder.Default
    private QualityGrade qualityGrade = QualityGrade.A;

    @Column(name = "gloss_level")
    @Builder.Default
    private Integer glossLevel = 0;

    @Column(name = "cost_per_m2", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal costPerM2 = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SlabStatus status = SlabStatus.AVAILABLE;


    public void calculateArea() {
        if (widthCm != null && lengthCm != null) {
            this.surfaceAreaM2 = widthCm.multiply(lengthCm)
                    .divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP);
        }
    }
}
