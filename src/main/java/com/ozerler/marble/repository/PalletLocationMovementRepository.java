package com.ozerler.marble.repository;

import com.ozerler.marble.model.PalletLocationMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PalletLocationMovementRepository extends JpaRepository<PalletLocationMovement, Long> {

    @EntityGraph(attributePaths = {"fromLocation", "toLocation"})
    List<PalletLocationMovement> findByPalletIdOrderByCreatedDateDesc(Long palletId);
}
