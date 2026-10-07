package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

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
        vars.put("customerName", customerName != null ? customerName : "");
        vars.put("dispatchNumber", dispatchNumber != null ? dispatchNumber : "");
        vars.put("dispatchDate", dispatchDate != null ? dispatchDate : "");
        vars.put("vehiclePlate", vehiclePlate != null ? vehiclePlate : "");
        vars.put("carrierCompany", carrierCompany != null ? carrierCompany : "");
        vars.put("driverName", driverName != null ? driverName : "");
        vars.put("driverPhone", driverPhone != null ? driverPhone : "");
        vars.put("palletCount", palletCount != null ? palletCount : "0");
        vars.put("totalM2", totalM2 != null ? totalM2 : "0.00");
        vars.put("totalWeightKg", totalWeightKg != null ? totalWeightKg : "0");
        vars.put("destinationAddress", destinationAddress != null ? destinationAddress : "");
        vars.put("trackingUrl", trackingUrl != null ? trackingUrl : "#");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
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
