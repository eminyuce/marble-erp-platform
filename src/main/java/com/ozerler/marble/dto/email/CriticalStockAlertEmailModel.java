package com.ozerler.marble.dto.email;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Model for 'CRITICAL_STOCK_ALERT' email template.
 * Dispatched to procurement when quarry/factory consumables fall below threshold.
 */
@Getter
@Builder
@ToString
public class CriticalStockAlertEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "CRITICAL_STOCK_ALERT";

    private final String stockCode;
    private final String stockName;
    private final String category;
    private final String currentStock;
    private final String minimumThreshold;
    private final String unit;
    private final String warehouseName;
    private final String suggestedReorderQuantity;
    private final String alertLevel;
    private final String companyName;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("stockCode", Objects.requireNonNullElse(stockCode, ""));
        vars.put("stockName", Objects.requireNonNullElse(stockName, ""));
        vars.put("category", Objects.requireNonNullElse(category, ""));
        vars.put("currentStock", Objects.requireNonNullElse(currentStock, "0"));
        vars.put("minimumThreshold", Objects.requireNonNullElse(minimumThreshold, "0"));
        vars.put("unit", Objects.requireNonNullElse(unit, "adet"));
        vars.put("warehouseName", Objects.requireNonNullElse(warehouseName, ""));
        vars.put("suggestedReorderQuantity", Objects.requireNonNullElse(suggestedReorderQuantity, "0"));
        vars.put("alertLevel", Objects.requireNonNullElse(alertLevel, "KRİTİK STOK UYARISI"));
        vars.put("companyName", Objects.requireNonNullElse(companyName, "Özerler Mermer A.Ş."));
        return vars;
    }

    public static CriticalStockAlertEmailModel sample() {
        return CriticalStockAlertEmailModel.builder()
                .stockCode("SARF-TEL-08")
                .stockName("İscehisar Ocak Elmas Kesme Teli (Ø 11.5 mm Kauçuklu)")
                .category("Ocak Sarf Malzemesi")
                .currentStock("45.00")
                .minimumThreshold("150.00")
                .unit("m.t.")
                .warehouseName("İscehisar Ocak Ana Ambarı")
                .suggestedReorderQuantity("300.00")
                .alertLevel("KRİTİK SEVİYE — ÜRETİM DURMA RİSKİ")
                .companyName("Özerler Mermer A.Ş.")
                .build();
    }
}
