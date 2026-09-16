package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostPeriodClose;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostPeriodCloseRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.security.SecurityUtils;
import com.ozerler.marble.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.ExpenseSummaryDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.util.GridPages;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final CostTransactionRepository costTransactionRepository;
    private final CostCenterRepository costCenterRepository;
    private final CostPeriodCloseRepository costPeriodCloseRepository;

    @Transactional
    public CostTransaction recordExpense(ExpenseDraft draft) {
        Objects.requireNonNull(draft, MessageUtils.getMessage("error.expense.required"));
        Objects.requireNonNull(draft.centerId(), MessageUtils.getMessage("error.cost_center.id.required"));
        Objects.requireNonNull(draft.expenseType(), MessageUtils.getMessage("error.cost_center.type.required"));
        Objects.requireNonNull(draft.amount(), MessageUtils.getMessage("error.cost_center.amount.required"));
        if (draft.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.cost.negative_amount"));
        }

        CostCenter center = costCenterRepository.findById(draft.centerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageUtils.getMessage("error.cost_center.not_found", draft.centerId())));

        BusinessUnit unit = draft.businessUnit() != null
                ? draft.businessUnit()
                : (center.getBusinessUnit() != null ? center.getBusinessUnit() : BusinessUnit.FACTORY);
        if (unit == BusinessUnit.SITE && draft.project() == null) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.expense.site.project.required"));
        }
        String postingPeriod = ExpensePeriods.normalize(
                draft.postingPeriod() != null ? draft.postingPeriod() : YearMonth.now().toString());
        String expensePeriod = draft.expensePeriod() != null
                ? ExpensePeriods.normalize(draft.expensePeriod())
                : ExpensePeriods.expensePeriod(draft.expenseType(), draft.invoiceDate(), draft.entryDate());

        assertPeriodOpen(unit, expensePeriod);

        ExpenseCategory category = draft.expenseCategory() != null
                ? draft.expenseCategory()
                : draft.expenseType().toCategory();

        CostTransaction tx = CostTransaction.builder()
                .costCenter(center)
                .block(draft.block())
                .quarry(draft.quarry())
                .slab(draft.slab())
                .project(draft.project())
                .constructionSite(draft.project())
                .expenseType(draft.expenseType())
                .expenseCategory(category)
                .businessUnit(unit)
                .amount(draft.amount())
                .currency(draft.currency() != null ? draft.currency() : Constants.CURRENCY_TRY)
                .documentNo(draft.documentNo())
                .invoiceDate(draft.invoiceDate())
                .entryDate(draft.entryDate() != null ? draft.entryDate() : LocalDate.now())
                .expensePeriod(expensePeriod)
                .postingPeriod(postingPeriod)
                .machine(draft.machine())
                .factoryOperation(draft.factoryOperation())
                .workshopOperation(draft.workshopOperation())
                .allocationKey(draft.allocationKey())
                .description(draft.description())
                .build();
        return costTransactionRepository.save(tx);
    }

    @Transactional
    public CostPeriodClose closePeriod(BusinessUnit unit, String period, String notes) {
        String normalized = ExpensePeriods.normalize(period);
        if (costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(unit, normalized)) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.expense.period.already_closed", normalized));
        }
        CostPeriodClose close = CostPeriodClose.builder()
                .businessUnit(unit)
                .expensePeriod(normalized)
                .closedAt(LocalDateTime.now())
                .closedBy(SecurityUtils.getCurrentUserLogin().orElse("system@ozerler.com"))
                .notes(notes)
                .build();
        return costPeriodCloseRepository.save(close);
    }

    @Transactional
    public void reopenPeriod(BusinessUnit unit, String period) {
        String normalized = ExpensePeriods.normalize(period);
        CostPeriodClose existing = costPeriodCloseRepository.findByBusinessUnitAndExpensePeriod(unit, normalized)
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageUtils.getMessage("error.expense.period.not_closed", normalized)));
        costPeriodCloseRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public boolean isPeriodClosed(BusinessUnit unit, String period) {
        return costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(unit, ExpensePeriods.normalize(period));
    }

    @Transactional(readOnly = true)
    public List<CostTransaction> listByUnitAndPeriod(BusinessUnit unit, String period) {
        return costTransactionRepository.findByBusinessUnitAndExpensePeriod(unit, ExpensePeriods.normalize(period));
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ExpenseDto> getExpensesPaged(
            int page, int size, String search, String sortField, String sortDir,
            BusinessUnit unit, String period, ExpenseType expenseType,
            Long centerId, Long quarryId, Long projectId) {

        String normSearch = GridPages.normalizeSearch(search);
        String normPeriod = (period != null && !period.isBlank()) ? ExpensePeriods.normalize(period) : null;

        Page<CostTransaction> paged = GridPages.execute(
                page, size, sortField, sortDir, GridPages.EXPENSE_SORTS,
                pageable -> costTransactionRepository.searchExpenses(
                        normSearch, unit, normPeriod, expenseType, centerId, quarryId, projectId, pageable));

        BigDecimal filteredTotal = costTransactionRepository.sumFilteredExpenses(
                normSearch, unit, normPeriod, expenseType, centerId, quarryId, projectId);

        List<ExpenseDto> dtos = paged.getContent().stream()
                .map(tx -> {
                    boolean closed = isPeriodClosed(tx.getBusinessUnit(), tx.getExpensePeriod());
                    return ExpenseDto.fromEntity(tx, closed);
                })
                .toList();

        Map<String, Object> meta = Map.of(
                "totalAmount", filteredTotal != null ? filteredTotal : BigDecimal.ZERO,
                "count", paged.getTotalElements()
        );

        return TabulatorResponse.of(dtos, paged.getTotalPages(), paged.getTotalElements(), meta);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDto getExpenseSummary(String period) {
        String normalized = ExpensePeriods.normalize(period != null && !period.isBlank() ? period : YearMonth.now().toString());
        BigDecimal quarry = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.QUARRY, normalized));
        BigDecimal factory = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.FACTORY, normalized));
        BigDecimal workshop = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.WORKSHOP, normalized));
        BigDecimal site = zero(costTransactionRepository.sumByUnitAndPeriod(BusinessUnit.SITE, normalized));
        BigDecimal total = quarry.add(factory).add(workshop).add(site);
        long count = costTransactionRepository.countByExpensePeriod(normalized);

        return ExpenseSummaryDto.builder()
                .period(normalized)
                .totalExpensesThisMonth(total)
                .quarryTotal(quarry)
                .factoryTotal(factory)
                .workshopTotal(workshop)
                .siteTotal(site)
                .totalCount(count)
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseDto getExpenseDto(Long id) {
        CostTransaction tx = costTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.expense.not_found", id)));
        boolean closed = isPeriodClosed(tx.getBusinessUnit(), tx.getExpensePeriod());
        return ExpenseDto.fromEntity(tx, closed);
    }

    @Transactional
    public CostTransaction updateExpense(Long id, ExpenseDraft draft) {
        Objects.requireNonNull(draft, MessageUtils.getMessage("error.expense.required"));
        Objects.requireNonNull(draft.centerId(), MessageUtils.getMessage("error.cost_center.id.required"));
        Objects.requireNonNull(draft.expenseType(), MessageUtils.getMessage("error.cost_center.type.required"));
        Objects.requireNonNull(draft.amount(), MessageUtils.getMessage("error.cost_center.amount.required"));
        if (draft.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.cost.negative_amount"));
        }

        CostTransaction tx = costTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.expense.not_found", id)));

        assertPeriodOpen(tx.getBusinessUnit(), tx.getExpensePeriod());

        CostCenter center = costCenterRepository.findById(draft.centerId())
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageUtils.getMessage("error.cost_center.not_found", draft.centerId())));

        BusinessUnit unit = draft.businessUnit() != null ? draft.businessUnit() : center.getBusinessUnit();
        if (unit == BusinessUnit.SITE && draft.project() == null) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.expense.site.project.required"));
        }

        String expensePeriod = draft.expensePeriod() != null
                ? ExpensePeriods.normalize(draft.expensePeriod())
                : ExpensePeriods.expensePeriod(draft.expenseType(), draft.invoiceDate(), draft.entryDate());

        assertPeriodOpen(unit, expensePeriod);

        ExpenseCategory category = draft.expenseCategory() != null
                ? draft.expenseCategory()
                : draft.expenseType().toCategory();

        tx.setCostCenter(center);
        tx.setBusinessUnit(unit);
        tx.setExpenseType(draft.expenseType());
        tx.setExpenseCategory(category);
        tx.setAmount(draft.amount());
        if (draft.currency() != null) tx.setCurrency(draft.currency());
        tx.setDocumentNo(draft.documentNo());
        tx.setInvoiceDate(draft.invoiceDate());
        if (draft.entryDate() != null) tx.setEntryDate(draft.entryDate());
        tx.setExpensePeriod(expensePeriod);
        if (draft.postingPeriod() != null) tx.setPostingPeriod(ExpensePeriods.normalize(draft.postingPeriod()));
        tx.setQuarry(draft.quarry());
        tx.setProject(draft.project());
        tx.setConstructionSite(draft.project());
        tx.setDescription(draft.description());

        return costTransactionRepository.save(tx);
    }

    @Transactional
    public void deleteExpense(Long id) {
        CostTransaction tx = costTransactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.expense.not_found", id)));
        assertPeriodOpen(tx.getBusinessUnit(), tx.getExpensePeriod());
        costTransactionRepository.delete(tx);
    }

    private BigDecimal zero(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private void assertPeriodOpen(BusinessUnit unit, String expensePeriod) {
        if (isPeriodClosed(unit, expensePeriod)) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.expense.period.closed", expensePeriod));
        }
    }

    public record ExpenseDraft(
            Long centerId,
            ExpenseType expenseType,
            ExpenseCategory expenseCategory,
            BusinessUnit businessUnit,
            BigDecimal amount,
            String currency,
            String documentNo,
            LocalDate invoiceDate,
            LocalDate entryDate,
            String expensePeriod,
            String postingPeriod,
            com.ozerler.marble.model.Block block,
            com.ozerler.marble.model.Quarry quarry,
            com.ozerler.marble.model.Slab slab,
            com.ozerler.marble.model.Project project,
            com.ozerler.marble.model.Machine machine,
            com.ozerler.marble.model.FactoryOperation factoryOperation,
            com.ozerler.marble.model.WorkshopOperation workshopOperation,
            String allocationKey,
            String description
    ) {
        public static ExpenseDraft of(Long centerId, ExpenseType type, BigDecimal amount,
                                      BusinessUnit unit, String expensePeriod, String postingPeriod,
                                      String description) {
            return new ExpenseDraft(centerId, type, null, unit, amount, Constants.CURRENCY_TRY,
                    null, null, LocalDate.now(), expensePeriod, postingPeriod,
                    null, null, null, null, null, null, null, null, description);
        }
    }
}
