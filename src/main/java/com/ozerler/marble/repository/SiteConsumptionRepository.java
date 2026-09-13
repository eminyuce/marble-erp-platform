package com.ozerler.marble.repository;

import com.ozerler.marble.model.SiteConsumption;
import com.ozerler.marble.model.enums.ConsumptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SiteConsumptionRepository extends JpaRepository<SiteConsumption, Long> {

    @Query("SELECT s FROM SiteConsumption s LEFT JOIN FETCH s.location WHERE s.project.id = :projectId")
    List<SiteConsumption> findByProjectId(@Param("projectId") Long projectId);

    List<SiteConsumption> findByLocationId(Long locationId);

    List<SiteConsumption> findByConsumptionType(ConsumptionType type);

    @Query("SELECT COALESCE(SUM(s.totalCost), 0) FROM SiteConsumption s WHERE s.project.id = :projectId")
    BigDecimal getTotalConsumptionByProject(@Param("projectId") Long projectId);
}
