package com.ozerler.marble.domain;

import com.ozerler.marble.model.enums.ExpenseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpensePeriodsTest {

    @Test
    @DisplayName("electricity is costed to the previous month of the invoice date")
    void electricityGoesToPreviousMonth() {
        assertThat(ExpensePeriods.expensePeriod(ExpenseType.ELECTRICITY, LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 12)))
                .isEqualTo("2026-08");
        assertThat(ExpensePeriods.postingPeriod(LocalDate.of(2026, 9, 12))).isEqualTo("2026-09");
    }

    @Test
    @DisplayName("non-electricity expenses use the invoice month")
    void dieselUsesInvoiceMonth() {
        assertThat(ExpensePeriods.expensePeriod(ExpenseType.DIESEL, LocalDate.of(2026, 9, 3), null))
                .isEqualTo("2026-09");
    }

    @Test
    @DisplayName("invalid period strings are rejected")
    void invalidPeriod() {
        assertThatThrownBy(() -> ExpensePeriods.normalize("2026/09"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ExpensePeriods.of(YearMonth.of(2026, 8))).isEqualTo("2026-08");
    }
}
