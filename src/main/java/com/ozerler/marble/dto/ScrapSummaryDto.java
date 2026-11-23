package com.ozerler.marble.dto;

import java.math.BigDecimal;

import com.ozerler.marble.model.enums.ScrapReasonCode;

import lombok.Builder;
import lombok.Getter;

/**
 * Strongly typed DTO representing scrap aggregation metrics grouped by reason code.
 */
@Getter
@Builder
public class ScrapSummaryDto {

    private final ScrapReasonCode reasonCode;
    private final String reasonLabel;
    private final long count;
    private final BigDecimal totalWeightKg;
    private final BigDecimal totalCostImpact;

    public static ScrapSummaryDto fromRow(Object[] row) {
        if (row == null || row.length == 0) {
            return ScrapSummaryDto.builder()
                    .reasonCode(null)
                    .reasonLabel("—")
                    .count(0L)
                    .totalWeightKg(BigDecimal.ZERO)
                    .totalCostImpact(BigDecimal.ZERO)
                    .build();
        }

        ScrapReasonCode code = null;
        if (row[0] instanceof ScrapReasonCode r) {
            code = r;
        } else if (row[0] instanceof String s) {
            try {
                code = ScrapReasonCode.valueOf(s);
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }

        long count;
        if (row.length > 1 && row[1] instanceof Number n) {
            count = n.longValue();
        } else {
            count = 0L;
        }
        BigDecimal weight;
        if (row.length > 2 && row[2] instanceof BigDecimal bd) {
            weight = bd;
        } else {
            if (row.length > 2 && row[2] instanceof Number n) {
                weight = BigDecimal.valueOf(n.doubleValue());
            } else {
                weight = BigDecimal.ZERO;
            }
        }
        BigDecimal cost;
        if (row.length > 3 && row[3] instanceof BigDecimal bd) {
            cost = bd;
        } else {
            if (row.length > 3 && row[3] instanceof Number n) {
                cost = BigDecimal.valueOf(n.doubleValue());
            } else {
                cost = BigDecimal.ZERO;
            }
        }
        String label;
        if (code != null) {
            label = code.getTitle();
        } else {
            if (row[0] != null) {
                label = row[0].toString();
            } else {
                label = "—";
            }
        }

        return ScrapSummaryDto.builder()
                .reasonCode(code)
                .reasonLabel(label)
                .count(count)
                .totalWeightKg(weight)
                .totalCostImpact(cost)
                .build();
    }
}
