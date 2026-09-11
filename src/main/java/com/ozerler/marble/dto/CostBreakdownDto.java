package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Data Transfer Object representing unit cost breakdown and dynamic pricing recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostBreakdownDto {

    @JsonProperty("item_identifier")
    @JsonAlias("itemIdentifier")
    private String itemIdentifier;

    @JsonProperty("stone_type")
    @JsonAlias("stoneType")
    private String stoneType;

    @JsonProperty("raw_block_cost_per_m2")
    @JsonAlias("rawBlockCostPerM2")
    private BigDecimal rawBlockCostPerM2;

    @JsonProperty("factory_cost_per_m2")
    @JsonAlias("factoryCostPerM2")
    private BigDecimal factoryCostPerM2;

    @JsonProperty("workshop_cost_per_m2")
    @JsonAlias("workshopCostPerM2")
    private BigDecimal workshopCostPerM2;

    @JsonProperty("scrap_burden_cost_per_m2")
    @JsonAlias("scrapBurdenCostPerM2")
    private BigDecimal scrapBurdenCostPerM2;

    @JsonProperty("logistics_cost_per_m2")
    @JsonAlias("logisticsCostPerM2")
    private BigDecimal logisticsCostPerM2;

    @JsonProperty("overhead_cost_per_m2")
    @JsonAlias("overheadCostPerM2")
    private BigDecimal overheadCostPerM2;

    @JsonProperty("total_unit_cost_per_m2")
    @JsonAlias("totalUnitCostPerM2")
    private BigDecimal totalUnitCostPerM2;

    // Smart pricing suggestions
    @JsonProperty("target_margin_pct")
    @JsonAlias("targetMarginPct")
    private BigDecimal targetMarginPct; // e.g. 30%

    @JsonProperty("suggested_base_price")
    @JsonAlias("suggestedBasePrice")
    private BigDecimal suggestedBasePrice;

    @JsonProperty("minimum_floor_price")
    @JsonAlias("minimumFloorPrice")
    private BigDecimal minimumFloorPrice; // e.g. 20% margin limit

    public static CostBreakdownDto calculateStandard(String identifier, String stoneType,
                                                    BigDecimal raw, BigDecimal factory,
                                                    BigDecimal workshop, BigDecimal scrap,
                                                    BigDecimal logistics, BigDecimal overhead,
                                                    BigDecimal marginPct) {
        BigDecimal total = raw.add(factory).add(workshop).add(scrap).add(logistics).add(overhead);

        BigDecimal margin = marginPct != null ? marginPct : new BigDecimal("30.0");
        BigDecimal marginDecimal = margin.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.subtract(marginDecimal);

        BigDecimal suggestedPrice = divisor.compareTo(BigDecimal.ZERO) > 0 ?
                total.divide(divisor, 2, RoundingMode.HALF_UP) : total;

        BigDecimal minDivisor = BigDecimal.ONE.subtract(new BigDecimal("0.20"));
        BigDecimal floorPrice = minDivisor.compareTo(BigDecimal.ZERO) > 0 ?
                total.divide(minDivisor, 2, RoundingMode.HALF_UP) : total;

        return CostBreakdownDto.builder()
                .itemIdentifier(identifier)
                .stoneType(stoneType)
                .rawBlockCostPerM2(raw.setScale(2, RoundingMode.HALF_UP))
                .factoryCostPerM2(factory.setScale(2, RoundingMode.HALF_UP))
                .workshopCostPerM2(workshop.setScale(2, RoundingMode.HALF_UP))
                .scrapBurdenCostPerM2(scrap.setScale(2, RoundingMode.HALF_UP))
                .logisticsCostPerM2(logistics.setScale(2, RoundingMode.HALF_UP))
                .overheadCostPerM2(overhead.setScale(2, RoundingMode.HALF_UP))
                .totalUnitCostPerM2(total.setScale(2, RoundingMode.HALF_UP))
                .targetMarginPct(margin)
                .suggestedBasePrice(suggestedPrice)
                .minimumFloorPrice(floorPrice)
                .build();
    }
}
