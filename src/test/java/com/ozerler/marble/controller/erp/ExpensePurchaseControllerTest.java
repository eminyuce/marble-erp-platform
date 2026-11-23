package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.ExpenseSummaryDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.BlockCostCalculationService;
import com.ozerler.marble.service.ExpenseService;
import com.ozerler.marble.service.FormLookupService;

@ExtendWith(MockitoExtension.class)
class ExpensePurchaseControllerTest {

    @Mock
    private FormLookupService formLookup;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private BlockCostCalculationService blockCostCalculationService;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ExpensePurchaseController controller;

    @Test
    @DisplayName("index populates summary, period and options")
    void indexPopulatesModel() {
        Mockito.when(expenseService.getExpenseSummary("2026-09")).thenReturn(ExpenseSummaryDto.builder()
                .period("2026-09")
                .totalExpensesThisMonth(new BigDecimal("1000"))
                .build());
        Mockito.when(formLookup.costCenters()).thenReturn(Collections.emptyList());
        Mockito.when(formLookup.quarries()).thenReturn(Collections.emptyList());
        Mockito.when(formLookup.projects()).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String view = controller.index(BusinessUnit.QUARRY, null, "2026-09", model);

        Assertions.assertThat(view).isEqualTo("erp/expenses/index");
        Assertions.assertThat(model.containsAttribute("summary")).isTrue();
        Assertions.assertThat(model.getAttribute("period")).isEqualTo("2026-09");
        Assertions.assertThat(model.containsAttribute("businessUnits")).isTrue();
    }

    @Test
    @DisplayName("getExpensesData delegates to expenseService")
    void getExpensesDataDelegatesToService() {
        TabulatorResponse<ExpenseDto> expected = TabulatorResponse.of(List.of(), 1, 0L);
        Mockito.when(expenseService.getExpensesPaged(1, 25, "test", null, null,
                BusinessUnit.QUARRY, "2026-09", null, null, null, null))
                .thenReturn(expected);

        TabulatorResponse<ExpenseDto> actual = controller.getExpensesData(
                1, 25, "test", null, null, BusinessUnit.QUARRY, "2026-09", null, null, null, null);

        Assertions.assertThat(actual).isSameAs(expected);
    }

    @Test
    @DisplayName("getUnitOptions returns types and centers for unit")
    void getUnitOptionsReturnsMap() {
        Mockito.when(formLookup.costCenters(BusinessUnit.QUARRY)).thenReturn(Collections.emptyList());

        Map<String, Object> options = controller.getUnitOptions(BusinessUnit.QUARRY);

        Assertions.assertThat(options).containsKey("types");
        Assertions.assertThat(options).containsKey("centers");
    }

    @Test
    @DisplayName("deleteExpenseApi deletes expense and returns ok response")
    void deleteExpenseApiSuccess() {
        Mockito.when(messageSource.getMessage(ArgumentMatchers.eq("erp.expense.delete.success"), ArgumentMatchers.any(), ArgumentMatchers.any(Locale.class)))
                .thenReturn("Gider silindi");

        BackEndResponse response = controller.deleteExpenseApi(42L, Locale.getDefault());

        Mockito.verify(expenseService).deleteExpense(42L);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
    }

    @Test
    @DisplayName("createForm populates lookups and returns form view")
    void createFormReturnsFormView() {
        Mockito.when(formLookup.costCenters()).thenReturn(Collections.emptyList());
        Mockito.when(formLookup.quarries()).thenReturn(Collections.emptyList());
        Mockito.when(formLookup.projects()).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String view = controller.createForm(model);

        Assertions.assertThat(view).isEqualTo("erp/expenses/form");
        Assertions.assertThat(model.getAttribute("record")).isNull();
        Assertions.assertThat(model.containsAttribute("businessUnits")).isTrue();
        Assertions.assertThat(model.containsAttribute("expenseTypes")).isTrue();
    }

    @Test
    @DisplayName("editForm fetches expense dto and returns form view")
    void editFormReturnsFormView() {
        ExpenseDto dto = ExpenseDto.builder().id(99L).build();
        Mockito.when(expenseService.getExpenseDto(99L)).thenReturn(dto);
        Mockito.when(formLookup.costCenters()).thenReturn(Collections.emptyList());
        Mockito.when(formLookup.quarries()).thenReturn(Collections.emptyList());
        Mockito.when(formLookup.projects()).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String view = controller.editForm(99L, model);

        Assertions.assertThat(view).isEqualTo("erp/expenses/form");
        Assertions.assertThat(model.getAttribute("record")).isSameAs(dto);
        Assertions.assertThat(model.containsAttribute("businessUnits")).isTrue();
    }

    @Test
    @DisplayName("detail fetches expense dto and returns detail view")
    void detailReturnsDetailView() {
        ExpenseDto dto = ExpenseDto.builder().id(55L).build();
        Mockito.when(expenseService.getExpenseDto(55L)).thenReturn(dto);

        Model model = new ConcurrentModel();
        String view = controller.detail(55L, model);

        Assertions.assertThat(view).isEqualTo("erp/expenses/detail");
        Assertions.assertThat(model.getAttribute("expense")).isSameAs(dto);
    }

    @Test
    @DisplayName("recordExpense saves expense and redirects")
    void recordExpenseSuccess() {
        Mockito.when(messageSource.getMessage(ArgumentMatchers.eq("erp.expense.record.success"), ArgumentMatchers.any(), ArgumentMatchers.any(Locale.class)))
                .thenReturn("Gider kaydedildi");

        RedirectAttributesModelMap ra =
                new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.recordExpense(
                BusinessUnit.FACTORY, 1L, ExpenseType.CONSUMABLES, new BigDecimal("250.00"),
                LocalDate.of(2026, 9, 16), null, null, "DOC1",
                LocalDate.of(2026, 9, 16), "2026-09", "desc",
                Locale.getDefault(), ra, model);

        Assertions.assertThat(view).isEqualTo("redirect:/expenses");
        Mockito.verify(expenseService).recordForScreen(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        Assertions.assertThat(ra.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    @DisplayName("updateExpense updates expense and redirects")
    void updateExpenseSuccess() {
        Mockito.when(messageSource.getMessage(ArgumentMatchers.eq("erp.expense.update.success"), ArgumentMatchers.any(), ArgumentMatchers.any(Locale.class)))
                .thenReturn("Gider güncellendi");

        RedirectAttributesModelMap ra =
                new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.updateExpense(
                10L, BusinessUnit.FACTORY, 1L, ExpenseType.CONSUMABLES, new BigDecimal("350.00"),
                LocalDate.of(2026, 9, 16), null, null, "DOC1",
                LocalDate.of(2026, 9, 16), "2026-09", "desc",
                Locale.getDefault(), ra, model);

        Assertions.assertThat(view).isEqualTo("redirect:/expenses");
        Mockito.verify(expenseService).updateForScreen(ArgumentMatchers.eq(10L), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        Assertions.assertThat(ra.getFlashAttributes()).containsKey("successMessage");
    }
}
