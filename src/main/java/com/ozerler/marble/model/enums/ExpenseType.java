package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ExpenseType {
    DIRECT_RAW("enum.expense_type.direct_raw"),
    DIRECT_LABOR("enum.expense_type.direct_labor"),
    ELECTRICITY("enum.expense_type.electricity"),
    CONSUMABLES("enum.expense_type.consumables"),
    LOGISTICS("enum.expense_type.logistics"),
    DEPRECIATION("enum.expense_type.depreciation"),
    OVERHEAD("enum.expense_type.overhead"),
    DIESEL("enum.expense_type.diesel"),
    TAX("enum.expense_type.tax"),
    FIXTURE_CONSUMABLE("enum.expense_type.fixture_consumable"),
    MATERIAL("enum.expense_type.material"),
    TRANSPORTATION("enum.expense_type.transportation");

    private final String messageKey;

    ExpenseType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public ExpenseCategory toCategory() {
        return switch (this) {
            case DIESEL -> ExpenseCategory.DIESEL;
            case ELECTRICITY -> ExpenseCategory.ELECTRICITY;
            case DIRECT_LABOR -> ExpenseCategory.LABOR;
            case FIXTURE_CONSUMABLE, CONSUMABLES -> ExpenseCategory.FIXTURE_CONSUMABLE;
            case TAX -> ExpenseCategory.TAX;
            case MATERIAL, DIRECT_RAW -> ExpenseCategory.MATERIAL;
            case TRANSPORTATION, LOGISTICS -> ExpenseCategory.TRANSPORTATION;
            case DEPRECIATION, OVERHEAD -> ExpenseCategory.OTHER;
        };
    }
}
