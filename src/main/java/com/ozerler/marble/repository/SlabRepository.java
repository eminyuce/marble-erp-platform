package com.ozerler.marble.repository;

import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.SlabStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlabRepository extends JpaRepository<Slab, Long> {

    Optional<Slab> findBySlabCode(String slabCode);

    @EntityGraph(attributePaths = {"block", "block.quarry", "productionOrder", "pallet"})
    Optional<Slab> findWithDetailsById(Long id);

    List<Slab> findByBlockId(Long blockId);

    List<Slab> findByProductionOrderId(Long orderId);

    List<Slab> findByStatus(SlabStatus status);

    @Query("SELECT s FROM Slab s JOIN FETCH s.block b JOIN FETCH b.quarry WHERE s.id = :id")
    Optional<Slab> findByIdWithBlockAndQuarry(@Param("id") Long id);

    @EntityGraph(attributePaths = {"block"})
    @Query("SELECT s FROM Slab s ORDER BY s.id DESC")
    List<Slab> findAllWithBlock();

    @EntityGraph(attributePaths = {"block", "productionOrder", "pallet"})
    @Query("SELECT s FROM Slab s WHERE " +
            "(:search IS NULL OR LOWER(s.slabCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(s.block.blockCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(s.block.stoneType) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Slab> searchSlabs(@Param("search") String search, Pageable pageable);

    @Query("SELECT s FROM Slab s WHERE LOWER(s.slabCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Slab> searchBySlabCode(@Param("query") String query, Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.surfaceAreaM2), 0) FROM Slab s WHERE s.status = :status")
    BigDecimal getTotalAreaByStatus(@Param("status") SlabStatus status);

    @Query("SELECT COALESCE(SUM(s.surfaceAreaM2), 0) FROM Slab s WHERE s.status IN ('AVAILABLE', 'RESERVED')")
    BigDecimal getTotalInventoryAreaM2();

    long countByStatus(SlabStatus status);
}
