package com.ozerler.marble.dto.email;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Model for 'ORDER_CONFIRMATION' email template.
 * Dispatched to customers when a sales order is confirmed in the ERP.
 */
@Getter
@Builder
@ToString
public class OrderConfirmationEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "ORDER_CONFIRMATION";

    private final String customerName;
    private final String orderNumber;
    private final String orderDate;
    private final String deliveryDate;
    private final String totalM2;
    private final String itemsSummary;
    private final String totalAmount;
    private final String currency;
    private final String salesRepresentative;
    private final String notes;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("customerName", Objects.requireNonNullElse(customerName, ""));
        vars.put("orderNumber", Objects.requireNonNullElse(orderNumber, ""));
        vars.put("orderDate", Objects.requireNonNullElse(orderDate, ""));
        vars.put("deliveryDate", Objects.requireNonNullElse(deliveryDate, ""));
        vars.put("totalM2", Objects.requireNonNullElse(totalM2, "0.00"));
        vars.put("itemsSummary", Objects.requireNonNullElse(itemsSummary, ""));
        vars.put("totalAmount", Objects.requireNonNullElse(totalAmount, "0.00"));
        vars.put("currency", Objects.requireNonNullElse(currency, "TL"));
        vars.put("salesRepresentative", Objects.requireNonNullElse(salesRepresentative, ""));
        vars.put("notes", Objects.requireNonNullElse(notes, ""));
        vars.put("companyName", Objects.requireNonNullElse(companyName, "Özerler Mermer A.Ş."));
        return vars;
    }

    public static OrderConfirmationEmailModel sample() {
        return OrderConfirmationEmailModel.builder()
                .customerName("Aksoy Mimarlık & İnşaat Ltd. Şti.")
                .orderNumber("SIP-2026-0842")
                .orderDate("07.10.2026")
                .deliveryDate("25.10.2026")
                .totalM2("420.50")
                .itemsSummary("Afyon Beyaz Honlu Plaka (3cm) & Muğla Beyaz Ebatlı (60x60)")
                .totalAmount("845.250,00")
                .currency("TL")
                .salesRepresentative("Mustafa Çelik (+90 532 100 20 30)")
                .notes("İnce cila ve ahşap kasa paletleme talep edilmiştir.")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
