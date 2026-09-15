package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockLocationRepository extends JpaRepository<StockLocation, Long> {

    Optional<StockLocation> findByCode(String code);

    Optional<StockLocation> findByLocationTypeAndActiveTrue(StockLocationType locationType);

    List<StockLocation> findByActiveTrueOrderByNameAsc();

    List<StockLocation> findAllByOrderByCodeAsc();

    long countByActiveTrue();

    @Query("SELECT l FROM StockLocation l WHERE (:search IS NULL "
            + "OR LOWER(l.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
            + "AND (:unit IS NULL OR l.businessUnit = :unit)")
    Page<StockLocation> search(@Param("search") String search, @Param("unit") BusinessUnit unit, Pageable pageable);
}
