package com.ozerler.marble.repository;

import com.ozerler.marble.model.Pallet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PalletRepository extends JpaRepository<Pallet, Long> {
    Optional<Pallet> findByPalletCode(String palletCode);

    boolean existsByPalletCode(String palletCode);

    List<Pallet> findByCurrentLocation_LocationType(com.ozerler.marble.model.enums.StockLocationType locationType);

    @EntityGraph(attributePaths = {"slabs", "currentLocation", "customer", "project"})
    @Query("SELECT p FROM Pallet p ORDER BY p.id DESC")
    List<Pallet> findAllWithLocationAndSlabs();

    Optional<Pallet> findByQrCodeHash(String qrCodeHash);
}
