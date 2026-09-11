package com.ozerler.marble.repository;

import com.ozerler.marble.model.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    @Query("SELECT so FROM SalesOrder so JOIN FETCH so.customer WHERE " +
           "(:search IS NULL OR LOWER(so.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(so.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<SalesOrder> searchSalesOrders(@Param("search") String search, Pageable pageable);

    @Query("SELECT so FROM SalesOrder so JOIN FETCH so.customer WHERE LOWER(so.orderNo) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<SalesOrder> searchByOrderNo(@Param("query") String query, Pageable pageable);
}
