package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;
import lombok.Getter;

public enum ScrapReasonCode {
    FR_01("FR-01"),
    FR_02("FR-02"),
    FR_03("FR-03"),
    FR_04("FR-04"),
    FR_05("FR-05"),
    FR_06("FR-06"),
    FR_07("FR-07"),
    FR_08("FR-08"),
    FR_09("FR-09"),
    FR_10("FR-10");

    @Getter
    private final String code;

    ScrapReasonCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return MessageUtils.getMessage("enum.scrap_reason." + name().toLowerCase() + ".title");
    }

    public String getDescription() {
        return MessageUtils.getMessage("enum.scrap_reason." + name().toLowerCase() + ".desc");
    }

    public String getCostImpact() {
        return MessageUtils.getMessage("enum.scrap_reason." + name().toLowerCase() + ".cost");
    }

    public static ScrapReasonCode fromCode(String code) {
        if (code == null) return null;
        for (ScrapReasonCode r : values()) {
            if (r.getCode().equalsIgnoreCase(code) || r.name().equalsIgnoreCase(code)) {
                return r;
            }
        }
        return null;
    }
}
