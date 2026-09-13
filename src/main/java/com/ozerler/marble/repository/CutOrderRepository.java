package com.ozerler.marble.repository;

import com.ozerler.marble.dto.OrderChildAggregate;
import com.ozerler.marble.model.CutOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CutOrderRepository extends JpaRepository<CutOrder, Long> {

    Optional<CutOrder> findByCutOrderNo(String cutOrderNo);

    List<CutOrder> findByProjectId(Long projectId);

    @EntityGraph(attributePaths = {"project"})
    @Query("SELECT c FROM CutOrder c ORDER BY c.id DESC")
    List<CutOrder> findAllWithProject();

    @EntityGraph(attributePaths = {"project", "location"})
    @Query("SELECT c FROM CutOrder c WHERE " +
            "(:search IS NULL OR LOWER(c.cutOrderNo) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(c.machineName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(c.project.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<CutOrder> searchCutOrders(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM CutOrder c LEFT JOIN FETCH c.project WHERE LOWER(c.cutOrderNo) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CutOrder> searchByCutOrderNo(@Param("query") String query, Pageable pageable);

    @Query("SELECT c.id AS parentId, COUNT(i.id) AS itemCount, COALESCE(SUM(i.areaM2), 0) AS totalArea "
            + "FROM CutOrder c LEFT JOIN c.items i WHERE c.id IN :ids GROUP BY c.id")
    List<OrderChildAggregate> aggregateItemMetrics(@Param("ids") Collection<Long> ids);
}
