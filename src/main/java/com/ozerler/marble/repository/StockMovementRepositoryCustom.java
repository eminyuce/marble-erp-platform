package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

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
