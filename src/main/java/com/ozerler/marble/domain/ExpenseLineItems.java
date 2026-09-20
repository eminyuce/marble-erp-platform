package com.ozerler.marble.domain;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;

import java.util.List;
import java.util.Map;

/**
 * Gider kalemleri — birim bazında kullanılabilir masraf türleri.
 */
public final class ExpenseLineItems {

    private ExpenseLineItems() {
    }

    private static final List<ExpenseType> STANDARD_TYPES = List.of(
            ExpenseType.DIESEL,
            ExpenseType.ELECTRICITY,
            ExpenseType.DIRECT_LABOR,
            ExpenseType.CONSUMABLES,
            ExpenseType.TAX,
            ExpenseType.OVERHEAD,
            ExpenseType.MAINTENANCE,
            ExpenseType.FIXTURE_CONSUMABLE);

    private static final List<ExpenseType> LABOR_FIRST_TYPES = List.of(
            ExpenseType.DIRECT_LABOR,
            ExpenseType.DIESEL,
            ExpenseType.ELECTRICITY,
            ExpenseType.CONSUMABLES,
            ExpenseType.TAX,
            ExpenseType.OVERHEAD,
            ExpenseType.MAINTENANCE,
            ExpenseType.FIXTURE_CONSUMABLE);

    private static final Map<BusinessUnit, List<ExpenseType>> BY_UNIT = Map.of(
            BusinessUnit.QUARRY, STANDARD_TYPES,
            BusinessUnit.FACTORY, STANDARD_TYPES,
            BusinessUnit.WORKSHOP, LABOR_FIRST_TYPES,
            BusinessUnit.SITE, LABOR_FIRST_TYPES);

    public static List<ExpenseType> forUnit(BusinessUnit unit) {
        return BY_UNIT.getOrDefault(unit, List.of(ExpenseType.values()));
    }
}
