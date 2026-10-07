package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

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
        vars.put("customerName", customerName != null ? customerName : "");
        vars.put("invoiceNumber", invoiceNumber != null ? invoiceNumber : "");
        vars.put("invoiceDate", invoiceDate != null ? invoiceDate : "");
        vars.put("dueDate", dueDate != null ? dueDate : "");
        vars.put("subtotalAmount", subtotalAmount != null ? subtotalAmount : "0.00");
        vars.put("taxAmount", taxAmount != null ? taxAmount : "0.00");
        vars.put("grandTotal", grandTotal != null ? grandTotal : "0.00");
        vars.put("currency", currency != null ? currency : "TL");
        vars.put("bankName", bankName != null ? bankName : "");
        vars.put("bankIban", bankIban != null ? bankIban : "");
        vars.put("pdfDownloadUrl", pdfDownloadUrl != null ? pdfDownloadUrl : "#");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
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
