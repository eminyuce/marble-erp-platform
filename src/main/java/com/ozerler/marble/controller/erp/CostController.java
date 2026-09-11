package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.CostBreakdownDto;
import com.ozerler.marble.service.CostAccountingService;
import com.ozerler.marble.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/costs")
@RequiredArgsConstructor
public class CostController {

    private final CostAccountingService costAccountingService;
    private final PricingService pricingService;

    @GetMapping
    public String costsIndex(Model model) {
        model.addAttribute("costCenters", costAccountingService.getAllCostCenters());
        model.addAttribute("costDistribution", costAccountingService.getCostDistributionByCenter());
        model.addAttribute("expenseDistribution", costAccountingService.getCostDistributionByType());
        model.addAttribute("totalExpenses", costAccountingService.getTotalExpenses());

        // Standard BRD multi-layer example breakdown
        CostBreakdownDto sampleCost = costAccountingService.calculateMultiLayerCost(
                "FG-80120-042 (Crema Marfil 80x120)", "Crema Marfil",
                new BigDecimal("820.00"), new BigDecimal("210.00"), new BigDecimal("165.00"),
                new BigDecimal("95.00"), new BigDecimal("45.00"), new BigDecimal("30.00"),
                new BigDecimal("30.00")
        );
        model.addAttribute("sampleCost", sampleCost);

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
}
