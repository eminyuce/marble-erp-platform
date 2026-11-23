package com.ozerler.marble.domain;

import java.time.LocalDate;
import java.time.YearMonth;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.enums.ExpenseType;

class ExpensePeriodsTest {

    @Test
    @DisplayName("electricity is costed to the previous month of the invoice date")
    void electricityGoesToPreviousMonth() {
        Assertions.assertThat(ExpensePeriods.expensePeriod(ExpenseType.ELECTRICITY, LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 12)))
                .isEqualTo("2026-08");
        Assertions.assertThat(ExpensePeriods.postingPeriod(LocalDate.of(2026, 9, 12))).isEqualTo("2026-09");
    }

    @Test
    @DisplayName("non-electricity expenses use the invoice month")
    void dieselUsesInvoiceMonth() {
        Assertions.assertThat(ExpensePeriods.expensePeriod(ExpenseType.DIESEL, LocalDate.of(2026, 9, 3), null))
                .isEqualTo("2026-09");
    }

    @Test
    @DisplayName("invalid period strings are rejected")
    void invalidPeriod() {
        Assertions.assertThatThrownBy(() -> ExpensePeriods.normalize("2026/09"))
                .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThat(ExpensePeriods.of(YearMonth.of(2026, 8))).isEqualTo("2026-08");
    }
}
