package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    boolean existsByMovementCode(String movementCode);

    List<StockMovement> findByBlockIdOrderByMovementDateDesc(Long blockId);

    List<StockMovement> findByStockItemIdOrderByMovementDateDesc(Long stockItemId);

    List<StockMovement> findBySlabIdOrderByMovementDateDesc(Long slabId);

    @Query("SELECT m FROM StockMovement m " +
           "LEFT JOIN FETCH m.block " +
           "LEFT JOIN FETCH m.slab " +
           "LEFT JOIN FETCH m.stockItem " +
           "LEFT JOIN FETCH m.fromLocation " +
           "LEFT JOIN FETCH m.toLocation " +
           "LEFT JOIN FETCH m.customer " +
           "WHERE (:movementType IS NULL OR m.movementType = :movementType) " +
           "AND (:sourceDept IS NULL OR m.sourceDepartment = :sourceDept) " +
           "AND (:targetDept IS NULL OR m.targetDepartment = :targetDept) " +
           "AND (:customerId IS NULL OR m.customer.id = :customerId) " +
           "AND (:fromDate IS NULL OR m.movementDate >= :fromDate) " +
           "AND (:toDate IS NULL OR m.movementDate <= :toDate) " +
           "ORDER BY m.movementDate DESC")
    Page<StockMovement> searchMovements(@Param("movementType") StockMovementType movementType,
                                        @Param("sourceDept") BusinessUnit sourceDept,
                                        @Param("targetDept") BusinessUnit targetDept,
                                        @Param("customerId") Long customerId,
                                        @Param("fromDate") LocalDateTime fromDate,
                                        @Param("toDate") LocalDateTime toDate,
                                        Pageable pageable);

    @Query("SELECT m FROM StockMovement m " +
           "WHERE m.sourceDepartment = :sourceDept " +
           "AND m.movementDate >= :since " +
           "ORDER BY m.movementDate DESC")
    List<StockMovement> findRecentBySourceDepartment(@Param("sourceDept") BusinessUnit sourceDept,
                                                     @Param("since") LocalDateTime since);
}
