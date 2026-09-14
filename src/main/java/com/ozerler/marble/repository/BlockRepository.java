package com.ozerler.marble.repository;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.enums.BlockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockCode(String blockCode);

    @Query("SELECT b FROM Block b JOIN FETCH b.quarry WHERE b.blockCode = :blockCode")
    Optional<Block> findByBlockCodeWithQuarry(@Param("blockCode") String blockCode);

    @EntityGraph(attributePaths = {"quarry"})
    @Query("SELECT b FROM Block b WHERE b.id = :id")
    Optional<Block> findByIdWithQuarry(@Param("id") Long id);

    @EntityGraph(attributePaths = {"quarry"})
    @Query("SELECT b FROM Block b ORDER BY b.id DESC")
    List<Block> findAllWithQuarry();

    List<Block> findByStatus(BlockStatus status);

    long countByStatus(BlockStatus status);

    @EntityGraph(attributePaths = {"quarry"})
    @Query("SELECT b FROM Block b WHERE " +
            "(:search IS NULL OR LOWER(b.blockCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(b.stoneType) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
            "LOWER(b.quarry.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Block> searchBlocks(@Param("search") String search, Pageable pageable);

    @Query("SELECT b FROM Block b WHERE LOWER(b.blockCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Block> searchByBlockCode(@Param("query") String query, Pageable pageable);

    @Query("SELECT SUM(b.actualWeightKg) FROM Block b WHERE b.status = 'FACTORY_STOCK'")
    Double getTotalFactoryStockWeightKg();

    @Query("SELECT COUNT(b) FROM Block b WHERE b.status = 'FACTORY_STOCK'")
    long getCountFactoryStock();
}
