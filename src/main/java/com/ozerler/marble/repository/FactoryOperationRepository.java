package com.ozerler.marble.repository;

import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactoryOperationRepository extends JpaRepository<FactoryOperation, Long> {

    List<FactoryOperation> findByWorkOrderIdOrderByIdAsc(Long workOrderId);

    Optional<FactoryOperation> findTopByWorkOrderIdAndStatusOrderByIdDesc(Long workOrderId, OperationStatus status);

    Optional<FactoryOperation> findTopByWorkOrderIdAndProcessTypeInAndStatusOrderByIdDesc(
            Long workOrderId, Collection<FactoryProcessType> processTypes, OperationStatus status);

    List<FactoryOperation> findByProcessTypeAndStatusOrderByIdDesc(FactoryProcessType processType, OperationStatus status);

    @EntityGraph(attributePaths = {"workOrder", "workOrder.block", "machine"})
    List<FactoryOperation> findByStatusOrderByIdDesc(OperationStatus status);

    @EntityGraph(attributePaths = {"workOrder", "workOrder.block", "machine"})
    List<FactoryOperation> findByOperatorNameAndStatus(String operatorName, OperationStatus status);

    @Query("SELECT o.processType, COALESCE(SUM(o.inputQuantity), 0), COALESCE(SUM(o.outputQuantity), 0), "
            + "COALESCE(SUM(o.wasteQuantity), 0) FROM FactoryOperation o GROUP BY o.processType")
    List<Object[]> aggregateQuantitiesByProcessType();
}
