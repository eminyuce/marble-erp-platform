package com.ozerler.marble.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
