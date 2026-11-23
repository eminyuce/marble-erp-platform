package com.ozerler.marble.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteProfitDto {
    private Long projectId;
    private String projectCode;
    private String projectName;
    private BigDecimal realizedRevenue;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal taxCost;
    private BigDecimal consumableCost;
    private BigDecimal transportationCost;
    private BigDecimal otherCost;
    private BigDecimal totalCost;
    private BigDecimal netProfitOrLoss;

    public BigDecimal getMarginPct() {
        if (realizedRevenue == null || realizedRevenue.compareTo(BigDecimal.ZERO) <= 0 || netProfitOrLoss == null) {
            return null;
        }
        return netProfitOrLoss.multiply(BigDecimal.valueOf(100)).divide(realizedRevenue, 1, java.math.RoundingMode.HALF_UP);
    }
}
