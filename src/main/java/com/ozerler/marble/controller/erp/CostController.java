package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.ProjectRepository;
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
    private final ProjectRepository projectRepository;
    private final MessageSource messageSource;

    @GetMapping
    public String costsIndex(@RequestParam(value = "period", required = false) String period, Model model) {
        String current = period != null && !period.isBlank() ? period : YearMonth.now().toString();
        model.addAttribute("period", current);
        var quarry = costAnalysisService.analyze(BusinessUnit.QUARRY, current);
        var factory = costAnalysisService.analyze(BusinessUnit.FACTORY, current);
        var workshop = costAnalysisService.analyze(BusinessUnit.WORKSHOP, current);
        var site = costAnalysisService.analyze(BusinessUnit.SITE, current);
        var siteProfits = costAnalysisService.siteProfits();

        model.addAttribute("quarryAnalysis", quarry);
        model.addAttribute("factoryAnalysis", factory);
        model.addAttribute("workshopAnalysis", workshop);
        model.addAttribute("siteAnalysis", site);
        model.addAttribute("siteProfits", siteProfits);
        model.addAttribute("costCenters", costAccountingService.getAllCostCenters());
        model.addAttribute("expenseTypes", ExpenseType.values());
        model.addAttribute("businessUnits", BusinessUnit.values());
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("totalExpenses", costAccountingService.getTotalExpenses());

        BigDecimal totalPeriodExpense = java.util.stream.Stream.of(
                        quarry.getTotalExpense(), factory.getTotalExpense(),
                        workshop.getTotalExpense(), site.getTotalExpense())
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalPeriodExpense", totalPeriodExpense);

        BigDecimal totalSiteRevenue = siteProfits.stream()
                .map(com.ozerler.marble.dto.SiteProfitDto::getRealizedRevenue)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMaterialCost = siteProfits.stream()
                .map(com.ozerler.marble.dto.SiteProfitDto::getMaterialCost)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLaborCost = siteProfits.stream()
                .map(com.ozerler.marble.dto.SiteProfitDto::getLaborCost)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOtherCost = siteProfits.stream()
                .map(p -> (p.getTransportationCost() != null ? p.getTransportationCost() : BigDecimal.ZERO)
                        .add(p.getConsumableCost() != null ? p.getConsumableCost() : BigDecimal.ZERO)
                        .add(p.getOtherCost() != null ? p.getOtherCost() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSiteCost = siteProfits.stream()
                .map(com.ozerler.marble.dto.SiteProfitDto::getTotalCost)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSiteNet = siteProfits.stream()
                .map(com.ozerler.marble.dto.SiteProfitDto::getNetProfitOrLoss)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalSiteRevenue", totalSiteRevenue);
        model.addAttribute("totalMaterialCost", totalMaterialCost);
        model.addAttribute("totalLaborCost", totalLaborCost);
        model.addAttribute("totalOtherCost", totalOtherCost);
        model.addAttribute("totalSiteCost", totalSiteCost);
        model.addAttribute("totalSiteNet", totalSiteNet);

        BigDecimal totalSiteMarginPct = (totalSiteRevenue != null && totalSiteRevenue.compareTo(BigDecimal.ZERO) > 0 && totalSiteNet != null)
                ? totalSiteNet.multiply(BigDecimal.valueOf(100)).divide(totalSiteRevenue, 1, java.math.RoundingMode.HALF_UP)
                : null;
        model.addAttribute("totalSiteMarginPct", totalSiteMarginPct);

        BigDecimal totalMonthlyBudget = costAccountingService.getAllCostCenters().stream()
                .map(com.ozerler.marble.model.CostCenter::getMonthlyBudget)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalMonthlyBudget", totalMonthlyBudget);

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
                                @RequestParam(value = "projectId", required = false) Long projectId,
                                @RequestParam(value = "documentNo", required = false) String documentNo,
                                @RequestParam(value = "invoiceDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate invoiceDate,
                                @RequestParam(value = "expensePeriod", required = false) String expensePeriod,
                                @RequestParam(value = "description", required = false) String description,
                                Locale locale,
                                RedirectAttributes redirectAttributes) {
        try {
            Project project = null;
            if (projectId != null) {
                project = projectRepository.findById(projectId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                messageSource.getMessage("error.project.not_found", new Object[]{projectId}, locale)));
            }
            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    centerId, expenseType, null, businessUnit, amount, Constants.CURRENCY_TRY,
                    documentNo, invoiceDate, LocalDate.now(), expensePeriod, YearMonth.now().toString(),
                    null, null, null, project, null, null, null, null, description));
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.cost.expense.success", null, locale));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
        }
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
