package com.ozerler.marble.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.ozerler.marble.domain.BlockMeasurement;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.TargetDestination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class Block extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarry_id", nullable = false)
    private Quarry quarry;

    @Column(name = "block_code", nullable = false, unique = true, length = 50)
    private String blockCode;

    @Column(name = "quarry_section", length = 50)
    private String quarrySection;

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

    @Column(name = "stone_type", length = 100)
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
    private BlockStatus status = BlockStatus.PRODUCED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id")
    private StockLocation currentLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sold_customer_id")
    private Customer soldCustomer;

    @Column(name = "estimated_tonnage", precision = 10, scale = 3)
    private BigDecimal estimatedTonnage;

    @Column(name = "actual_tonnage", precision = 10, scale = 3)
    private BigDecimal actualTonnage;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_destination", length = 40)
    @Builder.Default
    private TargetDestination targetDestination = TargetDestination.FACTORY;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_customer_id")
    private Customer assignedCustomer;

    @Column(name = "dispatch_invoice_id")
    private Long dispatchInvoiceId;

    @Column(name = "extraction_cost", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal extractionCost = BigDecimal.ZERO;

    @Column(name = "sale_price", precision = 14, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "sale_date")
    private LocalDate saleDate;

    @Column(name = "sale_notes", columnDefinition = "TEXT")
    private String saleNotes;

    /** Satılmayan bloklar için ton başına tahmini piyasa değeri (TL/ton). */
    @Column(name = "unit_market_value_per_ton", precision = 14, scale = 2)
    private BigDecimal unitMarketValuePerTon;

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


    public void calculateMetrics(BigDecimal specificGravity) {
        if (widthCm != null && lengthCm != null && heightCm != null) {
            this.volumeM3 = BlockMeasurement.volumeCubicMeters(widthCm, lengthCm, heightCm);
            BigDecimal density;
            if (specificGravity != null) {
                density = specificGravity;
            } else {
                density = new BigDecimal("2.70");
            }
            BigDecimal approximateTonnage = BlockMeasurement.approximateTonnage(this.volumeM3, density);
            this.theoreticalWeightKg = BlockMeasurement.theoreticalWeightKg(approximateTonnage);
            if (this.estimatedTonnage == null || this.estimatedTonnage.compareTo(BigDecimal.ZERO) == 0) {
                this.estimatedTonnage = approximateTonnage;
            }
            if (this.actualTonnage != null && this.actualTonnage.compareTo(BigDecimal.ZERO) > 0) {
                this.actualWeightKg = this.actualTonnage.multiply(BigDecimal.valueOf(1000));
            } else if (this.actualWeightKg != null && this.actualWeightKg.compareTo(BigDecimal.ZERO) > 0) {
                this.actualTonnage = this.actualWeightKg.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            }
            this.weightDeviationPct = BlockMeasurement.weightDeviationPercent(this.actualWeightKg, this.theoreticalWeightKg);
        }

        BigDecimal ext;
        if (extractionCost != null) {
            ext = extractionCost;
        } else {
            ext = BigDecimal.ZERO;
        }
        BigDecimal trp;
        if (transportCost != null) {
            trp = transportCost;
        } else {
            trp = BigDecimal.ZERO;
        }
        this.totalCost = ext.add(trp);
    }

    @Transient
    public BigDecimal getApproximateTonnage() {
        if (estimatedTonnage != null) {
            return estimatedTonnage;
        }
        return BlockMeasurement.kilogramsToTons(theoreticalWeightKg);
    }

    @Transient
    public BigDecimal getActualTonnage() {
        if (actualTonnage != null) {
            return actualTonnage;
        }
        if (actualWeightKg != null && actualWeightKg.compareTo(BigDecimal.ZERO) > 0) {
            return BlockMeasurement.kilogramsToTons(actualWeightKg);
        }
        return null;
    }

    @Transient
    public BigDecimal getTonnageDeviation() {
        BigDecimal est = getApproximateTonnage();
        BigDecimal act = getActualTonnage();
        if (est != null && act != null) {
            return act.subtract(est);
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public boolean isWeightDeviationWarning() {
        return BlockMeasurement.hasActualWeight(actualWeightKg)
                && BlockMeasurement.exceedsDeviationWarning(weightDeviationPct);
    }

    @Transient
    public BlockStatus getCanonicalStatus() {
        if (status == null) {
            return BlockStatus.PRODUCED;
        }
        return status.canonical();
    }

    @Transient
    public String getQuarryLocation() {
        return quarrySection;
    }

    @Transient
    public String getColorQuality() {
        String value;
        if (qualityGrade != null) {
            value = " / " + qualityGrade.getLabel();
        } else {
            value = "";
        }
        return Objects.requireNonNullElse(colorTone, "") + value;
    }

    @Transient
    public String getDimensions() {
        String value;
        if (lengthCm != null) {
            value = lengthCm + "×";
        } else {
            value = "";
        }
        String valueValue;
        if (widthCm != null) {
            valueValue = widthCm + "×";
        } else {
            valueValue = "";
        }
        String valueValue2;
        if (heightCm != null) {
            valueValue2 = heightCm + " cm";
        } else {
            valueValue2 = "";
        }
        return value + valueValue + valueValue2;
    }

    @Transient
    public String getQuarryName() {
        if (quarry != null) {
            return quarry.getName();
        }
        return null;
    }

    @Transient
    public BigDecimal getWeightTons() {
        return getApproximateTonnage();
    }

    @Transient
    public Customer getCustomer() {
        return soldCustomer;
    }

    @Transient
    public boolean isAtQuarry() {
        return status != null && status.isAtQuarry();
    }

    @Transient
    public boolean isAtFactory() {
        return status != null && status.isAtFactory();
    }

    @Transient
    public BigDecimal getEffectiveTonnage() {
        if (actualTonnage != null && actualTonnage.compareTo(BigDecimal.ZERO) > 0) {
            return actualTonnage;
        }
        return getApproximateTonnage();
    }
}
