package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.SiteSupplyAllocation;

@Repository
public interface SiteSupplyAllocationRepository extends JpaRepository<SiteSupplyAllocation, Long> {
    List<SiteSupplyAllocation> findByStonePlanId(Long stonePlanId);
}
