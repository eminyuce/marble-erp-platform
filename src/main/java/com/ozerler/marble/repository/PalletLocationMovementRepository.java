package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.PalletLocationMovement;

@Repository
public interface PalletLocationMovementRepository extends JpaRepository<PalletLocationMovement, Long> {

    @EntityGraph(attributePaths = {"fromLocation", "toLocation"})
    List<PalletLocationMovement> findByPalletIdOrderByCreatedDateDesc(Long palletId);
}
