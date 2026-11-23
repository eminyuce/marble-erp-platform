package com.ozerler.marble.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.ShipmentItem;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    List<ShipmentItem> findByShipmentId(Long shipmentId);

    List<ShipmentItem> findByPalletId(Long palletId);

    List<ShipmentItem> findByMaterialLotId(Long materialLotId);

    @EntityGraph(attributePaths = "shipment")
    List<ShipmentItem> findByPalletIdIn(Collection<Long> palletIds);

    boolean existsByBlockId(Long blockId);
}
