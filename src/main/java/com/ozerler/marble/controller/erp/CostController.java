package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.service.CostAccountingService;
import com.ozerler.marble.service.CostAnalysisService;
import com.ozerler.marble.service.ExpenseService;
import com.ozerler.marble.service.PricingService;
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
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/costs")
@RequiredArgsConstructor
public class CostController {

    private final CostAccountingService costAccountingService;
    private final CostAnalysisService costAnalysisService;
    private final ExpenseService expenseService;
    private final PricingService pricingService;
    private final MessageSource messageSource;

    @GetMapping
    public String costsIndex(@RequestParam(value = "period", required = false) String period, Model model) {
        String current = period != null && !period.isBlank() ? period : YearMonth.now().toString();
        model.addAttribute("period", current);
        model.addAttribute("quarryAnalysis", costAnalysisService.analyze(BusinessUnit.QUARRY, current));
        model.addAttribute("factoryAnalysis", costAnalysisService.analyze(BusinessUnit.FACTORY, current));
        model.addAttribute("workshopAnalysis", costAnalysisService.analyze(BusinessUnit.WORKSHOP, current));
        model.addAttribute("siteAnalysis", costAnalysisService.analyze(BusinessUnit.SITE, current));
        model.addAttribute("siteProfits", costAnalysisService.siteProfits());
        model.addAttribute("costCenters", costAccountingService.getAllCostCenters());
        model.addAttribute("expenseTypes", ExpenseType.values());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("totalExpenses", costAccountingService.getTotalExpenses());
        return "erp/costs/index";
    }

    @PostMapping("/simulate-pricing")
    public String simulatePricing(@RequestParam("unitCost") BigDecimal unitCost,
                                  @RequestParam("targetMarginPct") BigDecimal targetMarginPct,
                                  @RequestParam(value = "discountPct", defaultValue = "0") BigDecimal discountPct,
                                  Model model) {

        Map<String, Object> simulation = pricingService.simulatePrice(unitCost, targetMarginPct, discountPct);
        model.addAttribute("sim", simulation);
        return "erp/costs/pricing-result :: pricingResultFragment";
    }

    @PostMapping("/expenses")
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public String recordExpense(@RequestParam("centerId") Long centerId,
                                @RequestParam("expenseType") ExpenseType expenseType,
                                @RequestParam("businessUnit") BusinessUnit businessUnit,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam(value = "documentNo", required = false) String documentNo,
                                @RequestParam(value = "invoiceDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
                                @RequestParam(value = "expensePeriod", required = false) String expensePeriod,
                                @RequestParam(value = "description", required = false) String description,
                                Locale locale,
                                RedirectAttributes redirectAttributes) {
        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                centerId, expenseType, null, businessUnit, amount, Constants.CURRENCY_TRY,
                documentNo, invoiceDate, LocalDate.now(), expensePeriod, YearMonth.now().toString(),
                null, null, null, null, null, null, null, description));
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.cost.expense.success", null, locale));
        return "redirect:/costs";
    }

    @PostMapping("/periods/close")
    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    public String closePeriod(@RequestParam("businessUnit") BusinessUnit businessUnit,
                              @RequestParam("period") String period,
                              @RequestParam(value = "notes", required = false) String notes,
                              Locale locale,
                              RedirectAttributes redirectAttributes) {
        expenseService.closePeriod(businessUnit, period, notes);
        redirectAttributes.addFlashAttribute("successMessage",
                messageSource.getMessage("erp.cost.period.close.success", null, locale));
        return "redirect:/costs?period=" + period;
    }
}
