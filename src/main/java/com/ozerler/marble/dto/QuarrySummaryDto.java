package com.ozerler.marble.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QuarrySummaryDto {
    private BigDecimal producedTonsThisMonth;
    private long productionYardCount;
    private long dispatchYardCount;
    private long factoryYardCount;
    private long soldCount;
    private long readyForDispatchCount;
    private long inTransitCount;
    private long totalInStockCount;
    private long markedCount;
    private BigDecimal fuelStockLiters;
    private long consumablesCount;
    private BigDecimal costPerTonThisMonth;
    private boolean unallocatedCarryForward;
    private String expensePeriod;
    private BigDecimal totalCostInStock;
    private BigDecimal totalMarketValueInStock;
}
