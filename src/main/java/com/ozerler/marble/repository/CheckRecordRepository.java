package com.ozerler.marble.repository;

import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.enums.CheckStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CheckRecordRepository extends JpaRepository<CheckRecord, Long> {

    @Query("SELECT ch FROM CheckRecord ch " +
           "LEFT JOIN FETCH ch.customer " +
           "LEFT JOIN FETCH ch.collection " +
           "WHERE (:status IS NULL OR ch.status = :status) " +
           "AND (:customerId IS NULL OR ch.customer.id = :customerId) " +
           "AND (:startDate IS NULL OR ch.dueDate >= :startDate) " +
           "AND (:endDate IS NULL OR ch.dueDate <= :endDate) " +
           "AND (:search IS NULL OR LOWER(ch.checkNo) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(ch.bankName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(ch.customer.companyName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY ch.dueDate ASC, ch.id DESC")
    Page<CheckRecord> searchChecks(@Param("status") CheckStatus status,
                                  @Param("customerId") Long customerId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("SELECT ch FROM CheckRecord ch " +
           "LEFT JOIN FETCH ch.customer " +
           "WHERE ch.status = 'PORTFOLIO' " +
           "AND ch.dueDate >= :today AND ch.dueDate <= :untilDate " +
           "ORDER BY ch.dueDate ASC")
    List<CheckRecord> findApproachingChecks(@Param("today") LocalDate today,
                                           @Param("untilDate") LocalDate untilDate);

    @Query("SELECT ch FROM CheckRecord ch " +
           "LEFT JOIN FETCH ch.customer " +
           "WHERE ch.status = 'PORTFOLIO' " +
           "AND ch.dueDate < :today " +
           "ORDER BY ch.dueDate ASC")
    List<CheckRecord> findOverdueChecks(@Param("today") LocalDate today);

    @Query("SELECT COUNT(ch) FROM CheckRecord ch " +
           "WHERE ch.status = 'PORTFOLIO' " +
           "AND ch.dueDate >= :today AND ch.dueDate <= :untilDate")
    long countApproachingChecks(@Param("today") LocalDate today,
                                @Param("untilDate") LocalDate untilDate);

    @Query("SELECT COALESCE(SUM(ch.amount), 0) FROM CheckRecord ch " +
           "WHERE ch.status = 'PORTFOLIO' " +
           "AND ch.dueDate >= :today AND ch.dueDate <= :untilDate")
    BigDecimal sumApproachingChecksAmount(@Param("today") LocalDate today,
                                         @Param("untilDate") LocalDate untilDate);

    @Query("SELECT COUNT(ch) FROM CheckRecord ch " +
           "WHERE ch.status = 'PORTFOLIO' " +
           "AND ch.dueDate < :today")
    long countOverdueChecks(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(ch.amount), 0) FROM CheckRecord ch " +
           "WHERE ch.status = 'PORTFOLIO' " +
           "AND ch.dueDate < :today")
    BigDecimal sumOverdueChecksAmount(@Param("today") LocalDate today);
}
