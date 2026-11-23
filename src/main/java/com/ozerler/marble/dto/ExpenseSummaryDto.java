package com.ozerler.marble.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
