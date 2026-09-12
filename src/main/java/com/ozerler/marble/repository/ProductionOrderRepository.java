package com.ozerler.marble.repository;

import com.ozerler.marble.dto.OrderChildAggregate;
import com.ozerler.marble.model.ProductionOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    Optional<ProductionOrder> findByOrderNo(String orderNo);

    List<ProductionOrder> findByBlockId(Long blockId);

    @EntityGraph(attributePaths = {"block"})
    @Query("SELECT p FROM ProductionOrder p WHERE " +
           "(:search IS NULL OR LOWER(p.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.machineName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.block.blockCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductionOrder> searchOrders(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM ProductionOrder p WHERE LOWER(p.orderNo) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<ProductionOrder> searchByOrderNo(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(p) FROM ProductionOrder p WHERE p.status = 'IN_PROGRESS'")
    long countActiveOrders();

    @Query("SELECT p.id AS parentId, COUNT(s.id) AS itemCount, COALESCE(SUM(s.surfaceAreaM2), 0) AS totalArea "
            + "FROM ProductionOrder p LEFT JOIN p.slabs s WHERE p.id IN :ids GROUP BY p.id")
    List<OrderChildAggregate> aggregateSlabMetrics(@Param("ids") Collection<Long> ids);
}
