package com.ozerler.marble.service;

import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostPeriodCloseRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private CostTransactionRepository costTransactionRepository;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private CostPeriodCloseRepository costPeriodCloseRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(costTransactionRepository, costCenterRepository, costPeriodCloseRepository);
    }

    @Test
    @DisplayName("electricity without an explicit period is costed to the previous invoice month")
    void electricityDefaultsToPreviousMonth() {
        CostCenter center = CostCenter.builder().id(1L).code("CC-Q").name("Ocak").businessUnit(BusinessUnit.QUARRY).build();
        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(center));
        when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-08")).thenReturn(false);
        when(costTransactionRepository.save(any(CostTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                1L, ExpenseType.ELECTRICITY, null, BusinessUnit.QUARRY, new BigDecimal("1000"), "TRY",
                "F-1", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10), null, "2026-09",
                null, null, null, null, null, null, null, null, "Elektrik"));

        ArgumentCaptor<CostTransaction> captor = ArgumentCaptor.forClass(CostTransaction.class);
        verify(costTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getExpensePeriod()).isEqualTo("2026-08");
        assertThat(captor.getValue().getPostingPeriod()).isEqualTo("2026-09");
    }

    @Test
    @DisplayName("explicit electricity period is stored as the costing month")
    void electricityUsesCallerPeriod() {
        CostCenter center = CostCenter.builder().id(1L).code("CC-Q").name("Ocak").businessUnit(BusinessUnit.QUARRY).build();
        when(costCenterRepository.findById(1L)).thenReturn(Optional.of(center));
        when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-08")).thenReturn(false);
        when(costTransactionRepository.save(any(CostTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                1L, ExpenseType.ELECTRICITY, null, BusinessUnit.QUARRY, new BigDecimal("1000"), "TRY",
                "F-1", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10), "2026-08", "2026-09",
                null, null, null, null, null, null, null, null, "Elektrik"));

        ArgumentCaptor<CostTransaction> captor = ArgumentCaptor.forClass(CostTransaction.class);
        verify(costTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getExpensePeriod()).isEqualTo("2026-08");
        assertThat(captor.getValue().getPostingPeriod()).isEqualTo("2026-09");
    }

    @Test
    @DisplayName("site expenses require a construction project")
    void siteExpenseWithoutProject_Throws() {
        CostCenter center = CostCenter.builder().id(5L).code("CC-S").name("Şantiye").businessUnit(BusinessUnit.SITE).build();
        when(costCenterRepository.findById(5L)).thenReturn(Optional.of(center));

        assertThatThrownBy(() -> expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                5L, ExpenseType.MATERIAL, null, BusinessUnit.SITE, new BigDecimal("500"), "TRY",
                "F-2", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10), "2026-09", "2026-09",
                null, null, null, null, null, null, null, null, "Malzeme")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(costTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getExpenseSummary calculates unit totals and overall sum")
    void getExpenseSummary_ReturnsCorrectTotals() {
        when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.QUARRY, "2026-09")).thenReturn(new BigDecimal("10000"));
        when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.FACTORY, "2026-09")).thenReturn(new BigDecimal("20000"));
        when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.WORKSHOP, "2026-09")).thenReturn(new BigDecimal("5000"));
        when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.SITE, "2026-09")).thenReturn(new BigDecimal("15000"));
        when(costTransactionRepository.countByExpensePeriod("2026-09")).thenReturn(12L);

        com.ozerler.marble.dto.ExpenseSummaryDto summary = expenseService.getExpenseSummary("2026-09");

        assertThat(summary).isNotNull();
        assertThat(summary.getPeriod()).isEqualTo("2026-09");
        assertThat(summary.getQuarryTotal()).isEqualByComparingTo("10000");
        assertThat(summary.getFactoryTotal()).isEqualByComparingTo("20000");
        assertThat(summary.getWorkshopTotal()).isEqualByComparingTo("5000");
        assertThat(summary.getSiteTotal()).isEqualByComparingTo("15000");
        assertThat(summary.getTotalExpensesThisMonth()).isEqualByComparingTo("50000");
        assertThat(summary.getTotalCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("deleteExpense deletes open expense or throws when period is closed")
    void deleteExpense_DeletesOrThrows() {
        CostTransaction tx = CostTransaction.builder()
                .id(99L)
                .businessUnit(BusinessUnit.QUARRY)
                .expensePeriod("2026-09")
                .build();
        when(costTransactionRepository.findById(99L)).thenReturn(Optional.of(tx));
        when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-09")).thenReturn(false);

        expenseService.deleteExpense(99L);
        verify(costTransactionRepository).delete(tx);

        // When period is closed
        when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-09")).thenReturn(true);
        assertThatThrownBy(() -> expenseService.deleteExpense(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
