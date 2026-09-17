package com.ozerler.marble.repository;

import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectCode(String projectCode);

    List<Project> findByStatus(ProjectStatus status);

    long countByStatus(ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE " +
            "(:search IS NULL OR LOWER(p.projectCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(p.customerName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Project> searchProjects(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE LOWER(p.projectCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Project> searchByCodeOrName(@Param("query") String query, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.contractValue), 0) FROM Project p")
    BigDecimal sumContractValue();
}
