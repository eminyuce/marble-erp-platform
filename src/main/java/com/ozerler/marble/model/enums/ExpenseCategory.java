package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ExpenseCategory {
    DIESEL("enum.expense_category.diesel"),
    ELECTRICITY("enum.expense_category.electricity"),
    LABOR("enum.expense_category.labor"),
    FIXTURE_CONSUMABLE("enum.expense_category.fixture_consumable"),
    MATERIAL("enum.expense_category.material"),
    TAX("enum.expense_category.tax"),
    CONSUMABLE("enum.expense_category.consumable"),
    TRANSPORTATION("enum.expense_category.transportation"),
    MAINTENANCE("enum.expense_category.maintenance"),
    OTHER("enum.expense_category.other");

    private final String messageKey;

    ExpenseCategory(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
