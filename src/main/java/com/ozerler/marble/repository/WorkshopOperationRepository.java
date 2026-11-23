package com.ozerler.marble.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.WorkshopOperation;
import com.ozerler.marble.model.enums.OperationStatus;

@Repository
public interface WorkshopOperationRepository extends JpaRepository<WorkshopOperation, Long> {
    List<WorkshopOperation> findByCutOrderIdOrderByIdAsc(Long cutOrderId);

    @Query("SELECT COALESCE(SUM(o.outputAreaM2), 0) FROM WorkshopOperation o WHERE o.status = :status")
    BigDecimal sumOutputAreaM2ByStatus(@Param("status") OperationStatus status);
}
