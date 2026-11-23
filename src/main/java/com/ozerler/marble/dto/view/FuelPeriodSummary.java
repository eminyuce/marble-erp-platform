package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

/**
 * Totals of the fuel entries of one period, calculated by the service.
 *
 * @param totalLitres  sum of litres
 * @param totalAmount  sum of amounts
 * @param entryCount   number of entries
 * @param machineCount number of distinct machines
 */
public record FuelPeriodSummary(BigDecimal totalLitres, BigDecimal totalAmount, int entryCount, long machineCount) {
}
