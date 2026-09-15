package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum WorkshopWorkPurpose {
    DIRECT_SALE("enum.workshop_work_purpose.direct_sale"),
    AFTER_PROCESSING_SALE("enum.workshop_work_purpose.after_processing_sale"),
    CONSTRUCTION_SITE("enum.workshop_work_purpose.construction_site");

    private final String messageKey;

    WorkshopWorkPurpose(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
