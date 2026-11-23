package com.ozerler.marble.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.SiteInstallation;

@Repository
public interface SiteInstallationRepository extends JpaRepository<SiteInstallation, Long> {

    @EntityGraph(attributePaths = {"location"})
    List<SiteInstallation> findByProjectIdOrderByInstalledOnDesc(Long projectId);

    List<SiteInstallation> findByLocationId(Long locationId);

    List<SiteInstallation> findByMaterialLotId(Long materialLotId);

    @EntityGraph(attributePaths = {"project", "location", "materialLot"})
    List<SiteInstallation> findByMaterialLotIdIn(Collection<Long> materialLotIds);
}
