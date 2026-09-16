package com.ozerler.marble.domain;

import com.ozerler.marble.common.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Ocak blok çıkarma maliyeti hesapları.
 * <p>
 * Ton başına maliyet = toplam gider / toplam çıkarılan tonaj.
 * Blok maliyeti = blok tonajı × ton başına maliyet.
 * Piyasa değeri: satış fiyatı veya birim ton değeri × tonaj.
 */
public final class BlockCostFormula {

    private BlockCostFormula() {
    }

    public static BigDecimal expensePerTon(BigDecimal totalExpenses, BigDecimal totalTonnage) {
        BigDecimal expenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
        BigDecimal tons = totalTonnage != null ? totalTonnage : BigDecimal.ZERO;
        if (tons.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return expenses.divide(tons, Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal blockExtractionCost(BigDecimal blockTonnage, BigDecimal expensePerTon) {
        if (blockTonnage == null || expensePerTon == null
                || blockTonnage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return blockTonnage.multiply(expensePerTon).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal blockMarketValue(BigDecimal blockTonnage, BigDecimal salePrice,
                                              BigDecimal unitMarketValuePerTon) {
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice;
        }
        if (blockTonnage != null && unitMarketValuePerTon != null
                && blockTonnage.compareTo(BigDecimal.ZERO) > 0
                && unitMarketValuePerTon.compareTo(BigDecimal.ZERO) > 0) {
            return blockTonnage.multiply(unitMarketValuePerTon).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
        }
        return null;
    }

    public static BigDecimal averageMarketValuePerTon(BigDecimal totalMarketValue, BigDecimal totalTonnage) {
        if (totalMarketValue == null || totalTonnage == null
                || totalTonnage.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return totalMarketValue.divide(totalTonnage, Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Kullanıcı formülü: (toplam tonaj × piyasa değeri − toplam gider) / toplam tonaj.
     * Piyasa değeri burada ton başına ortalama piyasa değeri olarak kullanılır.
     */
    public static BigDecimal marginPerTon(BigDecimal avgMarketValuePerTon, BigDecimal totalExpenses,
                                          BigDecimal totalTonnage) {
        Objects.requireNonNull(totalTonnage);
        if (totalTonnage.compareTo(BigDecimal.ZERO) <= 0 || avgMarketValuePerTon == null) {
            return null;
        }
        BigDecimal expenses = totalExpenses != null ? totalExpenses : BigDecimal.ZERO;
        return avgMarketValuePerTon.subtract(
                expenses.divide(totalTonnage, Constants.COST_SCALE, RoundingMode.HALF_UP));
    }

    public static BigDecimal footprintAreaM2(Integer widthCm, Integer lengthCm) {
        if (widthCm == null || lengthCm == null || widthCm <= 0 || lengthCm <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((long) widthCm * lengthCm)
                .divide(new BigDecimal("10000"), Constants.AREA_SCALE, RoundingMode.HALF_UP);
    }
}
