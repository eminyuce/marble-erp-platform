package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ReservationStatus {
    ACTIVE("enum.reservation_status.active"),
    FULFILLED("enum.reservation_status.fulfilled"),
    CANCELLED("enum.reservation_status.cancelled");

    private final String messageKey;

    ReservationStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public static String labelOf(String code) {
        return NamedEnumLabels.labelOf(ReservationStatus.class, code, status -> status.getLabel());
    }
}
