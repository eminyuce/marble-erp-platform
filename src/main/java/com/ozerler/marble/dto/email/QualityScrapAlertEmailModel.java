package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model for 'QUALITY_SCRAP_ALERT' email template.
 * Dispatched when inspection flags hairline crack, color deviation or scrap rate.
 */
@Getter
@Builder
@ToString
public class QualityScrapAlertEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "QUALITY_SCRAP_ALERT";

    private final String blockCode;
    private final String quarryName;
    private final String defectType;
    private final String affectedCount;
    private final String scrapM2;
    private final String inspectorName;
    private final String inspectionDate;
    private final String quarantineLocation;
    private final String actionRequired;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("blockCode", blockCode != null ? blockCode : "");
        vars.put("quarryName", quarryName != null ? quarryName : "");
        vars.put("defectType", defectType != null ? defectType : "");
        vars.put("affectedCount", affectedCount != null ? affectedCount : "0");
        vars.put("scrapM2", scrapM2 != null ? scrapM2 : "0.00");
        vars.put("inspectorName", inspectorName != null ? inspectorName : "");
        vars.put("inspectionDate", inspectionDate != null ? inspectionDate : "");
        vars.put("quarantineLocation", quarantineLocation != null ? quarantineLocation : "");
        vars.put("actionRequired", actionRequired != null ? actionRequired : "");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
        return vars;
    }

    public static QualityScrapAlertEmailModel sample() {
        return QualityScrapAlertEmailModel.builder()
                .blockCode("BLK-2026-088")
                .quarryName("İscehisar Gri Mermer Ocağı - Kademe 3")
                .defectType("Derin Kılcal Damar Çatlağı (Fissure Defect)")
                .affectedCount("16")
                .scrapM2("44.80")
                .inspectorName("Müh. Serkan Varol")
                .inspectionDate("07.10.2026 11:15")
                .quarantineLocation("Kalite Kontrol Karantina Sahası K-2")
                .actionRequired("Epoksi filesi çekilmeden veya blok sağlamlaştırılmadan nihai kesime ve sevkiyata izin verilmeyecektir.")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
