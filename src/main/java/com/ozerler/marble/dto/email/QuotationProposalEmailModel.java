package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model for 'QUOTATION_PROPOSAL' email template.
 * Dispatched with marble quotation offer, proforma details and terms.
 */
@Getter
@Builder
@ToString
public class QuotationProposalEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "QUOTATION_PROPOSAL";

    private final String customerName;
    private final String quotationNumber;
    private final String validUntilDate;
    private final String projectReference;
    private final String itemsSummary;
    private final String totalPrice;
    private final String currency;
    private final String paymentTerms;
    private final String salesEngineerName;
    private final String salesEngineerPhone;
    private final String portalLink;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("customerName", customerName != null ? customerName : "");
        vars.put("quotationNumber", quotationNumber != null ? quotationNumber : "");
        vars.put("validUntilDate", validUntilDate != null ? validUntilDate : "");
        vars.put("projectReference", projectReference != null ? projectReference : "");
        vars.put("itemsSummary", itemsSummary != null ? itemsSummary : "");
        vars.put("totalPrice", totalPrice != null ? totalPrice : "0.00");
        vars.put("currency", currency != null ? currency : "USD");
        vars.put("paymentTerms", paymentTerms != null ? paymentTerms : "%50 Peşin, %50 Yükleme Öncesi");
        vars.put("salesEngineerName", salesEngineerName != null ? salesEngineerName : "");
        vars.put("salesEngineerPhone", salesEngineerPhone != null ? salesEngineerPhone : "");
        vars.put("portalLink", portalLink != null ? portalLink : "#");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
        return vars;
    }

    public static QuotationProposalEmailModel sample() {
        return QuotationProposalEmailModel.builder()
                .customerName("Al-Mansoor International Trading LLC")
                .quotationNumber("TEK-2026-0312")
                .validUntilDate("22.10.2026")
                .projectReference("Doha Marina Towers Lobby Project")
                .itemsSummary("Afyon Bal Bej Blok (120 Ton) & Silver Traverten Cross-cut Plaka (650 m²)")
                .totalPrice("142.500,00")
                .currency("USD")
                .paymentTerms("%40 Sipariş Onayı, %60 Konşimento / Akreditif Karşılığı")
                .salesEngineerName("Burak Özkan")
                .salesEngineerPhone("+90 533 444 88 99")
                .portalLink("https://erp.ozerlermermer.com/portal/proposals/TEK-2026-0312")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
