package com.ozerler.marble.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class QuarrySummaryDto {
    BigDecimal producedTonsThisMonth;
    long productionYardCount;
    long dispatchYardCount;
    long factoryYardCount;
    long soldCount;
    long readyForDispatchCount;
    long inTransitCount;
    long totalInStockCount;
    BigDecimal fuelStockLiters;
    long consumablesCount;
    BigDecimal costPerTonThisMonth;
    boolean unallocatedCarryForward;
    String expensePeriod;
}
