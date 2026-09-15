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
import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.WorkshopOperation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.FactoryOperationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.WorkshopOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        List<SiteProfitDto> rows = new ArrayList<>();
        for (Project project : projectRepository.findAll()) {
            rows.add(profitFor(project));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public SiteProfitDto profitFor(Project project) {
        Map<ExpenseCategory, BigDecimal> byCategory = categoryMap(
                costTransactionRepository.sumCategoriesByProjectId(project.getId()));
        BigDecimal material = byCategory.getOrDefault(ExpenseCategory.MATERIAL, BigDecimal.ZERO);
        BigDecimal labor = byCategory.getOrDefault(ExpenseCategory.LABOR, BigDecimal.ZERO);
        BigDecimal tax = byCategory.getOrDefault(ExpenseCategory.TAX, BigDecimal.ZERO);
        BigDecimal consumable = byCategory.getOrDefault(ExpenseCategory.CONSUMABLE, BigDecimal.ZERO);
        BigDecimal transport = byCategory.getOrDefault(ExpenseCategory.TRANSPORTATION, BigDecimal.ZERO);
        BigDecimal other = byCategory.getOrDefault(ExpenseCategory.OTHER, BigDecimal.ZERO)
                .add(byCategory.getOrDefault(ExpenseCategory.FIXTURE_CONSUMABLE, BigDecimal.ZERO))
                .add(byCategory.getOrDefault(ExpenseCategory.DIESEL, BigDecimal.ZERO))
                .add(byCategory.getOrDefault(ExpenseCategory.ELECTRICITY, BigDecimal.ZERO));
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
        BigDecimal expense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.FACTORY, current));
        BigDecimal previousExpense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.FACTORY, previous));
        List<CostAnalysisDto.YieldRow> yields = factoryYields();
        BigDecimal outputM2 = yields.stream()
                .map(CostAnalysisDto.YieldRow::getOutputQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unitCost = outputM2.compareTo(BigDecimal.ZERO) > 0
                ? expense.divide(outputM2, Constants.COST_SCALE, java.math.RoundingMode.HALF_UP)
                : null;
        return base(BusinessUnit.FACTORY, current, previous, expense, previousExpense,
                outputM2, "m²", unitCost, null, outputM2.compareTo(BigDecimal.ZERO) == 0 && expense.compareTo(BigDecimal.ZERO) > 0)
                .yields(yields)
                .expenseByCategory(categoryTotals(BusinessUnit.FACTORY, current))
                .lines(lines(BusinessUnit.FACTORY, current))
                .build();
    }

    private CostAnalysisDto workshopAnalysis(String current, String previous) {
        BigDecimal expense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.WORKSHOP, current));
        BigDecimal previousExpense = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.WORKSHOP, previous));
        BigDecimal outputM2 = workshopOperationRepository.findAll().stream()
                .filter(op -> op.getStatus() == OperationStatus.COMPLETED)
                .map(WorkshopOperation::getOutputAreaM2)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unitCost = outputM2.compareTo(BigDecimal.ZERO) > 0
                ? expense.divide(outputM2, Constants.COST_SCALE, java.math.RoundingMode.HALF_UP)
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
        BigDecimal revenue = projectRepository.findAll().stream()
                .map(Project::getContractValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return base(BusinessUnit.SITE, current, previous, expense, previousExpense,
                revenue, "TL gelir", null, null, false)
                .expenseByCategory(categoryTotals(BusinessUnit.SITE, current))
                .lines(lines(BusinessUnit.SITE, current))
                .build();
    }

    private List<CostAnalysisDto.YieldRow> factoryYields() {
        Map<FactoryProcessType, List<FactoryOperation>> grouped = new LinkedHashMap<>();
        for (FactoryOperation operation : factoryOperationRepository.findAll()) {
            grouped.computeIfAbsent(operation.getProcessType(), key -> new ArrayList<>()).add(operation);
        }
        List<CostAnalysisDto.YieldRow> rows = new ArrayList<>();
        for (Map.Entry<FactoryProcessType, List<FactoryOperation>> entry : grouped.entrySet()) {
            FactoryProcessType type = entry.getKey();
            BigDecimal input = BigDecimal.ZERO;
            BigDecimal output = BigDecimal.ZERO;
            BigDecimal waste = BigDecimal.ZERO;
            for (FactoryOperation op : entry.getValue()) {
                input = input.add(zero(op.getInputQuantity()));
                output = output.add(zero(op.getOutputQuantity()));
                waste = waste.add(zero(op.getWasteQuantity()));
            }
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
