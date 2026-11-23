package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, StockMovementRepositoryCustom {

    boolean existsByMovementCode(String movementCode);

    List<StockMovement> findByBlockIdOrderByMovementDateDesc(Long blockId);

    List<StockMovement> findByStockItemIdOrderByMovementDateDesc(Long stockItemId);

    List<StockMovement> findBySlabIdOrderByMovementDateDesc(Long slabId);

    @Query("SELECT m FROM StockMovement m " +
           "WHERE m.sourceDepartment = :sourceDept " +
           "AND m.movementDate >= :since " +
           "ORDER BY m.movementDate DESC")
    List<StockMovement> findRecentBySourceDepartment(@Param("sourceDept") BusinessUnit sourceDept,
                                                     @Param("since") LocalDateTime since);
}
