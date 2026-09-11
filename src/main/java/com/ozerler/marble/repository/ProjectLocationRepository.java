package com.ozerler.marble.repository;

import com.ozerler.marble.model.ProjectLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectLocationRepository extends JpaRepository<ProjectLocation, Long> {
    List<ProjectLocation> findByProjectId(Long projectId);
    List<ProjectLocation> findByProjectIdAndParentIsNull(Long projectId);
}
