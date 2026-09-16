package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseSummaryDto {

    private String period;
    private BigDecimal totalExpensesThisMonth;
    private BigDecimal quarryTotal;
    private BigDecimal factoryTotal;
    private BigDecimal workshopTotal;
    private BigDecimal siteTotal;
    private long totalCount;
}
