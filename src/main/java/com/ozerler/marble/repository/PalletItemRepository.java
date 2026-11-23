package com.ozerler.marble.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.PalletItem;

@Repository
public interface PalletItemRepository extends JpaRepository<PalletItem, Long> {
    List<PalletItem> findByPalletId(Long palletId);

    List<PalletItem> findByMaterialLotId(Long materialLotId);

    @EntityGraph(attributePaths = {"pallet", "materialLot"})
    List<PalletItem> findByMaterialLotIdIn(Collection<Long> materialLotIds);

    @EntityGraph(attributePaths = {"pallet", "materialLot"})
    List<PalletItem> findByPalletIn(Collection<com.ozerler.marble.model.Pallet> pallets);
}
