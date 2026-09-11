package com.ozerler.marble.model;

import com.ozerler.marble.model.enums.ScrapReasonCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scrap_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class ScrapLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scrap_code", nullable = false, unique = true, length = 50)
    private String scrapCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cut_order_id")
    private CutOrder cutOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id")
    private Slab slab;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 20)
    private ScrapReasonCode reasonCode;

    @Column(name = "scrap_weight_kg", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal scrapWeightKg = BigDecimal.ZERO;

    @Column(name = "scrap_area_m2", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal scrapAreaM2 = BigDecimal.ZERO;

    @Column(name = "cost_impact", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal costImpact = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logged_by", length = 100)
    private String loggedBy;

    @CreationTimestamp
    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;
}
