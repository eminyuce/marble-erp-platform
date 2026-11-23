package com.ozerler.marble.dto.email;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Model for 'INVOICE_ISSUED' email template.
 * Dispatched to customer upon invoice generation and validation.
 */
@Getter
@Builder
@ToString
public class InvoiceIssuedEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "INVOICE_ISSUED";

    private final String customerName;
    private final String invoiceNumber;
    private final String invoiceDate;
    private final String dueDate;
    private final String subtotalAmount;
    private final String taxAmount;
    private final String grandTotal;
    private final String currency;
    private final String bankName;
    private final String bankIban;
    private final String pdfDownloadUrl;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("customerName", Objects.requireNonNullElse(customerName, ""));
        vars.put("invoiceNumber", Objects.requireNonNullElse(invoiceNumber, ""));
        vars.put("invoiceDate", Objects.requireNonNullElse(invoiceDate, ""));
        vars.put("dueDate", Objects.requireNonNullElse(dueDate, ""));
        vars.put("subtotalAmount", Objects.requireNonNullElse(subtotalAmount, "0.00"));
        vars.put("taxAmount", Objects.requireNonNullElse(taxAmount, "0.00"));
        vars.put("grandTotal", Objects.requireNonNullElse(grandTotal, "0.00"));
        vars.put("currency", Objects.requireNonNullElse(currency, "TL"));
        vars.put("bankName", Objects.requireNonNullElse(bankName, ""));
        vars.put("bankIban", Objects.requireNonNullElse(bankIban, ""));
        vars.put("pdfDownloadUrl", Objects.requireNonNullElse(pdfDownloadUrl, "#"));
        vars.put("companyName", Objects.requireNonNullElse(companyName, "Özerler Mermer A.Ş."));
        return vars;
    }

    public static InvoiceIssuedEmailModel sample() {
        return InvoiceIssuedEmailModel.builder()
                .customerName("Ege Doğaltaş İthalat İhracat A.Ş.")
                .invoiceNumber("OZR202600000128")
                .invoiceDate("07.10.2026")
                .dueDate("06.11.2026")
                .subtotalAmount("650.000,00")
                .taxAmount("130.000,00")
                .grandTotal("780.000,00")
                .currency("TL")
                .bankName("Ziraat Bankası — Afyon Şubesi")
                .bankIban("TR88 0001 0002 0003 0004 0005 01")
                .pdfDownloadUrl("https://erp.ozerlermermer.com/invoices/OZR202600000128/pdf")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
