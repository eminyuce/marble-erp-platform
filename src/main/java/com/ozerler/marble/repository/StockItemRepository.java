package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.enums.StockProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    Optional<StockItem> findByItemCode(String itemCode);

    boolean existsByItemCode(String itemCode);

    List<StockItem> findByProductTypeAndStatus(StockProductType productType, String status);

    List<StockItem> findByCustomerIdAndStatus(Long customerId, String status);

    @Query("SELECT s FROM StockItem s " +
           "LEFT JOIN s.customer c " +
           "LEFT JOIN s.sourceBlock b " +
           "LEFT JOIN s.stockLocation l " +
           "WHERE (:productType IS NULL OR s.productType = :productType) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:customerId IS NULL OR c.id = :customerId) " +
           "AND (:generalStockOnly = false OR c IS NULL) " +
           "AND (:search IS NULL OR :search = '' " +
           "OR LOWER(s.itemCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(COALESCE(s.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(COALESCE(s.stoneType, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "ORDER BY s.id DESC")
    Page<StockItem> searchItems(@Param("productType") StockProductType productType,
                               @Param("status") String status,
                               @Param("customerId") Long customerId,
                               @Param("generalStockOnly") boolean generalStockOnly,
                               @Param("search") String search,
                               Pageable pageable);

    @Query("SELECT COUNT(s) FROM StockItem s WHERE s.productType = :productType AND s.status = 'AVAILABLE'")
    long countAvailableByProductType(@Param("productType") StockProductType productType);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockItem s WHERE s.productType = :productType AND s.status = 'AVAILABLE'")
    BigDecimal sumQuantityByProductType(@Param("productType") StockProductType productType);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockItem s WHERE s.customer.id = :customerId AND s.status = 'AVAILABLE'")
    BigDecimal sumQuantityByCustomer(@Param("customerId") Long customerId);
}
