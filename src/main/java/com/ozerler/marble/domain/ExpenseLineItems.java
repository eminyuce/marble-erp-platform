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

    private static final Map<BusinessUnit, List<ExpenseType>> BY_UNIT = Map.of(
            BusinessUnit.QUARRY, List.of(
                    ExpenseType.DIESEL,
                    ExpenseType.ELECTRICITY,
                    ExpenseType.DIRECT_LABOR,
                    ExpenseType.CONSUMABLES,
                    ExpenseType.TAX,
                    ExpenseType.OVERHEAD,
                    ExpenseType.MAINTENANCE,
                    ExpenseType.FIXTURE_CONSUMABLE),
            BusinessUnit.FACTORY, List.of(
                    ExpenseType.DIESEL,
                    ExpenseType.ELECTRICITY,
                    ExpenseType.DIRECT_LABOR,
                    ExpenseType.CONSUMABLES,
                    ExpenseType.TAX,
                    ExpenseType.OVERHEAD,
                    ExpenseType.MAINTENANCE,
                    ExpenseType.FIXTURE_CONSUMABLE),
            BusinessUnit.WORKSHOP, List.of(
                    ExpenseType.DIRECT_LABOR,
                    ExpenseType.DIESEL,
                    ExpenseType.ELECTRICITY,
                    ExpenseType.CONSUMABLES,
                    ExpenseType.TAX,
                    ExpenseType.OVERHEAD,
                    ExpenseType.MAINTENANCE,
                    ExpenseType.FIXTURE_CONSUMABLE),
            BusinessUnit.SITE, List.of(
                    ExpenseType.DIRECT_LABOR,
                    ExpenseType.DIESEL,
                    ExpenseType.ELECTRICITY,
                    ExpenseType.CONSUMABLES,
                    ExpenseType.TAX,
                    ExpenseType.OVERHEAD,
                    ExpenseType.MAINTENANCE,
                    ExpenseType.FIXTURE_CONSUMABLE));

    public static List<ExpenseType> forUnit(BusinessUnit unit) {
        return BY_UNIT.getOrDefault(unit, List.of(ExpenseType.values()));
    }
}
