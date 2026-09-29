package com.ozerler.marble.repository;

import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNo(String invoiceNo);

    boolean existsByInvoiceNo(String invoiceNo);

    @Query("SELECT i FROM Invoice i " +
           "LEFT JOIN FETCH i.customer " +
           "LEFT JOIN FETCH i.supplier " +
           "WHERE (:invoiceType IS NULL OR i.invoiceType = :invoiceType) " +
           "AND (:department IS NULL OR i.department = :department) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:customerId IS NULL OR i.customer.id = :customerId) " +
           "AND (:startDate IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (:endDate IS NULL OR i.invoiceDate <= :endDate) " +
           "AND (:search IS NULL OR :search = '' " +
           "OR LOWER(i.invoiceNo) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(COALESCE(i.partyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(COALESCE(i.customer.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "ORDER BY i.invoiceDate DESC, i.id DESC")
    Page<Invoice> searchInvoices(@Param("invoiceType") InvoiceType invoiceType,
                                @Param("department") BusinessUnit department,
                                @Param("status") InvoiceStatus status,
                                @Param("customerId") Long customerId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("search") String search,
                                Pageable pageable);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.invoiceType = :invoiceType " +
           "AND (:department IS NULL OR i.department = :department) " +
           "AND i.invoiceDate >= :startDate AND i.invoiceDate <= :endDate " +
           "AND i.status != 'CANCELLED'")
    BigDecimal sumAmountByTypeAndDateRange(@Param("invoiceType") InvoiceType invoiceType,
                                          @Param("department") BusinessUnit department,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
           "WHERE i.invoiceType = :invoiceType " +
           "AND i.invoiceDate >= :startDate AND i.invoiceDate <= :endDate " +
           "AND i.status != 'CANCELLED'")
    BigDecimal sumAmountByTypeThisMonth(@Param("invoiceType") InvoiceType invoiceType,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}
