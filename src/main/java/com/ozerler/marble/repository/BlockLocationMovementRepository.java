package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.BlockLocationMovement;

@Repository
public interface BlockLocationMovementRepository extends JpaRepository<BlockLocationMovement, Long> {

    @EntityGraph(attributePaths = {"fromLocation", "toLocation"})
    List<BlockLocationMovement> findByBlockIdOrderByCreatedDateDesc(Long blockId);
}
