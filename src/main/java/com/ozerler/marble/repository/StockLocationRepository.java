package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.web.GridColumnFilterRewriter;

@Repository
public interface StockLocationRepository extends JpaRepository<StockLocation, Long> {

    Optional<StockLocation> findByCode(String code);

    boolean existsByCode(String code);

    Optional<StockLocation> findByLocationTypeAndActiveTrue(StockLocationType locationType);

    List<StockLocation> findByActiveTrueOrderByNameAsc();

    List<StockLocation> findAllByOrderByCodeAsc();

    @Query(value = "SELECT l FROM StockLocation l WHERE (:search IS NULL "
            + "OR LOWER(l.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(l.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
            + "AND (:unit IS NULL OR l.businessUnit = :unit)",
            queryRewriter = GridColumnFilterRewriter.class)
    Page<StockLocation> search(@Param("search") String search, @Param("unit") BusinessUnit unit, Pageable pageable);
}
