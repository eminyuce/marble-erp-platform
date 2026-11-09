package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationLogKpiDto {

    private long totalLast24Hours;
    private long errorLast24Hours;
    private long warnLast24Hours;
    private long infoLast24Hours;

    /** All stored rows. The log grid is not limited to the last 24 hours. */
    private long totalCount;
    private long errorCount;
    private long warnCount;
    private long infoCount;
}
