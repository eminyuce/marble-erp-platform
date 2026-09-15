package com.ozerler.marble.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "site_installations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class SiteInstallation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private ProjectLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_lot_id")
    private MaterialLot materialLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pallet_id")
    private Pallet pallet;

    @Column(name = "installed_area_m2", nullable = false, precision = 12, scale = 4)
    private BigDecimal installedAreaM2;

    @Column(name = "waste_area_m2", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal wasteAreaM2 = BigDecimal.ZERO;

    @Column(name = "installed_on", nullable = false)
    private LocalDate installedOn;

    @Column(name = "crew_name", length = 120)
    private String crewName;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
