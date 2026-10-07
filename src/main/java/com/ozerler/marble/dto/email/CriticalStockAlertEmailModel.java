package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

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
        vars.put("stockCode", stockCode != null ? stockCode : "");
        vars.put("stockName", stockName != null ? stockName : "");
        vars.put("category", category != null ? category : "");
        vars.put("currentStock", currentStock != null ? currentStock : "0");
        vars.put("minimumThreshold", minimumThreshold != null ? minimumThreshold : "0");
        vars.put("unit", unit != null ? unit : "adet");
        vars.put("warehouseName", warehouseName != null ? warehouseName : "");
        vars.put("suggestedReorderQuantity", suggestedReorderQuantity != null ? suggestedReorderQuantity : "0");
        vars.put("alertLevel", alertLevel != null ? alertLevel : "KRİTİK STOK UYARISI");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
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
