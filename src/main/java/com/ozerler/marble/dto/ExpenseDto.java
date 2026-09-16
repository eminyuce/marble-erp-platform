package com.ozerler.marble.dto;

import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.ExpenseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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

        return ExpenseDto.builder()
                .id(tx.getId())
                .centerId(tx.getCostCenter() != null ? tx.getCostCenter().getId() : null)
                .costCenterCode(tx.getCostCenter() != null ? tx.getCostCenter().getCode() : "—")
                .costCenterName(tx.getCostCenter() != null ? tx.getCostCenter().getName() : "—")
                .businessUnit(tx.getBusinessUnit())
                .businessUnitLabel(tx.getBusinessUnit() != null ? tx.getBusinessUnit().getLabel() : "—")
                .expenseType(tx.getExpenseType())
                .expenseTypeLabel(tx.getExpenseType() != null ? tx.getExpenseType().getLabel() : "—")
                .expenseCategory(tx.getExpenseCategory())
                .expenseCategoryLabel(tx.getExpenseCategory() != null ? tx.getExpenseCategory().getLabel() : "—")
                .quarryId(tx.getQuarry() != null ? tx.getQuarry().getId() : null)
                .quarryName(tx.getQuarry() != null ? tx.getQuarry().getName() : null)
                .projectId(tx.getProject() != null ? tx.getProject().getId() : null)
                .projectName(tx.getProject() != null ? tx.getProject().getName() : null)
                .targetName(target)
                .amount(tx.getAmount())
                .currency(tx.getCurrency() != null ? tx.getCurrency() : "TRY")
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
