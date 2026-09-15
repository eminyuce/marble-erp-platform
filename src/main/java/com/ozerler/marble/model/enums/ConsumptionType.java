package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ConsumptionType {
    STONE("enum.consumption_type.stone"),
    ADHESIVE("enum.consumption_type.adhesive"),
    GROUT("enum.consumption_type.grout"),
    ANCHORAGE("enum.consumption_type.anchorage"),
    MECHANICAL_ANCHOR("enum.consumption_type.mechanical_anchor"),
    CHEMICAL("enum.consumption_type.chemical"),
    SEALANT("enum.consumption_type.sealant"),
    LABOR("enum.consumption_type.labor"),
    SAND("enum.consumption_type.sand"),
    CEMENT("enum.consumption_type.cement"),
    TAX("enum.consumption_type.tax"),
    TRANSPORTATION("enum.consumption_type.transportation"),
    MISCELLANEOUS("enum.consumption_type.miscellaneous");

    private final String messageKey;

    ConsumptionType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public ExpenseCategory toExpenseCategory() {
        return switch (this) {
            case STONE -> ExpenseCategory.MATERIAL;
            case LABOR -> ExpenseCategory.LABOR;
            case TAX -> ExpenseCategory.TAX;
            case TRANSPORTATION -> ExpenseCategory.TRANSPORTATION;
            case SAND, CEMENT, ADHESIVE, GROUT, ANCHORAGE, MECHANICAL_ANCHOR, CHEMICAL, SEALANT ->
                    ExpenseCategory.CONSUMABLE;
            case MISCELLANEOUS -> ExpenseCategory.OTHER;
        };
    }
}
