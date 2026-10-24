package com.ozerler.marble.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CariSummaryDto {

    @Builder.Default
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal netBalance = BigDecimal.ZERO;

    @Builder.Default
    private String balanceStatus = "BAKİYE SIFIR"; // BORÇLU (B), ALACAKLI (A), SIFIR

    @Builder.Default
    private int transactionCount = 0;
}
