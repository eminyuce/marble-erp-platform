package com.ozerler.marble.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class SiteProfitDto {
    Long projectId;
    String projectCode;
    String projectName;
    BigDecimal realizedRevenue;
    BigDecimal materialCost;
    BigDecimal laborCost;
    BigDecimal taxCost;
    BigDecimal consumableCost;
    BigDecimal transportationCost;
    BigDecimal otherCost;
    BigDecimal totalCost;
    BigDecimal netProfitOrLoss;

    public BigDecimal getMarginPct() {
        if (realizedRevenue == null || realizedRevenue.compareTo(BigDecimal.ZERO) <= 0 || netProfitOrLoss == null) {
            return null;
        }
        return netProfitOrLoss.multiply(BigDecimal.valueOf(100)).divide(realizedRevenue, 1, java.math.RoundingMode.HALF_UP);
    }
}
