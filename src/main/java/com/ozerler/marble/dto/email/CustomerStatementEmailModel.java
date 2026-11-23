package com.ozerler.marble.dto.email;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Model for 'CUSTOMER_STATEMENT' email template.
 * Dispatched monthly or quarterly for ledger reconciliation and aging balance overview.
 */
@Getter
@Builder
@ToString
public class CustomerStatementEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "CUSTOMER_STATEMENT";

    private final String customerCode;
    private final String customerName;
    private final String statementPeriod;
    private final String totalDebit;
    private final String totalCredit;
    private final String currentBalance;
    private final String currency;
    private final String overdueAmount;
    private final String financeContactEmail;
    private final String confirmationDueDate;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("customerCode", Objects.requireNonNullElse(customerCode, ""));
        vars.put("customerName", Objects.requireNonNullElse(customerName, ""));
        vars.put("statementPeriod", Objects.requireNonNullElse(statementPeriod, ""));
        vars.put("totalDebit", Objects.requireNonNullElse(totalDebit, "0.00"));
        vars.put("totalCredit", Objects.requireNonNullElse(totalCredit, "0.00"));
        vars.put("currentBalance", Objects.requireNonNullElse(currentBalance, "0.00"));
        vars.put("currency", Objects.requireNonNullElse(currency, "TL"));
        vars.put("overdueAmount", Objects.requireNonNullElse(overdueAmount, "0.00"));
        vars.put("financeContactEmail", Objects.requireNonNullElse(financeContactEmail, "finans@ozerlermermer.com"));
        vars.put("confirmationDueDate", Objects.requireNonNullElse(confirmationDueDate, ""));
        vars.put("companyName", Objects.requireNonNullElse(companyName, "Özerler Mermer A.Ş."));
        return vars;
    }

    public static CustomerStatementEmailModel sample() {
        return CustomerStatementEmailModel.builder()
                .customerCode("CARI-2024-0012")
                .customerName("Taş Yapı Mimari İnşaat San. ve Tic. A.Ş.")
                .statementPeriod("Eylül 2026")
                .totalDebit("1.850.400,00")
                .totalCredit("1.320.000,00")
                .currentBalance("530.400,00 (Borçlu)")
                .currency("TL")
                .overdueAmount("145.000,00")
                .financeContactEmail("mutabakat@ozerlermermer.com")
                .confirmationDueDate("15.10.2026")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
