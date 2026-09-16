package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.domain.ExpenseLineItems;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.ExpenseSummaryDto;
import com.ozerler.marble.dto.QuarryCostBreakdownDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.service.BlockCostCalculationService;
import com.ozerler.marble.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpensePurchaseController extends AbstractController {

    private final ExpenseService expenseService;
    private final CostCenterRepository costCenterRepository;
    private final QuarryRepository quarryRepository;
    private final ProjectRepository projectRepository;
    private final BlockCostCalculationService blockCostCalculationService;
    private final MessageSource messageSource;

    @GetMapping
    public String index(@RequestParam(value = "unit", required = false) BusinessUnit unit,
                        @RequestParam(value = "quarryId", required = false) Long quarryId,
                        @RequestParam(value = "period", required = false) String period,
                        Model model) {
        String currentPeriod = period != null && !period.isBlank() ? period : YearMonth.now().toString();
        ExpenseSummaryDto summary = expenseService.getExpenseSummary(currentPeriod);

        model.addAttribute("summary", summary);
        model.addAttribute("selectedUnit", unit);
        model.addAttribute("period", currentPeriod);
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("costCenters", costCenterRepository.findAll());
        model.addAttribute("quarries", quarryRepository.findAll());
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("expenseTypes", ExpenseType.values());
        model.addAttribute("selectedQuarryId", quarryId);

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
            List<Map<String, Object>> centers = costCenterRepository.findAll().stream()
                    .map(c -> Map.<String, Object>of("id", c.getId(), "code", c.getCode(), "name", c.getName()))
                    .toList();
            return Map.of("types", types, "centers", centers);
        }
        List<Map<String, String>> types = ExpenseLineItems.forUnit(unit).stream()
                .map(t -> Map.of("name", t.name(), "label", t.getLabel()))
                .toList();
        List<Map<String, Object>> centers = costCenterRepository.findByBusinessUnit(unit).stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "code", c.getCode(), "name", c.getName()))
                .toList();
        return Map.of("types", types, "centers", centers);
    }

    @PostMapping("/api")
    @ResponseBody
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public BackEndResponse createExpenseApi(
            @RequestParam("businessUnit") BusinessUnit businessUnit,
            @RequestParam("centerId") Long centerId,
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

        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            Project project = resolveProject(businessUnit, projectId, locale);
            Quarry quarry = resolveQuarry(businessUnit, quarryId, locale);
            String period = expensePeriod != null && !expensePeriod.isBlank()
                    ? ExpensePeriods.normalize(expensePeriod)
                    : ExpensePeriods.expensePeriod(expenseType, invoiceDate, entryDate);

            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    centerId, expenseType, null, businessUnit, amount, Constants.CURRENCY_TRY,
                    documentNo, invoiceDate, entryDate, period, YearMonth.now().toString(),
                    null, quarry, null, project, null, null, null, null, description));

            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage(messageSource.getMessage("erp.expense.record.success", null, locale));
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
            ber.setResponse(ResponseEntity.ok().build());
            return ber;
        } catch (Exception e) {
            log.error("Error creating expense via API", e);
            return buildFatalResponse(ber, serviceStatus, status, "createExpenseApi", Constants.ERR_FATAL);
        }
    }

    @PostMapping("/api/{id}/edit")
    @ResponseBody
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public BackEndResponse updateExpenseApi(
            @PathVariable("id") Long id,
            @RequestParam("businessUnit") BusinessUnit businessUnit,
            @RequestParam("centerId") Long centerId,
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

        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            Project project = resolveProject(businessUnit, projectId, locale);
            Quarry quarry = resolveQuarry(businessUnit, quarryId, locale);
            String period = expensePeriod != null && !expensePeriod.isBlank()
                    ? ExpensePeriods.normalize(expensePeriod)
                    : ExpensePeriods.expensePeriod(expenseType, invoiceDate, entryDate);

            expenseService.updateExpense(id, new ExpenseService.ExpenseDraft(
                    centerId, expenseType, null, businessUnit, amount, Constants.CURRENCY_TRY,
                    documentNo, invoiceDate, entryDate, period, YearMonth.now().toString(),
                    null, quarry, null, project, null, null, null, null, description));

            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage(messageSource.getMessage("erp.expense.update.success", null, locale));
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
            ber.setResponse(ResponseEntity.ok().build());
            return ber;
        } catch (Exception e) {
            log.error("Error updating expense {} via API", id, e);
            return buildFatalResponse(ber, serviceStatus, status, "updateExpenseApi", Constants.ERR_FATAL);
        }
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
    public String createForm(Model model) {
        model.addAttribute("record", null);
        populateFormLookups(model);
        return "erp/expenses/form";
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
    public String recordExpense(@RequestParam("businessUnit") BusinessUnit businessUnit,
                                @RequestParam("centerId") Long centerId,
                                @RequestParam("expenseType") ExpenseType expenseType,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("entryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
                                @RequestParam(value = "quarryId", required = false) Long quarryId,
                                @RequestParam(value = "projectId", required = false) Long projectId,
                                @RequestParam(value = "documentNo", required = false) String documentNo,
                                @RequestParam(value = "invoiceDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
                                @RequestParam(value = "expensePeriod", required = false) String expensePeriod,
                                @RequestParam(value = "description", required = false) String description,
                                Locale locale,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            Project project = resolveProject(businessUnit, projectId, locale);
            Quarry quarry = resolveQuarry(businessUnit, quarryId, locale);
            String period = expensePeriod != null && !expensePeriod.isBlank()
                    ? ExpensePeriods.normalize(expensePeriod)
                    : ExpensePeriods.expensePeriod(expenseType, invoiceDate, entryDate);

            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    centerId, expenseType, null, businessUnit, amount, Constants.CURRENCY_TRY,
                    documentNo, invoiceDate, entryDate, period, YearMonth.now().toString(),
                    null, quarry, null, project, null, null, null, null, description));

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
    public String updateExpense(@PathVariable("id") Long id,
                                @RequestParam("businessUnit") BusinessUnit businessUnit,
                                @RequestParam("centerId") Long centerId,
                                @RequestParam("expenseType") ExpenseType expenseType,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("entryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entryDate,
                                @RequestParam(value = "quarryId", required = false) Long quarryId,
                                @RequestParam(value = "projectId", required = false) Long projectId,
                                @RequestParam(value = "documentNo", required = false) String documentNo,
                                @RequestParam(value = "invoiceDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
                                @RequestParam(value = "expensePeriod", required = false) String expensePeriod,
                                @RequestParam(value = "description", required = false) String description,
                                Locale locale,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            Project project = resolveProject(businessUnit, projectId, locale);
            Quarry quarry = resolveQuarry(businessUnit, quarryId, locale);
            String period = expensePeriod != null && !expensePeriod.isBlank()
                    ? ExpensePeriods.normalize(expensePeriod)
                    : ExpensePeriods.expensePeriod(expenseType, invoiceDate, entryDate);

            expenseService.updateExpense(id, new ExpenseService.ExpenseDraft(
                    centerId, expenseType, null, businessUnit, amount, Constants.CURRENCY_TRY,
                    documentNo, invoiceDate, entryDate, period, YearMonth.now().toString(),
                    null, quarry, null, project, null, null, null, null, description));

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
        model.addAttribute("costCenters", costCenterRepository.findAll());
        model.addAttribute("quarries", quarryRepository.findAll());
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("expenseTypes", ExpenseType.values());
    }

    private Project resolveProject(BusinessUnit unit, Long projectId, Locale locale) {
        if (unit != BusinessUnit.SITE) {
            return null;
        }
        if (projectId == null) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.expense.site.project.required", null, locale));
        }
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.project.not_found", new Object[]{projectId}, locale)));
    }

    private Quarry resolveQuarry(BusinessUnit unit, Long quarryId, Locale locale) {
        if (unit != BusinessUnit.QUARRY || quarryId == null) {
            return null;
        }
        return quarryRepository.findById(quarryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageSource.getMessage("error.quarry.not_found", new Object[]{quarryId}, locale)));
    }
}
