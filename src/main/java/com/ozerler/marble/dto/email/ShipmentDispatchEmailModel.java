package com.ozerler.marble.dto.email;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Model for 'SHIPMENT_DISPATCH' email template.
 * Dispatched to customer / logistics when truck leaves quarry or factory.
 */
@Getter
@Builder
@ToString
public class ShipmentDispatchEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "SHIPMENT_DISPATCH";

    private final String customerName;
    private final String dispatchNumber;
    private final String dispatchDate;
    private final String vehiclePlate;
    private final String carrierCompany;
    private final String driverName;
    private final String driverPhone;
    private final String palletCount;
    private final String totalM2;
    private final String totalWeightKg;
    private final String destinationAddress;
    private final String trackingUrl;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("customerName", Objects.requireNonNullElse(customerName, ""));
        vars.put("dispatchNumber", Objects.requireNonNullElse(dispatchNumber, ""));
        vars.put("dispatchDate", Objects.requireNonNullElse(dispatchDate, ""));
        vars.put("vehiclePlate", Objects.requireNonNullElse(vehiclePlate, ""));
        vars.put("carrierCompany", Objects.requireNonNullElse(carrierCompany, ""));
        vars.put("driverName", Objects.requireNonNullElse(driverName, ""));
        vars.put("driverPhone", Objects.requireNonNullElse(driverPhone, ""));
        vars.put("palletCount", Objects.requireNonNullElse(palletCount, "0"));
        vars.put("totalM2", Objects.requireNonNullElse(totalM2, "0.00"));
        vars.put("totalWeightKg", Objects.requireNonNullElse(totalWeightKg, "0"));
        vars.put("destinationAddress", Objects.requireNonNullElse(destinationAddress, ""));
        vars.put("trackingUrl", Objects.requireNonNullElse(trackingUrl, "#"));
        vars.put("companyName", Objects.requireNonNullElse(companyName, "Özerler Mermer A.Ş."));
        return vars;
    }

    public static ShipmentDispatchEmailModel sample() {
        return ShipmentDispatchEmailModel.builder()
                .customerName("Kaya Yapı Taahhüt A.Ş.")
                .dispatchNumber("IRS-2026-0418")
                .dispatchDate("07.10.2026 14:30")
                .vehiclePlate("03 BK 742")
                .carrierCompany("ÖzAfyon Lojistik Nakliyat")
                .driverName("Salih Demir")
                .driverPhone("+90 544 555 66 77")
                .palletCount("14")
                .totalM2("380.00")
                .totalWeightKg("26.450")
                .destinationAddress("Bodrum Yalıkavak Marina Villa Projesi, Şantiye Alanı No: 12 Muğla")
                .trackingUrl("https://erp.ozerlermermer.com/shipments/IRS-2026-0418")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
