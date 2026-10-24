package com.ozerler.marble.repository;

import com.ozerler.marble.model.OperationWorkOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationWorkOrderItemRepository extends JpaRepository<OperationWorkOrderItem, Long> {

    List<OperationWorkOrderItem> findByWorkOrderIdOrderByItemOrderAscIdAsc(Long workOrderId);

    void deleteByWorkOrderId(Long workOrderId);
}
