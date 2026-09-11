package com.ozerler.marble.service;

import com.ozerler.marble.dto.CostBreakdownDto;
import com.ozerler.marble.model.enums.QualityGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class CostAccountingServiceTest {

    private final CostAccountingService costService = new CostAccountingService(null, null);

    @Test
    @DisplayName("Verify Grade Multiplier Algorithm matches BRD Section 6.1 exact numeric results")
    void testGradeMultiplierAlgorithm() {
        // Block + Cutting Cost: 150,000 TL
        BigDecimal totalCost = new BigDecimal("150000.00");

        // Slabs: 40 m2 Grade A, 80 m2 Grade B, 30 m2 Grade C
        BigDecimal areaA = new BigDecimal("40.0");
        BigDecimal areaB = new BigDecimal("80.0");
        BigDecimal areaC = new BigDecimal("30.0");

        // Multipliers from enum: A=1.15, B=1.00, C=0.65
        BigDecimal eqA = areaA.multiply(QualityGrade.A.getMultiplier()); // 46.0
        BigDecimal eqB = areaB.multiply(QualityGrade.B.getMultiplier()); // 80.0
        BigDecimal eqC = areaC.multiply(QualityGrade.C.getMultiplier()); // 19.5
        BigDecimal totalEquivalentArea = eqA.add(eqB).add(eqC); // 145.5

        assertThat(totalEquivalentArea).isEqualByComparingTo(new BigDecimal("145.50"));

        // C_base = 150000 / 145.5 = 1030.93 TL/m2
        BigDecimal cBase = totalCost.divide(totalEquivalentArea, 2, RoundingMode.HALF_UP);
        assertThat(cBase).isEqualByComparingTo(new BigDecimal("1030.93"));

        // Grade A = 1030.93 * 1.15 = 1185.57 TL/m2
        BigDecimal costA = cBase.multiply(QualityGrade.A.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
        assertThat(costA).isEqualByComparingTo(new BigDecimal("1185.57"));

        // Grade B = 1030.93 * 1.00 = 1030.93 TL/m2
        BigDecimal costB = cBase.multiply(QualityGrade.B.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
        assertThat(costB).isEqualByComparingTo(new BigDecimal("1030.93"));

        // Grade C = 1030.93 * 0.65 = 670.10 TL/m2
        BigDecimal costC = cBase.multiply(QualityGrade.C.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
        assertThat(costC).isEqualByComparingTo(new BigDecimal("670.10"));
    }

    @Test
    @DisplayName("Verify Multi-Layer Unit Cost Breakdown calculation from BRD Section 6.2")
    void testMultiLayerCostCalculation() {
        CostBreakdownDto breakdown = costService.calculateMultiLayerCost(
                "FG-80120-042", "Crema Marfil",
                new BigDecimal("820.00"),
                new BigDecimal("210.00"),
                new BigDecimal("165.00"),
                new BigDecimal("95.00"),
                new BigDecimal("45.00"),
                new BigDecimal("30.00"),
                new BigDecimal("30.00")
        );

        // Sum = 820 + 210 + 165 + 95 + 45 + 30 = 1,365 TL/m2
        assertThat(breakdown.getTotalUnitCostPerM2()).isEqualByComparingTo(new BigDecimal("1365.00"));

        // Suggested Price with 30% margin = 1365 / (1 - 0.30) = 1365 / 0.70 = 1,950.00 TL/m2
        assertThat(breakdown.getSuggestedBasePrice()).isEqualByComparingTo(new BigDecimal("1950.00"));
    }
}
