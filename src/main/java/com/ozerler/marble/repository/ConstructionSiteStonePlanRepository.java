package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.ConstructionSiteStonePlan;

@Repository
public interface ConstructionSiteStonePlanRepository extends JpaRepository<ConstructionSiteStonePlan, Long> {

    @EntityGraph(attributePaths = {"location"})
    List<ConstructionSiteStonePlan> findByProjectIdOrderByIdAsc(Long projectId);

    List<ConstructionSiteStonePlan> findByLocationId(Long locationId);
}
