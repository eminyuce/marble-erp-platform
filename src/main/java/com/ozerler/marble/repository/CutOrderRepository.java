package com.ozerler.marble.repository;

import com.ozerler.marble.model.CutOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CutOrderRepository extends JpaRepository<CutOrder, Long> {

    Optional<CutOrder> findByCutOrderNo(String cutOrderNo);

    List<CutOrder> findByProjectId(Long projectId);

    @Query("SELECT c FROM CutOrder c WHERE " +
           "(:search IS NULL OR LOWER(c.cutOrderNo) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.machineName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.project.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CutOrder> searchCutOrders(@Param("search") String search, Pageable pageable);
}
