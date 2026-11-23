package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.enums.FactoryWorkOrderStatus;

@Repository
public interface FactoryWorkOrderRepository extends JpaRepository<FactoryWorkOrder, Long> {

    Optional<FactoryWorkOrder> findByOrderNo(String orderNo);

    boolean existsByOrderNo(String orderNo);

    Optional<FactoryWorkOrder> findFirstByBlockIdOrderByIdDesc(Long blockId);

    List<FactoryWorkOrder> findByStatusOrderByAcceptedAtDesc(FactoryWorkOrderStatus status);

    List<FactoryWorkOrder> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = {"block", "assignedMachine"})
    @Query("SELECT wo FROM FactoryWorkOrder wo ORDER BY wo.id DESC")
    List<FactoryWorkOrder> findAllWithBlockOrderByIdDesc();
}
