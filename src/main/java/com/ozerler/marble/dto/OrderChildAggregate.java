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
        if (getItemCount() == null) {
            return 0;
        }
        return getItemCount().intValue();
    }

    default BigDecimal totalAreaOrZero() {
        if (getTotalArea() == null) {
            return BigDecimal.ZERO;
        }
        return getTotalArea();
    }

    static int itemCountOrZero(OrderChildAggregate aggregate) {
        if (aggregate == null) {
            return 0;
        }
        return aggregate.itemCountOrZero();
    }

    static BigDecimal totalAreaOrZero(OrderChildAggregate aggregate) {
        if (aggregate == null) {
            return BigDecimal.ZERO;
        }
        return aggregate.totalAreaOrZero();
    }
}
