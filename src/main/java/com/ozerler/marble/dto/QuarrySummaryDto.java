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
    long soldCount;
    BigDecimal costPerTonThisMonth;
    boolean unallocatedCarryForward;
    String expensePeriod;
}
