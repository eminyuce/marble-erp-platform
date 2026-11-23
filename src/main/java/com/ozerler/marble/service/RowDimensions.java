package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Reads one positive-width row from parallel dimension lists.
 */
final class RowDimensions {

    private RowDimensions() {
    }

    record Row(BigDecimal width, BigDecimal length, BigDecimal thickness) {
    }

    static Row positiveWidth(List<BigDecimal> widths, List<BigDecimal> lengths, List<BigDecimal> thicknesses,
                             int index, BigDecimal thicknessFallback) {
        BigDecimal width = widths.get(index);
        if (width == null || width.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return new Row(width, listedOr(lengths, index, width), listedOr(thicknesses, index, thicknessFallback));
    }

    @FunctionalInterface
    interface SizedPiece {
        void accept(int index, BigDecimal width, BigDecimal length, BigDecimal thickness, int pieceCount, BigDecimal quantity);
    }

    static void eachPositiveSizedRow(List<BigDecimal> widths, List<BigDecimal> lengths, List<BigDecimal> thicknesses,
                                     List<Integer> pieceCounts, List<BigDecimal> quantities,
                                     BigDecimal thicknessFallback, SizedPiece piece) {
        if (widths == null || widths.isEmpty()) {
            return;
        }
        for (int i = 0; i < widths.size(); i++) {
            Row row = positiveWidth(widths, lengths, thicknesses, i, thicknessFallback);
            if (row == null) {
                continue;
            }
            int count = (pieceCounts != null && i < pieceCounts.size() && pieceCounts.get(i) != null)
                    ? pieceCounts.get(i) : 1;
            BigDecimal quantity = (quantities != null && i < quantities.size() && quantities.get(i) != null)
                    ? quantities.get(i)
                    : row.width().multiply(row.length()).multiply(BigDecimal.valueOf(count))
                        .divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
            piece.accept(i, row.width(), row.length(), row.thickness(), count, quantity);
        }
    }

    private static BigDecimal listedOr(List<BigDecimal> values, int index, BigDecimal fallback) {
        return (values != null && index < values.size() && values.get(index) != null)
                ? values.get(index)
                : fallback;
    }
}
