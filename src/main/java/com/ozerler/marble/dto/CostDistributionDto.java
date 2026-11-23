package com.ozerler.marble.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

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

        String centerName;
        if (row[0] != null) {
            centerName = row[0].toString();
        } else {
            centerName = "—";
        }
        BigDecimal amount;
        if (row.length > 1 && row[1] instanceof BigDecimal bd) {
            amount = bd;
        } else {
            if (row.length > 1 && row[1] instanceof Number n) {
                amount = BigDecimal.valueOf(n.doubleValue());
            } else {
                amount = BigDecimal.ZERO;
            }
        }

        return CostDistributionDto.builder()
                .costCenterName(centerName)
                .totalAmount(amount)
                .build();
    }
}
