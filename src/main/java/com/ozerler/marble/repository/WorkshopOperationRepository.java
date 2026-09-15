package com.ozerler.marble.repository;

import com.ozerler.marble.model.WorkshopOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkshopOperationRepository extends JpaRepository<WorkshopOperation, Long> {
    List<WorkshopOperation> findByCutOrderIdOrderByIdAsc(Long cutOrderId);
}
