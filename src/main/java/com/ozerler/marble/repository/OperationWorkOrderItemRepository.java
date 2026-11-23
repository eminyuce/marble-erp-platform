package com.ozerler.marble.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.OperationWorkOrderItem;

@Repository
public interface OperationWorkOrderItemRepository extends JpaRepository<OperationWorkOrderItem, Long> {

    List<OperationWorkOrderItem> findByWorkOrderIdOrderByItemOrderAscIdAsc(Long workOrderId);

    void deleteByWorkOrderId(Long workOrderId);
}
