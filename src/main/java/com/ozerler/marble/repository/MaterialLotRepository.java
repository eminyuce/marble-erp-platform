package com.ozerler.marble.repository;

import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.model.enums.ProductForm;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialLotRepository extends JpaRepository<MaterialLot, Long> {

    Optional<MaterialLot> findByLotCode(String lotCode);

    Optional<MaterialLot> findBySlabId(Long slabId);

    Optional<MaterialLot> findByCutItemId(Long cutItemId);

    List<MaterialLot> findByProductFormAndStatus(ProductForm productForm, MaterialLotStatus status);

    List<MaterialLot> findByStatus(MaterialLotStatus status);

    List<MaterialLot> findBySourceBlockId(Long blockId);

    @EntityGraph(attributePaths = "slab")
    List<MaterialLot> findBySlabIdIn(Collection<Long> slabIds);
}
