package com.ozerler.marble.repository;

import com.ozerler.marble.model.ProductionOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    Optional<ProductionOrder> findByOrderNo(String orderNo);

    List<ProductionOrder> findByBlockId(Long blockId);

    @Query("SELECT p FROM ProductionOrder p WHERE " +
           "(:search IS NULL OR LOWER(p.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.machineName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.block.blockCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ProductionOrder> searchOrders(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(p) FROM ProductionOrder p WHERE p.status = 'IN_PROGRESS'")
    long countActiveOrders();
}
