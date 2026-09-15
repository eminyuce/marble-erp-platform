package com.ozerler.marble.repository;

import com.ozerler.marble.model.SiteSupplyAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteSupplyAllocationRepository extends JpaRepository<SiteSupplyAllocation, Long> {
    List<SiteSupplyAllocation> findByStonePlanId(Long stonePlanId);
}
