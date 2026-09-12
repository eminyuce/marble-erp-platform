package com.ozerler.marble.repository;

import com.ozerler.marble.model.ScrapLog;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapLogRepository extends JpaRepository<ScrapLog, Long> {

    Optional<ScrapLog> findByScrapCode(String scrapCode);

    List<ScrapLog> findByReasonCode(ScrapReasonCode reasonCode);

    List<ScrapLog> findByBlockId(Long blockId);

    List<ScrapLog> findByProductionOrderId(Long orderId);

    @EntityGraph(attributePaths = {"block"})
    @Query("SELECT s FROM ScrapLog s ORDER BY s.loggedAt DESC")
    List<ScrapLog> findAllWithBlock();

    @Query("SELECT s.reasonCode, COUNT(s), COALESCE(SUM(s.scrapWeightKg), 0), COALESCE(SUM(s.costImpact), 0) " +
           "FROM ScrapLog s GROUP BY s.reasonCode")
    List<Object[]> getScrapSummaryByReason();

    @Query("SELECT COALESCE(SUM(s.costImpact), 0) FROM ScrapLog s")
    BigDecimal getTotalScrapCostImpact();

    @Query("SELECT s FROM ScrapLog s ORDER BY s.loggedAt DESC")
    Page<ScrapLog> findAllPaged(Pageable pageable);
}
