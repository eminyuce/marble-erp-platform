package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.model.enums.SurfaceFinish;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "stock_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class StockItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, unique = true, length = 60)
    private String itemCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    private StockProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_block_id")
    private Block sourceBlock;

    @Column(name = "stone_type", length = 100)
    private String stoneType;

    @Column(length = 255)
    private String description;

    @Column(name = "thickness_cm", precision = 8, scale = 2)
    private BigDecimal thicknessCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(length = 20)
    @Builder.Default
    private String unit = "m2";

    @Column(name = "piece_count", nullable = false)
    @Builder.Default
    private Integer pieceCount = 1;

    @Column(name = "actual_produced_quantity", precision = 12, scale = 4)
    private BigDecimal actualProducedQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_location_id")
    private StockLocation stockLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_grade", length = 20)
    @Builder.Default
    private QualityGrade qualityGrade = QualityGrade.A;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_finish", length = 50)
    @Builder.Default
    private SurfaceFinish surfaceFinish = SurfaceFinish.RAW;

    @Column(name = "edge_finish", length = 50)
    @Builder.Default
    private String edgeFinish = "DUZ";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "production_date", nullable = false)
    @Builder.Default
    private LocalDate productionDate = LocalDate.now();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Transient
    public String getCustomerDisplayName() {
        return customer != null ? customer.getCompanyName() : "Genel Stok";
    }

    @Transient
    public boolean isReserved() {
        return customer != null || "RESERVED".equalsIgnoreCase(status);
    }
}
