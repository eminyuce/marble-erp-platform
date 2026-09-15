package com.ozerler.marble.repository;

import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.OperationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactoryOperationRepository extends JpaRepository<FactoryOperation, Long> {

    List<FactoryOperation> findByWorkOrderIdOrderByIdAsc(Long workOrderId);

    List<FactoryOperation> findByProcessTypeAndStatusOrderByIdDesc(FactoryProcessType processType, OperationStatus status);

    List<FactoryOperation> findByStatusOrderByIdDesc(OperationStatus status);

    List<FactoryOperation> findByOperatorNameAndStatus(String operatorName, OperationStatus status);
}
