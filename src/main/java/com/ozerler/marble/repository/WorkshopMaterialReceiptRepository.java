package com.ozerler.marble.repository;

import com.ozerler.marble.model.WorkshopMaterialReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkshopMaterialReceiptRepository extends JpaRepository<WorkshopMaterialReceipt, Long> {
    Optional<WorkshopMaterialReceipt> findByReceiptNo(String receiptNo);

    List<WorkshopMaterialReceipt> findAllByOrderByReceivedAtDesc();
}
