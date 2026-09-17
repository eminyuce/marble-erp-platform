package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.CostAnalysisDto;
import com.ozerler.marble.dto.SiteProfitDto;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.service.CostAccountingService;
import com.ozerler.marble.service.CostAnalysisService;
import com.ozerler.marble.service.ExpenseService;
import com.ozerler.marble.service.PricingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostControllerTest {

    @Mock
    private CostAccountingService costAccountingService;
    @Mock
    private CostAnalysisService costAnalysisService;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private PricingService pricingService;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CostController costController;

    @Test
    @DisplayName("costsIndex computes consolidated totals and safe contribution calculations")
    void costsIndex_ComputesTotalsAndRenders() {
        CostAnalysisDto quarryDto = CostAnalysisDto.builder()
                .businessUnit(BusinessUnit.QUARRY)
                .businessUnitLabel("Ocak")
                .expensePeriod("2026-09")
                .totalExpense(new BigDecimal("100000.00"))
                .productionQuantity(new BigDecimal("200.0"))
                .productionUnit("Ton")
                .unitCost(new BigDecimal("500.00"))
                .expenseByCategory(Map.of(
                        ExpenseCategory.DIESEL, new BigDecimal("40000.00"),
                        ExpenseCategory.LABOR, new BigDecimal("60000.00")
                ))
                .build();

        CostAnalysisDto emptyDto = CostAnalysisDto.builder()
                .businessUnit(BusinessUnit.FACTORY)
                .businessUnitLabel("Fabrika")
                .expensePeriod("2026-09")
                .totalExpense(BigDecimal.ZERO)
                .productionQuantity(BigDecimal.ZERO)
                .productionUnit("m²")
                .unitCost(BigDecimal.ZERO)
                .expenseByCategory(Map.of())
                .build();

        when(costAnalysisService.analyze(eq(BusinessUnit.QUARRY), any())).thenReturn(quarryDto);
        when(costAnalysisService.analyze(eq(BusinessUnit.FACTORY), any())).thenReturn(emptyDto);
        when(costAnalysisService.analyze(eq(BusinessUnit.WORKSHOP), any())).thenReturn(emptyDto);
        when(costAnalysisService.analyze(eq(BusinessUnit.SITE), any())).thenReturn(emptyDto);

        SiteProfitDto profitDto = SiteProfitDto.builder()
                .projectId(1L)
                .projectName("Test Rezidans")
                .projectCode("PRJ-01")
                .realizedRevenue(new BigDecimal("500000.00"))
                .materialCost(new BigDecimal("200000.00"))
                .laborCost(new BigDecimal("100000.00"))
                .transportationCost(new BigDecimal("20000.00"))
                .consumableCost(new BigDecimal("10000.00"))
                .otherCost(BigDecimal.ZERO)
                .totalCost(new BigDecimal("330000.00"))
                .netProfitOrLoss(new BigDecimal("170000.00"))
                .build();

        when(costAnalysisService.siteProfits()).thenReturn(List.of(profitDto));
        when(costAccountingService.getAllCostCenters()).thenReturn(List.of(
                CostCenter.builder().id(1L).code("CC-01").name("Ocak").monthlyBudget(new BigDecimal("150000.00")).build()
        ));

        Model model = new ConcurrentModel();
        String view = costController.costsIndex("2026-09", model);

        assertThat(view).isEqualTo("erp/costs/index");
        assertThat(model.getAttribute("totalPeriodExpense")).isEqualTo(new BigDecimal("100000.00"));
        assertThat(model.getAttribute("totalSiteRevenue")).isEqualTo(new BigDecimal("500000.00"));
        assertThat(model.getAttribute("totalSiteCost")).isEqualTo(new BigDecimal("330000.00"));
        assertThat(model.getAttribute("totalSiteNet")).isEqualTo(new BigDecimal("170000.00"));
        assertThat(model.getAttribute("totalSiteMarginPct")).isEqualTo(new BigDecimal("34.0"));
        assertThat(model.getAttribute("totalMonthlyBudget")).isEqualTo(new BigDecimal("150000.00"));

        // Test safe helper calculations on DTO
        BigDecimal dieselContribution = quarryDto.calculateUnitContribution(new BigDecimal("40000.00"));
        assertThat(dieselContribution).isEqualTo(new BigDecimal("200.00"));

        BigDecimal dieselShare = quarryDto.calculateCategorySharePct(new BigDecimal("40000.00"));
        assertThat(dieselShare).isEqualTo(new BigDecimal("40.0"));

        // Test safe zero/null handling on empty DTO
        assertThat(emptyDto.calculateUnitContribution(new BigDecimal("40000.00"))).isNull();
        assertThat(emptyDto.calculateCategorySharePct(new BigDecimal("40000.00"))).isEqualTo(BigDecimal.ZERO);

        // Test profit margin helper on DTO
        assertThat(profitDto.getMarginPct()).isEqualTo(new BigDecimal("34.0"));
    }
}
