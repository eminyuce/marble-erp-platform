package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

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
        vars.put("customerCode", customerCode != null ? customerCode : "");
        vars.put("customerName", customerName != null ? customerName : "");
        vars.put("statementPeriod", statementPeriod != null ? statementPeriod : "");
        vars.put("totalDebit", totalDebit != null ? totalDebit : "0.00");
        vars.put("totalCredit", totalCredit != null ? totalCredit : "0.00");
        vars.put("currentBalance", currentBalance != null ? currentBalance : "0.00");
        vars.put("currency", currency != null ? currency : "TL");
        vars.put("overdueAmount", overdueAmount != null ? overdueAmount : "0.00");
        vars.put("financeContactEmail", financeContactEmail != null ? financeContactEmail : "finans@ozerlermermer.com");
        vars.put("confirmationDueDate", confirmationDueDate != null ? confirmationDueDate : "");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
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
