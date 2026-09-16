package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.ExpenseLineItems;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.dto.QuarryCostBreakdownDto;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.service.BlockCostCalculationService;
import com.ozerler.marble.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpensePurchaseController {

    private final ExpenseService expenseService;
    private final CostCenterRepository costCenterRepository;
    private final QuarryRepository quarryRepository;
    private final ProjectRepository projectRepository;
    private final BlockCostCalculationService blockCostCalculationService;
    private final MessageSource messageSource;

    @GetMapping
    public String index(@RequestParam(value = "unit", defaultValue = "QUARRY") BusinessUnit unit,
                        @RequestParam(value = "quarryId", required = false) Long quarryId,
                        @RequestParam(value = "period", required = false) String period,
                        Model model) {
        String currentPeriod = period != null && !period.isBlank() ? period : YearMonth.now().toString();
        model.addAttribute("selectedUnit", unit);
        model.addAttribute("period", currentPeriod);
        model.addAttribute("expenseTypes", ExpenseLineItems.forUnit(unit));
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("costCenters", costCenterRepository.findByBusinessUnit(unit));
        model.addAttribute("quarries", quarryRepository.findAll());
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("recentExpenses", expenseService.listByUnitAndPeriod(unit, currentPeriod));
        model.addAttribute("selectedQuarryId", quarryId);

        if (unit == BusinessUnit.QUARRY && quarryId != null) {
            QuarryCostBreakdownDto breakdown = blockCostCalculationService.quarryBreakdown(quarryId, currentPeriod);
            model.addAttribute("quarryBreakdown", breakdown);
        }
        return "erp/expenses/index";
    }

    @PostMapping
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
                                RedirectAttributes redirectAttributes) {
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
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
        }
        redirectAttributes.addAttribute("unit", businessUnit.name());
        if (quarryId != null) {
            redirectAttributes.addAttribute("quarryId", quarryId);
        }
        if (expensePeriod != null && !expensePeriod.isBlank()) {
            redirectAttributes.addAttribute("period", expensePeriod);
        }
        return "redirect:/expenses";
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
