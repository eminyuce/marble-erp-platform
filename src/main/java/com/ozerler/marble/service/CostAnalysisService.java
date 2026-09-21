package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.BlockMeasurement;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.domain.OperationYield;
import com.ozerler.marble.domain.QuarryPeriodCost;
import com.ozerler.marble.domain.SiteProfitAndLoss;
import com.ozerler.marble.dto.CostAnalysisDto;
import com.ozerler.marble.dto.SiteProfitDto;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.FactoryOperationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.WorkshopOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CostAnalysisService {

    private final CostTransactionRepository costTransactionRepository;
    private final BlockRepository blockRepository;
    private final FactoryOperationRepository factoryOperationRepository;
    private final WorkshopOperationRepository workshopOperationRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public CostAnalysisDto analyze(BusinessUnit unit, String period) {
        String current = ExpensePeriods.normalize(period != null ? period : YearMonth.now().toString());
        String previous = ExpensePeriods.of(ExpensePeriods.parse(current).minusMonths(1));
        return switch (unit) {
            case QUARRY -> quarryAnalysis(current, previous);
            case FACTORY -> factoryAnalysis(current, previous);
            case WORKSHOP -> workshopAnalysis(current, previous);
            case SITE -> siteAnalysis(current, previous);
        };
    }

    @Transactional(readOnly = true)
    public List<SiteProfitDto> siteProfits() {
        Map<Long, Map<ExpenseCategory, BigDecimal>> categoriesByProject = new HashMap<>();
        for (Object[] row : costTransactionRepository.sumCategoriesGroupedByProjectId()) {
            if (row[0] instanceof Long projectId && row[1] instanceof ExpenseCategory category) {
                categoriesByProject
                        .computeIfAbsent(projectId, ignored -> new EnumMap<>(ExpenseCategory.class))
                        .put(category, zero((BigDecimal) row[2]));
            }
        }
        List<SiteProfitDto> rows = new ArrayList<>();
        for (Project project : projectRepository.findAll()) {
            rows.add(profitFor(project, categoriesByProject.getOrDefault(project.getId(), Map.of())));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public SiteProfitDto profitFor(Project project) {
        return profitFor(project, categoryMap(costTransactionRepository.sumCategoriesByProjectId(project.getId())));
    }

    private SiteProfitDto profitFor(Project project, Map<ExpenseCategory, BigDecimal> byCategory) {
        BigDecimal material = byCategory.getOrDefault(ExpenseCategory.MATERIAL, BigDecimal.ZERO);
        BigDecimal labor = byCategory.getOrDefault(ExpenseCategory.LABOR, BigDecimal.ZERO);
        BigDecimal tax = byCategory.getOrDefault(ExpenseCategory.TAX, BigDecimal.ZERO);
        BigDecimal consumable = byCategory.getOrDefault(ExpenseCategory.CONSUMABLE, BigDecimal.ZERO);
        BigDecimal transport = byCategory.getOrDefault(ExpenseCategory.TRANSPORTATION, BigDecimal.ZERO);
        BigDecimal other = byCategory.getOrDefault(ExpenseCategory.OTHER, BigDecimal.ZERO)
                .add(byCategory.getOrDefault(ExpenseCategory.FIXTURE_CONSUMABLE, BigDecimal.ZERO))
                .add(byCategory.getOrDefault(ExpenseCategory.DIESEL, BigDecimal.ZERO))
                .add(byCategory.getOrDefault(ExpenseCategory.ELECTRICITY, BigDecimal.ZERO))
                .add(byCategory.getOrDefault(ExpenseCategory.MAINTENANCE, BigDecimal.ZERO));
        BigDecimal revenue = project.getContractValue() != null ? project.getContractValue() : BigDecimal.ZERO;
        SiteProfitAndLoss.Result result = SiteProfitAndLoss.calculate(
                material, labor, tax, consumable, transport, other, revenue);
        return SiteProfitDto.builder()
                .projectId(project.getId())
                .projectCode(project.getProjectCode())
                .projectName(project.getName())
                .realizedRevenue(result.realizedRevenue())
                .materialCost(material)
                .laborCost(labor)
                .taxCost(tax)
                .consumableCost(consumable)
                .transportationCost(transport)
                .otherCost(other)
                .totalCost(result.totalCost())
                .netProfitOrLoss(result.netProfitOrLoss())
                .build();
    }

    private CostAnalysisDto quarryAnalysis(String current, String previous) {
        BigDecimal expense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.QUARRY, current));
        BigDecimal previousExpense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.QUARRY, previous));
        BigDecimal tons = producedTons(current);
        BigDecimal previousTons = producedTons(previous);
        QuarryPeriodCost.QuarryPeriodCostResult result = QuarryPeriodCost.calculate(current, expense, tons);
        QuarryPeriodCost.QuarryPeriodCostResult previousResult = QuarryPeriodCost.calculate(previous, previousExpense, previousTons);
        return base(BusinessUnit.QUARRY, current, previous, expense, previousExpense,
                tons, "ton", result.costPerTon(), previousResult.costPerTon(), result.unallocatedCarryForward())
                .expenseByCategory(categoryTotals(BusinessUnit.QUARRY, current))
                .lines(lines(BusinessUnit.QUARRY, current))
                .build();
    }

    private CostAnalysisDto factoryAnalysis(String current, String previous) {
        BigDecimal factoryExpense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.FACTORY, current));
        BigDecimal previousFactoryExpense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.FACTORY, previous));
        BigDecimal incomingBlockCost = incomingFactoryBlockCost(current);
        BigDecimal previousIncoming = incomingFactoryBlockCost(previous);
        BigDecimal expense = factoryExpense.add(incomingBlockCost);
        BigDecimal previousExpense = previousFactoryExpense.add(previousIncoming);
        List<CostAnalysisDto.YieldRow> yields = factoryYields();
        BigDecimal outputM2 = yields.stream()
                .map(row -> row.getOutputQuantity())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal unitCost = outputM2.compareTo(BigDecimal.ZERO) > 0
                ? expense.divide(outputM2, Constants.COST_SCALE, RoundingMode.HALF_UP)
                : null;
        return base(BusinessUnit.FACTORY, current, previous, expense, previousExpense,
                outputM2, "m²", unitCost, null, outputM2.compareTo(BigDecimal.ZERO) == 0 && expense.compareTo(BigDecimal.ZERO) > 0)
                .incomingBlockCost(incomingBlockCost)
                .yields(yields)
                .expenseByCategory(categoryTotals(BusinessUnit.FACTORY, current))
                .lines(lines(BusinessUnit.FACTORY, current))
                .build();
    }

    private BigDecimal incomingFactoryBlockCost(String period) {
        YearMonth yearMonth = ExpensePeriods.parse(period);
        return zero(blockRepository.sumExtractionCostMovedTo(
                StockLocationType.FACTORY_BLOCK_YARD,
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.plusMonths(1).atDay(1).atStartOfDay()));
    }

    private CostAnalysisDto workshopAnalysis(String current, String previous) {
        BigDecimal expense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.WORKSHOP, current));
        BigDecimal previousExpense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.WORKSHOP, previous));
        BigDecimal outputM2 = zero(workshopOperationRepository.sumOutputAreaM2ByStatus(OperationStatus.COMPLETED));
        BigDecimal unitCost = outputM2.compareTo(BigDecimal.ZERO) > 0
                ? expense.divide(outputM2, Constants.COST_SCALE, RoundingMode.HALF_UP)
                : null;
        return base(BusinessUnit.WORKSHOP, current, previous, expense, previousExpense,
                outputM2, "m²", unitCost, null, outputM2.compareTo(BigDecimal.ZERO) == 0 && expense.compareTo(BigDecimal.ZERO) > 0)
                .expenseByCategory(categoryTotals(BusinessUnit.WORKSHOP, current))
                .lines(lines(BusinessUnit.WORKSHOP, current))
                .build();
    }

    private CostAnalysisDto siteAnalysis(String current, String previous) {
        BigDecimal expense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.SITE, current));
        BigDecimal previousExpense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.SITE, previous));
        BigDecimal revenue = zero(projectRepository.sumContractValue());
        return base(BusinessUnit.SITE, current, previous, expense, previousExpense,
                revenue, "TL gelir", null, null, false)
                .expenseByCategory(categoryTotals(BusinessUnit.SITE, current))
                .lines(lines(BusinessUnit.SITE, current))
                .build();
    }

    private List<CostAnalysisDto.YieldRow> factoryYields() {
        List<CostAnalysisDto.YieldRow> rows = new ArrayList<>();
        for (Object[] row : factoryOperationRepository.aggregateQuantitiesByProcessType()) {
            if (!(row[0] instanceof FactoryProcessType type)) {
                continue;
            }
            BigDecimal input = zero((BigDecimal) row[1]);
            BigDecimal output = zero((BigDecimal) row[2]);
            BigDecimal waste = zero((BigDecimal) row[3]);
            BigDecimal yieldValue;
            String yieldLabel;
            if (type.usesSamePhysicalUnit()) {
                yieldValue = OperationYield.yieldPercent(input, output);
                yieldLabel = "Verim %";
            } else {
                yieldValue = OperationYield.squareMetersPerTon(output, input);
                yieldLabel = "m²/ton";
            }
            rows.add(CostAnalysisDto.YieldRow.builder()
                    .processLabel(type.getLabel())
                    .inputQuantity(input)
                    .inputUnit(type.getDefaultInputUnit().getLabel())
                    .outputQuantity(output)
                    .outputUnit(type.getDefaultOutputUnit().getLabel())
                    .wasteQuantity(waste)
                    .wasteUnit(type.getDefaultOutputUnit().getLabel())
                    .yieldValue(yieldValue)
                    .yieldLabel(yieldLabel)
                    .build());
        }
        return rows;
    }

    private BigDecimal producedTons(String period) {
        YearMonth yearMonth = ExpensePeriods.parse(period);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.plusMonths(1).atDay(1);
        BigDecimal kg = blockRepository.sumProductionWeightKgBetween(start, end);
        return BlockMeasurement.kilogramsToTons(zero(kg));
    }

    private Map<ExpenseCategory, BigDecimal> categoryTotals(BusinessUnit unit, String period) {
        return categoryMap(costTransactionRepository.sumByCategoryForUnitAndPeriod(unit, period));
    }

    private Map<ExpenseCategory, BigDecimal> categoryMap(List<Object[]> rows) {
        Map<ExpenseCategory, BigDecimal> map = new EnumMap<>(ExpenseCategory.class);
        for (Object[] row : rows) {
            if (row[0] instanceof ExpenseCategory category) {
                map.put(category, zero((BigDecimal) row[1]));
            }
        }
        return map;
    }

    private List<CostAnalysisDto.Line> lines(BusinessUnit unit, String period) {
        List<CostAnalysisDto.Line> result = new ArrayList<>();
        for (CostTransaction tx : costTransactionRepository.findByBusinessUnitAndExpensePeriod(unit, period)) {
            result.add(CostAnalysisDto.Line.builder()
                    .id(tx.getId())
                    .categoryLabel(tx.getExpenseCategory() != null ? tx.getExpenseCategory().getLabel() : tx.getExpenseType().getLabel())
                    .description(tx.getDescription())
                    .amount(tx.getAmount())
                    .documentNo(tx.getDocumentNo())
                    .build());
        }
        return result;
    }

    private CostAnalysisDto.CostAnalysisDtoBuilder base(BusinessUnit unit, String current, String previous,
                                                        BigDecimal expense, BigDecimal previousExpense,
                                                        BigDecimal quantity, String unitLabel,
                                                        BigDecimal unitCost, BigDecimal previousUnitCost,
                                                        boolean unallocated) {
        return CostAnalysisDto.builder()
                .businessUnit(unit)
                .businessUnitLabel(unit.getLabel())
                .expensePeriod(current)
                .previousPeriod(previous)
                .totalExpense(expense)
                .previousExpense(previousExpense)
                .productionQuantity(quantity)
                .productionUnit(unitLabel)
                .unitCost(unitCost)
                .previousUnitCost(previousUnitCost)
                .unallocatedCarryForward(unallocated)
                .yields(List.of());
    }

    private static BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
