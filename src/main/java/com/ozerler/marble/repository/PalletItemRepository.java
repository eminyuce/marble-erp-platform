package com.ozerler.marble.repository;

import com.ozerler.marble.model.PalletItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PalletItemRepository extends JpaRepository<PalletItem, Long> {
    List<PalletItem> findByPalletId(Long palletId);

    List<PalletItem> findByMaterialLotId(Long materialLotId);

    @EntityGraph(attributePaths = {"pallet", "materialLot"})
    List<PalletItem> findByMaterialLotIdIn(Collection<Long> materialLotIds);
}
