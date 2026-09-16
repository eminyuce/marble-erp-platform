package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuarryCostBreakdownDto {

    private Long quarryId;
    private String quarryName;
    private String expensePeriod;
    private BigDecimal totalExpenses;
    private BigDecimal totalTonnage;
    private BigDecimal totalMarketValue;
    private BigDecimal expensePerTon;
    private BigDecimal avgMarketValuePerTon;
    private BigDecimal marginPerTon;
    @Builder.Default
    private Map<String, BigDecimal> expenseByType = new LinkedHashMap<>();
}
