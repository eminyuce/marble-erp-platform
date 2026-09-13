package com.ozerler.marble.repository;

import com.ozerler.marble.model.CostCenter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostCenterRepository extends JpaRepository<CostCenter, Long> {
    Optional<CostCenter> findByCode(String code);

    @Query("SELECT c FROM CostCenter c WHERE LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CostCenter> searchByCodeOrName(@Param("query") String query, Pageable pageable);
}
