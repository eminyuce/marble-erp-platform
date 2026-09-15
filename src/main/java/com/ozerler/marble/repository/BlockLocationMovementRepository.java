package com.ozerler.marble.repository;

import com.ozerler.marble.model.BlockLocationMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockLocationMovementRepository extends JpaRepository<BlockLocationMovement, Long> {
    List<BlockLocationMovement> findByBlockIdOrderByCreatedDateDesc(Long blockId);
}
