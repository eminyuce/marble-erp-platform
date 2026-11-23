package com.ozerler.marble.model;

import java.math.BigDecimal;

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

import com.ozerler.marble.model.enums.ChamferStatus;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.model.enums.ProductForm;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SurfaceFinish;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "material_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class MaterialLot extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lot_code", nullable = false, unique = true, length = 60)
    private String lotCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_form", nullable = false, length = 30)
    private ProductForm productForm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_operation_id")
    private FactoryOperation sourceFactoryOperation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_workshop_operation_id")
    private WorkshopOperation sourceWorkshopOperation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_block_id")
    private Block sourceBlock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id")
    private Slab slab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_item_id")
    private CutItem cutItem;

    @Column(name = "stone_type", length = 100)
    private String stoneType;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_grade", length = 20)
    private QualityGrade qualityGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_finish", length = 30)
    private SurfaceFinish surfaceFinish;

    @Column(name = "thickness_cm", precision = 8, scale = 2)
    private BigDecimal thicknessCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "total_area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal totalAreaM2 = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "chamfer_status", length = 30)
    @Builder.Default
    private ChamferStatus chamferStatus = ChamferStatus.NOT_APPLICABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_location_id")
    private StockLocation stockLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MaterialLotStatus status = MaterialLotStatus.AVAILABLE;

    @Column(name = "unit_cost", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(name = "total_cost", precision = 16, scale = 2)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;
}
