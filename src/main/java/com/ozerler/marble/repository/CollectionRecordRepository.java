package com.ozerler.marble.repository;

import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.enums.CollectionMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CollectionRecordRepository extends JpaRepository<CollectionRecord, Long> {

    Optional<CollectionRecord> findByCollectionNo(String collectionNo);

    boolean existsByCollectionNo(String collectionNo);

    @Query("SELECT c FROM CollectionRecord c " +
           "LEFT JOIN FETCH c.customer " +
           "LEFT JOIN FETCH c.invoice " +
           "WHERE (:method IS NULL OR c.collectionMethod = :method) " +
           "AND (:customerId IS NULL OR c.customer.id = :customerId) " +
           "AND (:startDate IS NULL OR c.collectionDate >= :startDate) " +
           "AND (:endDate IS NULL OR c.collectionDate <= :endDate) " +
           "AND (:search IS NULL OR :search = '' " +
           "OR LOWER(c.collectionNo) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(COALESCE(c.customer.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "ORDER BY c.collectionDate DESC, c.id DESC")
    Page<CollectionRecord> searchCollections(@Param("method") CollectionMethod method,
                                            @Param("customerId") Long customerId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("search") String search,
                                            Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CollectionRecord c " +
           "WHERE (:method IS NULL OR c.collectionMethod = :method) " +
           "AND c.collectionDate >= :startDate AND c.collectionDate <= :endDate")
    BigDecimal sumAmountByMethodAndDateRange(@Param("method") CollectionMethod method,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
