package com.ozerler.marble.repository;

import com.ozerler.marble.model.SiteInstallation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteInstallationRepository extends JpaRepository<SiteInstallation, Long> {

    @EntityGraph(attributePaths = {"location"})
    List<SiteInstallation> findByProjectIdOrderByInstalledOnDesc(Long projectId);

    List<SiteInstallation> findByLocationId(Long locationId);

    List<SiteInstallation> findByMaterialLotId(Long materialLotId);
}
