package com.ozerler.marble.dto;

import com.ozerler.marble.model.enums.ScrapReasonCode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

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
            }
        }

        long count = row.length > 1 && row[1] instanceof Number n ? n.longValue() : 0L;
        BigDecimal weight = row.length > 2 && row[2] instanceof BigDecimal bd ? bd
                : (row.length > 2 && row[2] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO);
        BigDecimal cost = row.length > 3 && row[3] instanceof BigDecimal bd ? bd
                : (row.length > 3 && row[3] instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO);
        String label = code != null ? code.getTitle() : (row[0] != null ? row[0].toString() : "—");

        return ScrapSummaryDto.builder()
                .reasonCode(code)
                .reasonLabel(label)
                .count(count)
                .totalWeightKg(weight)
                .totalCostImpact(cost)
                .build();
    }
}
