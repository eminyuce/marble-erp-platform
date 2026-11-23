package com.ozerler.marble.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.web.GridColumnFilterRewriter;

@Repository
public interface CutItemRepository extends JpaRepository<CutItem, Long> {

    Optional<CutItem> findByItemCode(String itemCode);

    boolean existsByItemCode(String itemCode);

    List<CutItem> findByCutOrderId(Long cutOrderId);

    List<CutItem> findBySourceSlabId(Long slabId);

    List<CutItem> findBySourceSlabIdIn(Collection<Long> slabIds);

    @Query(value = "SELECT i FROM CutItem i WHERE "
            + "(:search IS NULL OR LOWER(i.itemCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(i.targetLocation) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))",
            queryRewriter = GridColumnFilterRewriter.class)
    Page<CutItem> searchItems(@Param("search") String search, Pageable pageable);

    @Query("SELECT i FROM CutItem i WHERE LOWER(i.itemCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CutItem> searchByItemCode(@Param("query") String query, Pageable pageable);
}
