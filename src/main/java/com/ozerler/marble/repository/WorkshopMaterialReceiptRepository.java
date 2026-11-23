package com.ozerler.marble.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.WorkshopMaterialReceipt;

@Repository
public interface WorkshopMaterialReceiptRepository extends JpaRepository<WorkshopMaterialReceipt, Long> {
    Optional<WorkshopMaterialReceipt> findByReceiptNo(String receiptNo);

    boolean existsByReceiptNo(String receiptNo);

    @Query("""
            select distinct receipt from WorkshopMaterialReceipt receipt
            left join fetch receipt.supplier
            left join fetch receipt.materialLot
            order by receipt.receivedAt desc, receipt.id desc
            """)
    List<WorkshopMaterialReceipt> findAllWithLotAndSupplier();
}
