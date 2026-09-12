package com.ozerler.marble.dto;

import java.math.BigDecimal;

/**
 * Database-level COUNT/SUM projection for child rows of a parent order.
 */
public interface OrderChildAggregate {

    Long getParentId();

    Long getItemCount();

    BigDecimal getTotalArea();

    default int itemCountOrZero() {
        return getItemCount() == null ? 0 : getItemCount().intValue();
    }

    default BigDecimal totalAreaOrZero() {
        return getTotalArea() == null ? BigDecimal.ZERO : getTotalArea();
    }

    static int itemCountOrZero(OrderChildAggregate aggregate) {
        return aggregate == null ? 0 : aggregate.itemCountOrZero();
    }

    static BigDecimal totalAreaOrZero(OrderChildAggregate aggregate) {
        return aggregate == null ? BigDecimal.ZERO : aggregate.totalAreaOrZero();
    }
}
