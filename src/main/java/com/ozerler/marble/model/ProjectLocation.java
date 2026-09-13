package com.ozerler.marble.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
public class ProjectLocation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ProjectLocation parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProjectLocation> children = new ArrayList<>();

    @Column(name = "location_name", nullable = false, length = 150)
    private String locationName;

    @Column(name = "floor_level", length = 50)
    private String floorLevel;

    @Column(name = "stone_spec", length = 150)
    private String stoneSpec;

    @Column(name = "planned_area_m2", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal plannedAreaM2 = BigDecimal.ZERO;

    @Column(name = "installed_area_m2", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal installedAreaM2 = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PLANNED"; // PLANNED, IN_PROGRESS, COMPLETED


    public BigDecimal getProgressPercentage() {
        if (plannedAreaM2 == null || plannedAreaM2.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return installedAreaM2.divide(plannedAreaM2, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
    }
}
