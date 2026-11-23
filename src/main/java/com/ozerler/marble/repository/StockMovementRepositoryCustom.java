package com.ozerler.marble.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;

public interface StockMovementRepositoryCustom {

    /**
     * Nullable filters are omitted instead of compared with an untyped null.
     * PostgreSQL rejects {@code (:p is null or col = :p)} with SQLState 42P18.
     */
    Page<StockMovement> searchMovements(StockMovementType movementType,
                                        BusinessUnit sourceDept,
                                        BusinessUnit targetDept,
                                        Long customerId,
                                        LocalDateTime fromDate,
                                        LocalDateTime toDate,
                                        Pageable pageable);
}
