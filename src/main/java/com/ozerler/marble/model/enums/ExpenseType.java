package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ExpenseType {
    DIRECT_RAW("enum.expense_type.direct_raw"),
    DIRECT_LABOR("enum.expense_type.direct_labor"),
    ELECTRICITY("enum.expense_type.electricity"),
    CONSUMABLES("enum.expense_type.consumables"),
    LOGISTICS("enum.expense_type.logistics"),
    DEPRECIATION("enum.expense_type.depreciation"),
    OVERHEAD("enum.expense_type.overhead");

    private final String messageKey;

    ExpenseType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
