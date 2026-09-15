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
}
