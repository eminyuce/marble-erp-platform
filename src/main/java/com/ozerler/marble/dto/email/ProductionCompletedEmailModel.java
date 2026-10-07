package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model for 'PRODUCTION_COMPLETED' email template.
 * Dispatched when gangsaw, wire cutting, slab polishing or bridge saw order completes.
 */
@Getter
@Builder
@ToString
public class ProductionCompletedEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "PRODUCTION_COMPLETED";

    private final String workOrderNumber;
    private final String marbleType;
    private final String sourceBlockNo;
    private final String producedItemType;
    private final String totalProcessedM2;
    private final String scrapM2;
    private final String efficiencyRate;
    private final String operatorName;
    private final String targetStockLocation;
    private final String completionDate;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("workOrderNumber", workOrderNumber != null ? workOrderNumber : "");
        vars.put("marbleType", marbleType != null ? marbleType : "");
        vars.put("sourceBlockNo", sourceBlockNo != null ? sourceBlockNo : "");
        vars.put("producedItemType", producedItemType != null ? producedItemType : "");
        vars.put("totalProcessedM2", totalProcessedM2 != null ? totalProcessedM2 : "0.00");
        vars.put("scrapM2", scrapM2 != null ? scrapM2 : "0.00");
        vars.put("efficiencyRate", efficiencyRate != null ? efficiencyRate : "0");
        vars.put("operatorName", operatorName != null ? operatorName : "");
        vars.put("targetStockLocation", targetStockLocation != null ? targetStockLocation : "");
        vars.put("completionDate", completionDate != null ? completionDate : "");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
        return vars;
    }

    public static ProductionCompletedEmailModel sample() {
        return ProductionCompletedEmailModel.builder()
                .workOrderNumber("IE-2026-092")
                .marbleType("Afyon Şeker Klasik")
                .sourceBlockNo("BLK-2026-042")
                .producedItemType("2 cm Honlu Katrak Plakası")
                .totalProcessedM2("214.80")
                .scrapM2("18.40")
                .efficiencyRate("92.1")
                .operatorName("Kemal Usta (1. Vardiya Katrak)")
                .targetStockLocation("Fabrika A Kapalı Sundurma - Palet Alanı 4")
                .completionDate("07.10.2026 16:45")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
