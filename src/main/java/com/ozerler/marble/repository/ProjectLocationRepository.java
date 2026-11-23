package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.ProjectLocation;

@Repository
public interface ProjectLocationRepository extends JpaRepository<ProjectLocation, Long> {
    List<ProjectLocation> findByProjectId(Long projectId);

    List<ProjectLocation> findByProjectIdAndParentIsNull(Long projectId);
}
