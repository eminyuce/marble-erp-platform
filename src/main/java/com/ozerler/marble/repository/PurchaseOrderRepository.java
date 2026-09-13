package com.ozerler.marble.repository;

import com.ozerler.marble.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @EntityGraph(attributePaths = {"supplier", "project"})
    @Query("SELECT po FROM PurchaseOrder po WHERE " +
            "(:search IS NULL OR LOWER(po.poNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(po.supplier.companyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<PurchaseOrder> searchPurchaseOrders(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = {"supplier", "project", "items"})
    java.util.Optional<PurchaseOrder> findWithDetailsById(@Param("id") Long id);

    @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.supplier WHERE LOWER(po.poNumber) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<PurchaseOrder> searchByPoNumber(@Param("query") String query, Pageable pageable);
}
