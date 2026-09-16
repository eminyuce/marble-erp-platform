package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.ExpenseSummaryDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.service.BlockCostCalculationService;
import com.ozerler.marble.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePurchaseControllerTest {

    @Mock
    private ExpenseService expenseService;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private QuarryRepository quarryRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private BlockCostCalculationService blockCostCalculationService;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ExpensePurchaseController controller;

    @Test
    @DisplayName("index populates summary, period and options")
    void index_PopulatesModel() {
        when(expenseService.getExpenseSummary("2026-09")).thenReturn(ExpenseSummaryDto.builder()
                .period("2026-09")
                .totalExpensesThisMonth(new BigDecimal("1000"))
                .build());
        when(costCenterRepository.findAll()).thenReturn(Collections.emptyList());
        when(quarryRepository.findAll()).thenReturn(Collections.emptyList());
        when(projectRepository.findAll()).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String view = controller.index(BusinessUnit.QUARRY, null, "2026-09", model);

        assertThat(view).isEqualTo("erp/expenses/index");
        assertThat(model.containsAttribute("summary")).isTrue();
        assertThat(model.getAttribute("period")).isEqualTo("2026-09");
        assertThat(model.containsAttribute("businessUnits")).isTrue();
    }

    @Test
    @DisplayName("getExpensesData delegates to expenseService")
    void getExpensesData_DelegatesToService() {
        TabulatorResponse<ExpenseDto> expected = TabulatorResponse.of(List.of(), 1, 0L);
        when(expenseService.getExpensesPaged(1, 25, "test", null, null,
                BusinessUnit.QUARRY, "2026-09", null, null, null, null))
                .thenReturn(expected);

        TabulatorResponse<ExpenseDto> actual = controller.getExpensesData(
                1, 25, "test", null, null, BusinessUnit.QUARRY, "2026-09", null, null, null, null);

        assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("getUnitOptions returns types and centers for unit")
    void getUnitOptions_ReturnsMap() {
        when(costCenterRepository.findByBusinessUnit(BusinessUnit.QUARRY)).thenReturn(Collections.emptyList());

        Map<String, Object> options = controller.getUnitOptions(BusinessUnit.QUARRY);

        assertThat(options).containsKey("types");
        assertThat(options).containsKey("centers");
    }

    @Test
    @DisplayName("deleteExpenseApi deletes expense and returns ok response")
    void deleteExpenseApi_Success() {
        when(messageSource.getMessage(eq("erp.expense.delete.success"), any(), any(Locale.class)))
                .thenReturn("Gider silindi");

        BackEndResponse response = controller.deleteExpenseApi(42L, Locale.getDefault());

        verify(expenseService).deleteExpense(42L);
        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
    }
}
