package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

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
        vars.put("customerName", customerName != null ? customerName : "");
        vars.put("orderNumber", orderNumber != null ? orderNumber : "");
        vars.put("orderDate", orderDate != null ? orderDate : "");
        vars.put("deliveryDate", deliveryDate != null ? deliveryDate : "");
        vars.put("totalM2", totalM2 != null ? totalM2 : "0.00");
        vars.put("itemsSummary", itemsSummary != null ? itemsSummary : "");
        vars.put("totalAmount", totalAmount != null ? totalAmount : "0.00");
        vars.put("currency", currency != null ? currency : "TL");
        vars.put("salesRepresentative", salesRepresentative != null ? salesRepresentative : "");
        vars.put("notes", notes != null ? notes : "");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
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
