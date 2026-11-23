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

import com.ozerler.marble.model.enums.SupplyRoute;
import com.ozerler.marble.model.enums.SurfaceFinish;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "construction_site_stone_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class ConstructionSiteStonePlan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private ProjectLocation location;

    @Column(name = "stone_type", nullable = false, length = 120)
    private String stoneType;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_finish", length = 30)
    private SurfaceFinish surfaceFinish;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "planned_area_m2", nullable = false, precision = 12, scale = 4)
    private BigDecimal plannedAreaM2;

    @Column(name = "scrap_percent", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal scrapPercent = BigDecimal.ZERO;

    @Column(name = "required_area_m2", nullable = false, precision = 12, scale = 4)
    private BigDecimal requiredAreaM2;

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_route", nullable = false, length = 40)
    @Builder.Default
    private SupplyRoute supplyRoute = SupplyRoute.INTERNAL_PRODUCTION;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
