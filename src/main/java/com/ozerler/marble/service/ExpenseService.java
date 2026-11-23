package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.ExpensePeriods;
import com.ozerler.marble.dto.ExpenseDto;
import com.ozerler.marble.dto.ExpenseSummaryDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostPeriodClose;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.WorkshopOperation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostPeriodCloseRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.security.SecurityUtils;
import com.ozerler.marble.util.GridPages;
import com.ozerler.marble.util.MessageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final CostTransactionRepository costTransactionRepository;
    private final CostCenterRepository costCenterRepository;
    private final CostPeriodCloseRepository costPeriodCloseRepository;
    private final ProjectRepository projectRepository;
    private final QuarryRepository quarryRepository;

    @Transactional
    public CostTransaction recordExpense(ExpenseDraft draft) {
        Objects.requireNonNull(draft, MessageUtils.getMessage("error.expense.required"));
        Objects.requireNonNull(draft.expenseType(), MessageUtils.getMessage("error.cost_center.type.required"));
        Objects.requireNonNull(draft.amount(), MessageUtils.getMessage("error.cost_center.amount.required"));
        if (draft.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.cost.negative_amount"));
        }

        BusinessUnit unit;
        if (draft.businessUnit() != null) {
            unit = draft.businessUnit();
        } else {
            unit = BusinessUnit.FACTORY;
        }

        CostCenter center;
        if (draft.centerId() != null) {
            center = costCenterRepository.findById(draft.centerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            MessageUtils.getMessage("error.cost_center.not_found", draft.centerId())));
        } else {
            center = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(unit)
                    .or(() -> BoundedReads.all(costCenterRepository).stream().findFirst())
                    .orElseThrow(() -> new IllegalArgumentException("Masraf merkezi tanımlı değil."));
        }
        String normalize;
        if (draft.postingPeriod() != null) {
            normalize = draft.postingPeriod();
        } else {
            normalize = YearMonth.now().toString();
        }
        String postingPeriod = ExpensePeriods.normalize(
                normalize);
        String expensePeriod;
        if (draft.expensePeriod() != null) {
            expensePeriod = ExpensePeriods.normalize(draft.expensePeriod());
        } else {
            expensePeriod = ExpensePeriods.expensePeriod(draft.expenseType(), draft.invoiceDate(), draft.entryDate());
        }

        assertPeriodOpen(unit, expensePeriod);

        ExpenseCategory category;
        if (draft.expenseCategory() != null) {
            category = draft.expenseCategory();
        } else {
            category = draft.expenseType().toCategory();
        }

        String currency;
        if (draft.currency() != null) {
            currency = draft.currency();
        } else {
            currency = Constants.CURRENCY_TRY;
        }
        LocalDate entryDate;
        if (draft.entryDate() != null) {
            entryDate = draft.entryDate();
        } else {
            entryDate = LocalDate.now();
        }
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
                .currency(currency)
                .documentNo(draft.documentNo())
                .invoiceDate(draft.invoiceDate())
                .entryDate(entryDate)
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
    public CostTransaction recordForScreen(ExpenseDraft draft, Long projectId, Long quarryId) {
        return recordExpense(link(draft, projectId, quarryId));
    }

    @Transactional
    public CostTransaction updateForScreen(Long id, ExpenseDraft draft, Long projectId, Long quarryId) {
        return updateExpense(id, link(draft, projectId, quarryId));
    }

    private ExpenseDraft link(ExpenseDraft draft, Long projectId, Long quarryId) {
        BusinessUnit unit = draft.businessUnit();
        Project project;
        if (unit == BusinessUnit.SITE && projectId != null) {
            project = projectRepository.findById(projectId).orElseThrow(() -> new IllegalArgumentException(
                            MessageUtils.getMessage("error.project.not_found", projectId)));
        } else {
            project = null;
        }
        Quarry quarry;
        if (unit == BusinessUnit.QUARRY && quarryId != null) {
            quarry = quarryRepository.findById(quarryId).orElseThrow(() -> new IllegalArgumentException(
                            MessageUtils.getMessage("error.quarry.not_found", quarryId)));
        } else {
            quarry = null;
        }
        return new ExpenseDraft(
                draft.centerId(), draft.expenseType(), draft.expenseCategory(), draft.businessUnit(),
                draft.amount(), draft.currency(), draft.documentNo(), draft.invoiceDate(), draft.entryDate(),
                draft.expensePeriod(), draft.postingPeriod(), draft.block(), quarry, draft.slab(), project,
                draft.machine(), draft.factoryOperation(), draft.workshopOperation(), draft.allocationKey(),
                draft.description());
    }

    @Transactional
    public CostPeriodClose closePeriod(BusinessUnit unit, String period, String notes) {
        String normalized = ExpensePeriods.normalize(period);
        if (costPeriodCloseRepository.existsByBusinessUnitAndExpensePeriod(unit, normalized)) {
            throw new IllegalArgumentException(
                    MessageUtils.getMessage("error.expense.period.already_closed", normalized));
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
        String normPeriod;
        if (period != null && !period.isBlank()) {
            normPeriod = ExpensePeriods.normalize(period);
        } else {
            normPeriod = null;
        }

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
                "totalAmount", Objects.requireNonNullElse(filteredTotal, BigDecimal.ZERO),
                "count", paged.getTotalElements()
        );

        return TabulatorResponse.of(dtos, paged.getTotalPages(), paged.getTotalElements(), meta);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryDto getExpenseSummary(String period) {
        String normalize;
        if (period != null && !period.isBlank()) {
            normalize = period;
        } else {
            normalize = YearMonth.now().toString();
        }
        String normalized = ExpensePeriods.normalize(normalize);
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
        CostTransaction tx =
                costTransactionRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                MessageUtils.getMessage(
                                                        "error.expense.not_found", id)));
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

        CostTransaction tx =
                costTransactionRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                MessageUtils.getMessage(
                                                        "error.expense.not_found", id)));

        assertPeriodOpen(tx.getBusinessUnit(), tx.getExpensePeriod());

        BusinessUnit unit;
        if (draft.businessUnit() != null) {
            unit = draft.businessUnit();
        } else {
            if (tx.getBusinessUnit() != null) {
                unit = tx.getBusinessUnit();
            } else {
                unit = BusinessUnit.FACTORY;
            }
        }

        CostCenter center;
        if (draft.centerId() != null) {
            center = costCenterRepository.findById(draft.centerId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            MessageUtils.getMessage("error.cost_center.not_found", draft.centerId())));
        } else if (tx.getCostCenter() != null) {
            center = tx.getCostCenter();
        } else {
            center = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(unit)
                    .or(() -> BoundedReads.all(costCenterRepository).stream().findFirst())
                    .orElseThrow(() -> new IllegalArgumentException("Masraf merkezi tanımlı değil."));
        }

        String expensePeriod;
        if (draft.expensePeriod() != null) {
            expensePeriod = ExpensePeriods.normalize(draft.expensePeriod());
        } else {
            expensePeriod = ExpensePeriods.expensePeriod(draft.expenseType(), draft.invoiceDate(), draft.entryDate());
        }

        assertPeriodOpen(unit, expensePeriod);

        ExpenseCategory category;
        if (draft.expenseCategory() != null) {
            category = draft.expenseCategory();
        } else {
            category = draft.expenseType().toCategory();
        }

        tx.setCostCenter(center);
        tx.setBusinessUnit(unit);
        tx.setExpenseType(draft.expenseType());
        tx.setExpenseCategory(category);
        tx.setAmount(draft.amount());
        if (draft.currency() != null) {
            tx.setCurrency(draft.currency());
        }
        tx.setDocumentNo(draft.documentNo());
        tx.setInvoiceDate(draft.invoiceDate());
        if (draft.entryDate() != null) {
            tx.setEntryDate(draft.entryDate());
        }
        tx.setExpensePeriod(expensePeriod);
        if (draft.postingPeriod() != null) {
            tx.setPostingPeriod(ExpensePeriods.normalize(draft.postingPeriod()));
        }
        tx.setQuarry(draft.quarry());
        tx.setProject(draft.project());
        tx.setConstructionSite(draft.project());
        tx.setDescription(draft.description());

        return costTransactionRepository.save(tx);
    }

    @Transactional
    public void deleteExpense(Long id) {
        CostTransaction tx =
                costTransactionRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                MessageUtils.getMessage(
                                                        "error.expense.not_found", id)));
        assertPeriodOpen(tx.getBusinessUnit(), tx.getExpensePeriod());
        costTransactionRepository.delete(tx);
    }

    private BigDecimal zero(BigDecimal val) {
        if (val != null) {
            return val;
        }
        return BigDecimal.ZERO;
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
            Block block,
            Quarry quarry,
            Slab slab,
            Project project,
            Machine machine,
            FactoryOperation factoryOperation,
            WorkshopOperation workshopOperation,
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
