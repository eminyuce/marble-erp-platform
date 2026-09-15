package com.ozerler.marble.repository;

import com.ozerler.marble.model.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    List<ShipmentItem> findByShipmentId(Long shipmentId);

    List<ShipmentItem> findByPalletId(Long palletId);

    List<ShipmentItem> findByMaterialLotId(Long materialLotId);
}
