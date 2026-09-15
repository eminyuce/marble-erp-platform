package com.ozerler.marble.repository;

import com.ozerler.marble.model.BlockCustomerMark;
import com.ozerler.marble.model.enums.BlockMarkStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockCustomerMarkRepository extends JpaRepository<BlockCustomerMark, Long> {

    @EntityGraph(attributePaths = {"customer"})
    List<BlockCustomerMark> findByBlockIdOrderByMarkedAtDesc(Long blockId);

    Optional<BlockCustomerMark> findFirstByBlockIdAndStatus(Long blockId, BlockMarkStatus status);

    @EntityGraph(attributePaths = {"customer", "block"})
    List<BlockCustomerMark> findByStatusOrderByMarkedAtDesc(BlockMarkStatus status);

    boolean existsByCustomerId(Long customerId);
}
