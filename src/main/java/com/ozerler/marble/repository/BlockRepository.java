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

    boolean existsByBlockCode(String blockCode);

    boolean existsByBlockCodeAndIdNot(String blockCode, Long id);

    @Query("SELECT b FROM Block b JOIN FETCH b.quarry WHERE b.blockCode = :blockCode")
    Optional<Block> findByBlockCodeWithQuarry(@Param("blockCode") String blockCode);

    @EntityGraph(attributePaths = {"quarry", "currentLocation", "soldCustomer"})
    @Query("SELECT b FROM Block b WHERE b.id = :id")
    Optional<Block> findByIdWithQuarry(@Param("id") Long id);

    @EntityGraph(attributePaths = {"quarry"})
    @Query("SELECT b FROM Block b ORDER BY b.id DESC")
    List<Block> findAllWithQuarry();

    List<Block> findByStatus(BlockStatus status);

    List<Block> findByStatusIn(List<BlockStatus> statuses);

    long countByStatus(BlockStatus status);

    long countByStatusIn(List<BlockStatus> statuses);

    @EntityGraph(attributePaths = {"quarry", "currentLocation", "soldCustomer"})
    @Query(value = "SELECT DISTINCT b FROM Block b "
            + "LEFT JOIN b.currentLocation loc "
            + "LEFT JOIN b.soldCustomer soldCust "
            + "LEFT JOIN b.quarry q WHERE "
            + "(:search IS NULL OR LOWER(b.blockCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(COALESCE(b.stoneType, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(COALESCE(q.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
            + "LOWER(COALESCE(soldCust.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND "
            + "(:locationType IS NULL OR loc.locationType = :locationType) AND "
            + "(:status IS NULL OR b.status = :status) AND "
            + "(:unsoldOnly = false OR b.status <> com.ozerler.marble.model.enums.BlockStatus.SOLD)",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Block b "
                    + "LEFT JOIN b.currentLocation loc "
                    + "LEFT JOIN b.soldCustomer soldCust "
                    + "LEFT JOIN b.quarry q WHERE "
                    + "(:search IS NULL OR LOWER(b.blockCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
                    + "LOWER(COALESCE(b.stoneType, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
                    + "LOWER(COALESCE(q.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR "
                    + "LOWER(COALESCE(soldCust.companyName, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND "
                    + "(:locationType IS NULL OR loc.locationType = :locationType) AND "
                    + "(:status IS NULL OR b.status = :status) AND "
                    + "(:unsoldOnly = false OR b.status <> com.ozerler.marble.model.enums.BlockStatus.SOLD)")
    Page<Block> searchBlocks(@Param("search") String search,
                             @Param("locationType") com.ozerler.marble.model.enums.StockLocationType locationType,
                             @Param("status") BlockStatus status,
                             @Param("unsoldOnly") boolean unsoldOnly,
                             Pageable pageable);

    @Query("SELECT b FROM Block b WHERE LOWER(b.blockCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Block> searchByBlockCode(@Param("query") String query, Pageable pageable);

    @Query("SELECT SUM(b.actualWeightKg) FROM Block b WHERE b.status IN ('FACTORY_STOCK', 'AT_FACTORY')")
    Double getTotalFactoryStockWeightKg();

    @Query("SELECT COUNT(b) FROM Block b WHERE b.status IN ('FACTORY_STOCK', 'AT_FACTORY')")
    long getCountFactoryStock();

    @Query("SELECT COUNT(b) FROM Block b JOIN b.currentLocation loc WHERE loc.locationType = :locationType")
    long countByLocationType(@Param("locationType") com.ozerler.marble.model.enums.StockLocationType locationType);

    long countByCurrentLocation_LocationTypeAndStatusNot(
            com.ozerler.marble.model.enums.StockLocationType locationType,
            BlockStatus status);

    @Query("SELECT COALESCE(SUM(CASE WHEN b.actualWeightKg > 0 THEN b.actualWeightKg ELSE b.theoreticalWeightKg END), 0) "
            + "FROM Block b WHERE b.extractionDate >= :start AND b.extractionDate < :end")
    java.math.BigDecimal sumProductionWeightKgBetween(@Param("start") java.time.LocalDate start,
                                                      @Param("end") java.time.LocalDate end);

    boolean existsByQuarryId(Long quarryId);

    boolean existsByCurrentLocationId(Long currentLocationId);

    boolean existsBySoldCustomer_Id(Long customerId);
}
