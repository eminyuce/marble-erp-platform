package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarry_id", nullable = false)
    private Quarry quarry;

    @Column(name = "block_code", nullable = false, unique = true, length = 50)
    private String blockCode;

    @Column(name = "extraction_date", nullable = false)
    private LocalDate extractionDate;

    @Column(name = "width_cm", nullable = false)
    private Integer widthCm;

    @Column(name = "length_cm", nullable = false)
    private Integer lengthCm;

    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Column(name = "volume_m3", nullable = false, precision = 10, scale = 3)
    private BigDecimal volumeM3;

    @Column(name = "theoretical_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal theoreticalWeightKg;

    @Column(name = "actual_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal actualWeightKg;

    @Column(name = "weight_deviation_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightDeviationPct;

    @Column(name = "stone_type", nullable = false, length = 100)
    private String stoneType;

    @Column(name = "color_tone", length = 100)
    private String colorTone;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_grade", nullable = false, length = 20)
    @Builder.Default
    private QualityGrade qualityGrade = QualityGrade.A;

    @Column(name = "crack_level", nullable = false)
    @Builder.Default
    private Integer crackLevel = 0; // 0-Yok, 1-Yüzeysel, 2-Kritik/Derin

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BlockStatus status = BlockStatus.QUARRY;

    @Column(name = "extraction_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal extractionCost = BigDecimal.ZERO;

    @Column(name = "transport_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal transportCost = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void calculateMetrics(BigDecimal specificGravity) {
        if (widthCm != null && lengthCm != null && heightCm != null) {
            BigDecimal vol = BigDecimal.valueOf((long) widthCm * lengthCm * heightCm)
                    .divide(new BigDecimal("1000000"), 3, RoundingMode.HALF_UP);
            this.volumeM3 = vol;

            BigDecimal density = specificGravity != null ? specificGravity : new BigDecimal("2.70");
            this.theoreticalWeightKg = vol.multiply(density).multiply(new BigDecimal("1000"))
                    .setScale(2, RoundingMode.HALF_UP);

            if (this.actualWeightKg != null && this.theoreticalWeightKg.compareTo(BigDecimal.ZERO) > 0) {
                this.weightDeviationPct = this.actualWeightKg.subtract(this.theoreticalWeightKg)
                        .divide(this.theoreticalWeightKg, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                this.weightDeviationPct = BigDecimal.ZERO;
            }
        }

        BigDecimal ext = extractionCost != null ? extractionCost : BigDecimal.ZERO;
        BigDecimal trp = transportCost != null ? transportCost : BigDecimal.ZERO;
        this.totalCost = ext.add(trp);
    }
}
