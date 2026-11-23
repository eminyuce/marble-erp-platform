package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.SalesOrder;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    @Query(
            value = "SELECT so FROM SalesOrder so "
                    + "LEFT JOIN FETCH so.customer customer WHERE "
                    + "(:search IS NULL OR LOWER(so.orderNo) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
                    + "LOWER(COALESCE(customer.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), "
                    + "'%')))",
            countQuery = "SELECT COUNT(so) FROM SalesOrder so "
                    + "LEFT JOIN so.customer customer WHERE "
                    + "(:search IS NULL OR LOWER(so.orderNo) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
                    + "LOWER(COALESCE(customer.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), "
                    + "'%')))")
    Page<SalesOrder> searchSalesOrders(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "items"})
    Optional<SalesOrder> findWithDetailsById(@Param("id") Long id);

    @Query(
            "SELECT so FROM SalesOrder so JOIN FETCH so.customer WHERE LOWER(so.orderNo) LIKE LOWER(CONCAT('%', "
                    + ":query, '%'))")
    List<SalesOrder> searchByOrderNo(@Param("query") String query, Pageable pageable);

    boolean existsByCustomerId(Long customerId);
}
