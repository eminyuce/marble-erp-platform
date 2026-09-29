package com.ozerler.marble.repository;

import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperationWorkOrderRepository extends JpaRepository<OperationWorkOrder, Long> {

    Optional<OperationWorkOrder> findByOrderNo(String orderNo);

    boolean existsByOrderNo(String orderNo);

    @Query("SELECT o FROM OperationWorkOrder o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.sourceStockItem " +
           "LEFT JOIN FETCH o.sourceSlab " +
           "LEFT JOIN FETCH o.sourceBlock " +
           "WHERE (:department IS NULL OR o.department = :department) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:customerId IS NULL OR o.customer.id = :customerId) " +
           "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
           "AND (:search IS NULL OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.stoneType) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(o.responsiblePerson) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY o.orderDate DESC, o.id DESC")
    Page<OperationWorkOrder> searchOrders(@Param("department") BusinessUnit department,
                                         @Param("status") OperationWorkOrderStatus status,
                                         @Param("customerId") Long customerId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("search") String search,
                                         Pageable pageable);

    long countByStatus(OperationWorkOrderStatus status);

    long countByStatusIn(List<OperationWorkOrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM OperationWorkOrder o WHERE o.status = :status AND (:department IS NULL OR o.department = :department)")
    long countByStatusAndDepartment(@Param("status") OperationWorkOrderStatus status,
                                   @Param("department") BusinessUnit department);
}
