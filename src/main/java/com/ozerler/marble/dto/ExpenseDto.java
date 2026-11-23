package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.ExpenseType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDto {

    private Long id;
    private Long centerId;
    private String costCenterCode;
    private String costCenterName;
    private BusinessUnit businessUnit;
    private String businessUnitLabel;
    private ExpenseType expenseType;
    private String expenseTypeLabel;
    private ExpenseCategory expenseCategory;
    private String expenseCategoryLabel;
    private Long quarryId;
    private String quarryName;
    private Long projectId;
    private String projectName;
    private String targetName;
    private BigDecimal amount;
    private String currency;
    private String documentNo;
    private LocalDate invoiceDate;
    private LocalDate entryDate;
    private String expensePeriod;
    private String postingPeriod;
    private String description;
    private boolean canEdit;
    private boolean canDelete;

    public static ExpenseDto fromEntity(CostTransaction tx, boolean isClosed) {
        if (tx == null) {
            return null;
        }
        String target = "—";
        if (tx.getQuarry() != null) {
            target = tx.getQuarry().getName();
        } else if (tx.getProject() != null) {
            target = tx.getProject().getName();
        } else if (tx.getBusinessUnit() != null) {
            target = tx.getBusinessUnit().getLabel();
        }

        Long centerIdValue;
        if (tx.getCostCenter() != null) {
            centerIdValue = tx.getCostCenter().getId();
        } else {
            centerIdValue = null;
        }
        String costCenterCodeValue;
        if (tx.getCostCenter() != null) {
            costCenterCodeValue = tx.getCostCenter().getCode();
        } else {
            costCenterCodeValue = "—";
        }
        String costCenterNameValue;
        if (tx.getCostCenter() != null) {
            costCenterNameValue = tx.getCostCenter().getName();
        } else {
            costCenterNameValue = "—";
        }
        String businessUnitLabelValue;
        if (tx.getBusinessUnit() != null) {
            businessUnitLabelValue = tx.getBusinessUnit().getLabel();
        } else {
            businessUnitLabelValue = "—";
        }
        String expenseTypeLabelValue;
        if (tx.getExpenseType() != null) {
            expenseTypeLabelValue = tx.getExpenseType().getLabel();
        } else {
            expenseTypeLabelValue = "—";
        }
        String expenseCategoryLabelValue;
        if (tx.getExpenseCategory() != null) {
            expenseCategoryLabelValue = tx.getExpenseCategory().getLabel();
        } else {
            expenseCategoryLabelValue = "—";
        }
        Long quarryIdValue;
        if (tx.getQuarry() != null) {
            quarryIdValue = tx.getQuarry().getId();
        } else {
            quarryIdValue = null;
        }
        String quarryNameValue;
        if (tx.getQuarry() != null) {
            quarryNameValue = tx.getQuarry().getName();
        } else {
            quarryNameValue = null;
        }
        Long projectIdValue;
        if (tx.getProject() != null) {
            projectIdValue = tx.getProject().getId();
        } else {
            projectIdValue = null;
        }
        String projectNameValue;
        if (tx.getProject() != null) {
            projectNameValue = tx.getProject().getName();
        } else {
            projectNameValue = null;
        }
        return ExpenseDto.builder()
                .id(tx.getId())
                .centerId(centerIdValue)
                .costCenterCode(costCenterCodeValue)
                .costCenterName(costCenterNameValue)
                .businessUnit(tx.getBusinessUnit())
                .businessUnitLabel(businessUnitLabelValue)
                .expenseType(tx.getExpenseType())
                .expenseTypeLabel(expenseTypeLabelValue)
                .expenseCategory(tx.getExpenseCategory())
                .expenseCategoryLabel(expenseCategoryLabelValue)
                .quarryId(quarryIdValue)
                .quarryName(quarryNameValue)
                .projectId(projectIdValue)
                .projectName(projectNameValue)
                .targetName(target)
                .amount(tx.getAmount())
                .currency(Objects.requireNonNullElse(tx.getCurrency(), "TRY"))
                .documentNo(tx.getDocumentNo())
                .invoiceDate(tx.getInvoiceDate())
                .entryDate(tx.getEntryDate())
                .expensePeriod(tx.getExpensePeriod())
                .postingPeriod(tx.getPostingPeriod())
                .description(tx.getDescription())
                .canEdit(!isClosed)
                .canDelete(!isClosed)
                .build();
    }
}
