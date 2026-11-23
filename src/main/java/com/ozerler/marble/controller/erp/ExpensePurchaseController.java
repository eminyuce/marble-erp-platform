package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.domain.ExpenseLineItems;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.ExpenseSummaryDto;
import com.ozerler.marble.dto.QuarryCostBreakdownDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.BlockCostCalculationService;
import com.ozerler.marble.service.ExpenseService;
import com.ozerler.marble.web.ExpenseGrid;
import com.ozerler.marble.web.ListQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpensePurchaseController extends AbstractController {

    private final com.ozerler.marble.service.FormLookupService formLookup;

    private final ExpenseService expenseService;
    private final BlockCostCalculationService blockCostCalculationService;
    private final MessageSource messageSource;

    public String index(BusinessUnit unit, Long quarryId, String period, Model model) {
        return index(unit, quarryId, period, null, new ListQuery(), model);
    }

    @GetMapping
    public String index(@RequestParam(value = "unit", required = false) BusinessUnit unit,
                        @RequestParam(value = "quarryId", required = false) Long quarryId,
                        @RequestParam(value = "period", required = false) String period,
                        @RequestParam(value = "expenseType", required = false) ExpenseType expenseType,
                        @ModelAttribute ListQuery query, Model model) {
        String currentPeriod;
        if (period != null && !period.isBlank()) {
            currentPeriod = period;
        } else {
            currentPeriod = YearMonth.now().toString();
        }
        ExpenseSummaryDto summary = expenseService.getExpenseSummary(currentPeriod);
        TabulatorResponse<ExpenseDto> expenses = expenseService.getExpensesPaged(
                query.pageNumber(), query.pageSize(), query.getSearch(), query.sortKey(), query.sortDirection(), unit, currentPeriod, expenseType, null, quarryId, null);

        model.addAttribute("summary", summary);
        model.addAttribute("selectedUnit", unit);
        model.addAttribute("period", currentPeriod);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("costCenters", formLookup.costCenters());
        model.addAttribute("quarries", formLookup.quarries());
        model.addAttribute("projects", formLookup.projects());
        model.addAttribute("expenseTypes", ExpenseType.values());
        model.addAttribute("selectedQuarryId", quarryId);
        Object valueValue;
        if (expenseType == null) {
            valueValue = "";
        } else {
            valueValue = expenseType.name();
        }
        model.addAttribute("selectedExpenseType", valueValue);
        model.addAttribute("grid", ExpenseGrid.view(expenses, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        if (expenses != null && expenses.getMeta() != null) {
            model.addAttribute("filteredAmount", expenses.getMeta().get("totalAmount"));
            model.addAttribute("filteredCount", expenses.getTotal());
        }

        if (unit == BusinessUnit.QUARRY && quarryId != null) {
            QuarryCostBreakdownDto breakdown = blockCostCalculationService.quarryBreakdown(quarryId, currentPeriod);
            model.addAttribute("quarryBreakdown", breakdown);
        }
        return "erp/expenses/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<ExpenseDto> getExpensesData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir,
            @RequestParam(value = "unit", required = false) BusinessUnit unit,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "expenseType", required = false) ExpenseType expenseType,
            @RequestParam(value = "centerId", required = false) Long centerId,
            @RequestParam(value = "quarryId", required = false) Long quarryId,
            @RequestParam(value = "projectId", required = false) Long projectId) {

        return expenseService.getExpensesPaged(page, size, search, sortField, sortDir,
                unit, period, expenseType, centerId, quarryId, projectId);
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ExpenseDto getExpenseDetail(@PathVariable("id") Long id) {
        return expenseService.getExpenseDto(id);
    }

    @GetMapping("/api/options")
    @ResponseBody
    public Map<String, Object> getUnitOptions(@RequestParam(value = "unit", required = false) BusinessUnit unit) {
        if (unit == null) {
            List<Map<String, String>> types = List.of(ExpenseType.values()).stream()
                    .map(t -> Map.of("name", t.name(), "label", t.getLabel()))
                    .toList();
            List<Map<String, Object>> centers = formLookup.costCenters().stream()
                    .map(c -> Map.<String, Object>of("id", c.getId(), "code", c.getCode(), "name", c.getName()))
                    .toList();
            return Map.of("types", types, "centers", centers);
        }
        List<Map<String, String>> types = ExpenseLineItems.forUnit(unit).stream()
                .map(t -> Map.of("name", t.name(), "label", t.getLabel()))
                .toList();
        List<Map<String, Object>> centers = formLookup.costCenters(unit).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "code", c.getCode(), "name", c.getName()))
                .toList();
        return Map.of("types", types, "centers", centers);
    }

    @PostMapping("/api")
    @ResponseBody
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public BackEndResponse createExpenseApi(
            @RequestParam("businessUnit") BusinessUnit businessUnit,
            @RequestParam(value = "centerId", required = false) Long centerId,
            @RequestParam("expenseType") ExpenseType expenseType,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("entryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
            @RequestParam(value = "quarryId", required = false) Long quarryId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "documentNo", required = false) String documentNo,
            @RequestParam(value = "invoiceDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
            @RequestParam(value = "expensePeriod", required = false) String expensePeriod,
            @RequestParam(value = "description", required = false) String description,
            Locale locale) {
        return writeExpenseApi(ExpenseApiAction.CREATE, null, expenseWrite(
                businessUnit, centerId, expenseType, amount, entryDate, quarryId, projectId,
                documentNo, invoiceDate, expensePeriod, description), locale);
    }

    @PostMapping("/api/{id}/edit")
    @ResponseBody
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public BackEndResponse updateExpenseApi(
            @PathVariable("id") Long id,
            @RequestParam(name = "businessUnit") BusinessUnit businessUnit,
            @RequestParam(name = "centerId", required = false) Long centerId,
            @RequestParam(name = "expenseType") ExpenseType expenseType,
            @RequestParam(name = "amount") BigDecimal amount,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(name = "entryDate") LocalDate entryDate,
            @RequestParam(name = "quarryId", required = false) Long quarryId,
            @RequestParam(name = "projectId", required = false) Long projectId,
            @RequestParam(name = "documentNo", required = false) String documentNo,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(name = "invoiceDate", required = false) LocalDate invoiceDate,
            @RequestParam(name = "expensePeriod", required = false) String expensePeriod,
            @RequestParam(name = "description", required = false) String description,
            Locale locale) {
        return writeExpenseApi(ExpenseApiAction.UPDATE, id, expenseWrite(
                businessUnit, centerId, expenseType, amount, entryDate, quarryId, projectId,
                documentNo, invoiceDate, expensePeriod, description), locale);
    }

    @PostMapping("/api/{id}/delete")
    @ResponseBody
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public BackEndResponse deleteExpenseApi(@PathVariable("id") Long id, Locale locale) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            expenseService.deleteExpense(id);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage(messageSource.getMessage("erp.expense.delete.success", null, locale));
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
            ber.setResponse(ResponseEntity.ok().build());
            return ber;
        } catch (Exception e) {
            log.error("Error deleting expense {} via API", id, e);
            return buildFatalResponse(ber, serviceStatus, status, "deleteExpenseApi", Constants.ERR_FATAL);
        }
    }

    @GetMapping("/create")
    public String createForm(@RequestParam(value = "unit", required = false) BusinessUnit unit, Model model) {
        model.addAttribute("record", null);
        model.addAttribute("selectedUnit", unit);
        populateFormLookups(model);
        return "erp/expenses/form";
    }

    public String createForm(Model model) {
        return createForm(null, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id, Model model) {
        ExpenseDto dto = expenseService.getExpenseDto(id);
        model.addAttribute("record", dto);
        populateFormLookups(model);
        return "erp/expenses/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        ExpenseDto dto = expenseService.getExpenseDto(id);
        model.addAttribute("expense", dto);
        return "erp/expenses/detail";
    }

    @PostMapping(path = {"", "/create"})
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public String recordExpense(@RequestParam(value = "businessUnit") BusinessUnit businessUnit,
                                @RequestParam(required = false, value = "centerId") Long centerId,
                                @RequestParam(value = "expenseType") ExpenseType expenseType,
                                @RequestParam(value = "amount") BigDecimal amount,
                                @RequestParam(value = "entryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
                                @RequestParam(required = false, value = "quarryId") Long quarryId,
                                @RequestParam(required = false, value = "projectId") Long projectId,
                                @RequestParam(required = false, value = "documentNo") String documentNo,
                                @RequestParam(required = false, value = "invoiceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
                                @RequestParam(required = false, value = "expensePeriod") String expensePeriod,
                                @RequestParam(required = false, value = "description") String description,
                                Locale locale,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            var write = expenseWrite(
                    businessUnit, centerId, expenseType, amount, entryDate, quarryId, projectId,
                    documentNo, invoiceDate, expensePeriod, description);
            expenseService.recordForScreen(toDraft(write), write.projectId(), write.quarryId());

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.expense.record.success", null, locale));
            return "redirect:/expenses";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateFormLookups(model);
            return "erp/expenses/form";
        }
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public String updateExpense(@PathVariable(name = "id") Long id,
                                @RequestParam(name = "businessUnit", required = true) BusinessUnit businessUnit,
                                @RequestParam(required = false, name = "centerId") Long centerId,
                                @RequestParam(name = "expenseType", required = true) ExpenseType expenseType,
                                @RequestParam(name = "amount", required = true) BigDecimal amount,
                                @RequestParam(name = "entryDate", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
                                @RequestParam(required = false, name = "quarryId") Long quarryId,
                                @RequestParam(required = false, name = "projectId") Long projectId,
                                @RequestParam(required = false, name = "documentNo") String documentNo,
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @RequestParam(required = false, name = "invoiceDate") LocalDate invoiceDate,
                                @RequestParam(required = false, name = "expensePeriod") String expensePeriod,
                                @RequestParam(required = false, name = "description") String description,
                                Locale locale,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            var write = expenseWrite(
                    businessUnit, centerId, expenseType, amount, entryDate, quarryId, projectId,
                    documentNo, invoiceDate, expensePeriod, description);
            expenseService.updateForScreen(id, toDraft(write), write.projectId(), write.quarryId());

            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.expense.update.success", null, locale));
            return "redirect:/expenses";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            ExpenseDto dto = expenseService.getExpenseDto(id);
            model.addAttribute("record", dto);
            populateFormLookups(model);
            return "erp/expenses/form";
        }
    }

    private void populateFormLookups(Model model) {
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("costCenters", formLookup.costCenters());
        model.addAttribute("quarries", formLookup.quarries());
        model.addAttribute("projects", formLookup.projects());
        model.addAttribute("expenseTypes", ExpenseType.values());
    }

    private Long resolveCenterId(BusinessUnit unit, Long centerId) {
        if (centerId != null) {
            return centerId;
        }
        return formLookup.firstCostCenterId(unit)
                .orElseGet(() -> formLookup.anyCostCenterId().orElse(null));
    }

    private enum ExpenseApiAction {
        CREATE, UPDATE
    }

    private record ExpenseWrite(BusinessUnit businessUnit, Long centerId, ExpenseType expenseType, BigDecimal amount,
                                LocalDate entryDate, Long quarryId, Long projectId, String documentNo,
                                LocalDate invoiceDate, String expensePeriod, String description) {
    }

    private static ExpenseWrite expenseWrite(BusinessUnit businessUnit, Long centerId, ExpenseType expenseType,
                                             BigDecimal amount, LocalDate entryDate, Long quarryId, Long projectId,
                                             String documentNo, LocalDate invoiceDate, String expensePeriod,
                                             String description) {
        return new ExpenseWrite(businessUnit, centerId, expenseType, amount, entryDate, quarryId, projectId,
                documentNo, invoiceDate, expensePeriod, description);
    }

    private ExpenseService.ExpenseDraft toDraft(ExpenseWrite write) {
        Long effectiveCenterId = resolveCenterId(write.businessUnit(), write.centerId());
        String period;
        if (write.expensePeriod() != null && !write.expensePeriod().isBlank()) {
            period = ExpensePeriods.normalize(write.expensePeriod());
        } else {
            period = ExpensePeriods.expensePeriod(write.expenseType(), write.invoiceDate(), write.entryDate());
        }
        return new ExpenseService.ExpenseDraft(
                effectiveCenterId, write.expenseType(), null, write.businessUnit(), write.amount(), Constants.CURRENCY_TRY,
                write.documentNo(), write.invoiceDate(), write.entryDate(), period, YearMonth.now().toString(),
                null, null, null, null, null, null, null, null, write.description());
    }

    private BackEndResponse writeExpenseApi(ExpenseApiAction action, Long id, ExpenseWrite write, Locale locale) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);
        try {
            ExpenseService.ExpenseDraft draft = toDraft(write);
            if (action == ExpenseApiAction.CREATE) {
                expenseService.recordForScreen(draft, write.projectId(), write.quarryId());
                return expenseApiOk(ber, serviceStatus, status,
                        messageSource.getMessage("erp.expense.record.success", null, locale));
            }
            expenseService.updateForScreen(id, draft, write.projectId(), write.quarryId());
            return expenseApiOk(ber, serviceStatus, status,
                    messageSource.getMessage("erp.expense.update.success", null, locale));
        } catch (Exception e) {
            if (action == ExpenseApiAction.CREATE) {
                log.error("Error creating expense via API", e);
                return buildFatalResponse(ber, serviceStatus, status, "createExpenseApi", Constants.ERR_FATAL);
            }
            log.error("Error updating expense {} via API", id, e);
            return buildFatalResponse(ber, serviceStatus, status, "updateExpenseApi", Constants.ERR_FATAL);
        }
    }

    private static BackEndResponse expenseApiOk(BackEndResponse ber, ServiceStatus serviceStatus, Status status, String message) {
        serviceStatus.setHttpStatus(HttpStatus.OK);
        status.setMessage(message);
        serviceStatus.setStatus(status);
        ber.setServiceStatus(serviceStatus);
        ber.setResponse(ResponseEntity.ok().build());
        return ber;
    }

}
