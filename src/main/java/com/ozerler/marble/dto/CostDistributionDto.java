package com.ozerler.marble.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Strongly typed DTO representing cost distribution metrics grouped by cost center.
 */
@Getter
@Builder
public class CostDistributionDto {

    private final String costCenterName;
    private final BigDecimal totalAmount;

    public static CostDistributionDto fromRow(Object[] row) {
        if (row == null || row.length == 0) {
            return CostDistributionDto.builder()
                    .costCenterName("—")
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }

        String centerName = row[0] != null ? row[0].toString() : "—";
        BigDecimal amount = row.length > 1 && row[1] instanceof BigDecimal bd ? bd
                : (row.length > 1 && row[1] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO);

        return CostDistributionDto.builder()
                .costCenterName(centerName)
                .totalAmount(amount)
                .build();
    }
}
