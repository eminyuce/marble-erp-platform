package com.ozerler.marble.domain;

import java.util.List;
import java.util.Map;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;

/**
 * Gider kalemleri — birim bazında kullanılabilir masraf türleri.
 */
public final class ExpenseLineItems {

    private static final List<ExpenseType> STANDARD_TYPES = List.of(
            ExpenseType.DIESEL,
            ExpenseType.ELECTRICITY,
            ExpenseType.DIRECT_LABOR,
            ExpenseType.SSK,
            ExpenseType.CONSUMABLES,
            ExpenseType.TAX,
            ExpenseType.STOPAJ,
            ExpenseType.BANK_FINANCE,
            ExpenseType.OFFICIAL_FEES,
            ExpenseType.OVERHEAD,
            ExpenseType.MAINTENANCE,
            ExpenseType.FIXTURE_CONSUMABLE);

    private static final List<ExpenseType> LABOR_FIRST_TYPES = List.of(
            ExpenseType.DIRECT_LABOR,
            ExpenseType.SSK,
            ExpenseType.DIESEL,
            ExpenseType.ELECTRICITY,
            ExpenseType.CONSUMABLES,
            ExpenseType.TAX,
            ExpenseType.STOPAJ,
            ExpenseType.BANK_FINANCE,
            ExpenseType.OFFICIAL_FEES,
            ExpenseType.OVERHEAD,
            ExpenseType.MAINTENANCE,
            ExpenseType.FIXTURE_CONSUMABLE);

    private static final Map<BusinessUnit, List<ExpenseType>> BY_UNIT = Map.of(
            BusinessUnit.QUARRY, STANDARD_TYPES,
            BusinessUnit.FACTORY, STANDARD_TYPES,
            BusinessUnit.WORKSHOP, LABOR_FIRST_TYPES,
            BusinessUnit.SITE, LABOR_FIRST_TYPES);

    private ExpenseLineItems() {
    }

    public static List<ExpenseType> forUnit(BusinessUnit unit) {
        return BY_UNIT.getOrDefault(unit, List.of(ExpenseType.values()));
    }
}
