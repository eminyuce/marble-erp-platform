package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.StockLocationType;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
