package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.dto.ExpenseSummaryDto;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostPeriodCloseRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.QuarryRepository;

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
        expenseService = new ExpenseService(costTransactionRepository, costCenterRepository, costPeriodCloseRepository,
                Mockito.mock(ProjectRepository.class),
                Mockito.mock(QuarryRepository.class));
    }

    @Test
    @DisplayName("electricity without an explicit period is costed to the previous invoice month")
    void electricityDefaultsToPreviousMonth() {
        CostCenter center = CostCenter.builder().id(1L).code("CC-Q").name("Ocak").businessUnit(BusinessUnit.QUARRY).build();
        Mockito.when(costCenterRepository.findById(1L)).thenReturn(Optional.of(center));
        Mockito.when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-08")).thenReturn(false);
        Mockito.when(costTransactionRepository.save(ArgumentMatchers.any(CostTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                1L, ExpenseType.ELECTRICITY, null, BusinessUnit.QUARRY, new BigDecimal("1000"), "TRY",
                "F-1", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10), null, "2026-09",
                null, null, null, null, null, null, null, null, "Elektrik"));

        ArgumentCaptor<CostTransaction> captor = ArgumentCaptor.forClass(CostTransaction.class);
        Mockito.verify(costTransactionRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getExpensePeriod()).isEqualTo("2026-08");
        Assertions.assertThat(captor.getValue().getPostingPeriod()).isEqualTo("2026-09");
    }

    @Test
    @DisplayName("explicit electricity period is stored as the costing month")
    void electricityUsesCallerPeriod() {
        CostCenter center = CostCenter.builder().id(1L).code("CC-Q").name("Ocak").businessUnit(BusinessUnit.QUARRY).build();
        Mockito.when(costCenterRepository.findById(1L)).thenReturn(Optional.of(center));
        Mockito.when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-08")).thenReturn(false);
        Mockito.when(costTransactionRepository.save(ArgumentMatchers.any(CostTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                1L, ExpenseType.ELECTRICITY, null, BusinessUnit.QUARRY, new BigDecimal("1000"), "TRY",
                "F-1", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10), "2026-08", "2026-09",
                null, null, null, null, null, null, null, null, "Elektrik"));

        ArgumentCaptor<CostTransaction> captor = ArgumentCaptor.forClass(CostTransaction.class);
        Mockito.verify(costTransactionRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getExpensePeriod()).isEqualTo("2026-08");
        Assertions.assertThat(captor.getValue().getPostingPeriod()).isEqualTo("2026-09");
    }

    @Test
    @DisplayName("site expenses do not require a construction project")
    void siteExpenseWithoutProjectSucceeds() {
        CostCenter center = CostCenter.builder().id(5L).code("CC-S").name("Şantiye").businessUnit(BusinessUnit.SITE).build();
        Mockito.when(costCenterRepository.findById(5L)).thenReturn(Optional.of(center));
        Mockito.when(costTransactionRepository.save(ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        CostTransaction tx = expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                5L, ExpenseType.CONSUMABLES, null, BusinessUnit.SITE, new BigDecimal("500"), "TRY",
                "F-2", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10), "2026-09", "2026-09",
                null, null, null, null, null, null, null, null, "Malzeme"));

        Assertions.assertThat(tx).isNotNull();
        Assertions.assertThat(tx.getBusinessUnit()).isEqualTo(BusinessUnit.SITE);
        Assertions.assertThat(tx.getProject()).isNull();
        Mockito.verify(costTransactionRepository).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("getExpenseSummary calculates unit totals and overall sum")
    void getExpenseSummaryReturnsCorrectTotals() {
        Mockito.when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.QUARRY, "2026-09")).thenReturn(new BigDecimal("10000"));
        Mockito.when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.FACTORY, "2026-09")).thenReturn(new BigDecimal("20000"));
        Mockito.when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.WORKSHOP, "2026-09")).thenReturn(new BigDecimal("5000"));
        Mockito.when(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.SITE, "2026-09")).thenReturn(new BigDecimal("15000"));
        Mockito.when(costTransactionRepository.countByExpensePeriod("2026-09")).thenReturn(12L);

        ExpenseSummaryDto summary = expenseService.getExpenseSummary("2026-09");

        Assertions.assertThat(summary).isNotNull();
        Assertions.assertThat(summary.getPeriod()).isEqualTo("2026-09");
        Assertions.assertThat(summary.getQuarryTotal()).isEqualByComparingTo("10000");
        Assertions.assertThat(summary.getFactoryTotal()).isEqualByComparingTo("20000");
        Assertions.assertThat(summary.getWorkshopTotal()).isEqualByComparingTo("5000");
        Assertions.assertThat(summary.getSiteTotal()).isEqualByComparingTo("15000");
        Assertions.assertThat(summary.getTotalExpensesThisMonth()).isEqualByComparingTo("50000");
        Assertions.assertThat(summary.getTotalCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("deleteExpense deletes open expense or throws when period is closed")
    void deleteExpenseDeletesOrThrows() {
        CostTransaction tx = CostTransaction.builder()
                .id(99L)
                .businessUnit(BusinessUnit.QUARRY)
                .expensePeriod("2026-09")
                .build();
        Mockito.when(costTransactionRepository.findById(99L)).thenReturn(Optional.of(tx));
        Mockito.when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-09")).thenReturn(false);

        expenseService.deleteExpense(99L);
        Mockito.verify(costTransactionRepository).delete(tx);

        // When period is closed
        Mockito.when(costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(BusinessUnit.QUARRY, "2026-09")).thenReturn(true);
        Assertions.assertThatThrownBy(() -> expenseService.deleteExpense(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
