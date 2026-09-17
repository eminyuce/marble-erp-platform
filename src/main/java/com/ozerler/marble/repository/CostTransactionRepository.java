package com.ozerler.marble.repository;

import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CostTransactionRepository extends JpaRepository<CostTransaction, Long> {

    List<CostTransaction> findByCostCenterId(Long centerId);

    long countByCostCenterId(Long costCenterId);

    List<CostTransaction> findByBlockId(Long blockId);

    List<CostTransaction> findByProjectId(Long projectId);

    List<CostTransaction> findByExpenseType(ExpenseType expenseType);

    List<CostTransaction> findByBusinessUnitAndExpensePeriod(BusinessUnit businessUnit, String expensePeriod);

    @Query("SELECT c.costCenter.name, SUM(c.amount) FROM CostTransaction c GROUP BY c.costCenter.name")
    List<Object[]> getCostDistributionByCenter();

    @Query("SELECT c.expenseType, SUM(c.amount) FROM CostTransaction c GROUP BY c.expenseType")
    List<Object[]> getCostDistributionByType();

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostTransaction c")
    BigDecimal getTotalCostAmount();

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.businessUnit = :unit AND c.expensePeriod = :period")
    BigDecimal sumByUnitAndPeriod(@Param("unit") BusinessUnit unit, @Param("period") String period);

    @Query("SELECT c.expenseCategory, COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.businessUnit = :unit AND c.expensePeriod = :period GROUP BY c.expenseCategory")
    List<Object[]> sumByCategoryForUnitAndPeriod(@Param("unit") BusinessUnit unit, @Param("period") String period);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostTransaction c WHERE c.project.id = :projectId")
    BigDecimal sumByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT c.expenseCategory, COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.project.id = :projectId GROUP BY c.expenseCategory")
    List<Object[]> sumCategoriesByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT c.project.id, c.expenseCategory, COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.project IS NOT NULL GROUP BY c.project.id, c.expenseCategory")
    List<Object[]> sumCategoriesGroupedByProjectId();

    @Query("SELECT c.quarry.id, COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.quarry IS NOT NULL AND c.expensePeriod = :period GROUP BY c.quarry.id")
    List<Object[]> sumAmountGroupedByQuarryForPeriod(@Param("period") String period);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.quarry.id = :quarryId AND c.expensePeriod = :period")
    BigDecimal sumByQuarryAndPeriod(@Param("quarryId") Long quarryId, @Param("period") String period);

    @Query("SELECT c.expenseType, COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.quarry.id = :quarryId AND c.expensePeriod = :period GROUP BY c.expenseType")
    List<Object[]> sumByTypeForQuarryAndPeriod(@Param("quarryId") Long quarryId, @Param("period") String period);

    @Query("SELECT c.expenseType, COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "WHERE c.businessUnit = :unit AND c.expensePeriod = :period GROUP BY c.expenseType")
    List<Object[]> sumByTypeForUnitAndPeriod(@Param("unit") BusinessUnit unit, @Param("period") String period);

    List<CostTransaction> findByBusinessUnitAndExpensePeriodOrderByEntryDateDesc(
            BusinessUnit businessUnit, String expensePeriod);

    @Query("SELECT c FROM CostTransaction c "
            + "LEFT JOIN c.costCenter cc "
            + "LEFT JOIN c.quarry q "
            + "LEFT JOIN c.project p "
            + "WHERE (:unit IS NULL OR c.businessUnit = :unit) "
            + "AND (:period IS NULL OR :period = '' OR c.expensePeriod = :period) "
            + "AND (:expenseType IS NULL OR c.expenseType = :expenseType) "
            + "AND (:quarryId IS NULL OR (c.quarry IS NOT NULL AND c.quarry.id = :quarryId)) "
            + "AND (:projectId IS NULL OR (c.project IS NOT NULL AND c.project.id = :projectId)) "
            + "AND (:centerId IS NULL OR cc.id = :centerId) "
            + "AND (:search IS NULL OR :search = '' OR "
            + "     LOWER(COALESCE(c.documentNo, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(cc.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(cc.code, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(q.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CostTransaction> searchExpenses(@Param("search") String search,
                                         @Param("unit") BusinessUnit unit,
                                         @Param("period") String period,
                                         @Param("expenseType") ExpenseType expenseType,
                                         @Param("centerId") Long centerId,
                                         @Param("quarryId") Long quarryId,
                                         @Param("projectId") Long projectId,
                                         Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostTransaction c "
            + "LEFT JOIN c.costCenter cc "
            + "LEFT JOIN c.quarry q "
            + "LEFT JOIN c.project p "
            + "WHERE (:unit IS NULL OR c.businessUnit = :unit) "
            + "AND (:period IS NULL OR :period = '' OR c.expensePeriod = :period) "
            + "AND (:expenseType IS NULL OR c.expenseType = :expenseType) "
            + "AND (:quarryId IS NULL OR (c.quarry IS NOT NULL AND c.quarry.id = :quarryId)) "
            + "AND (:projectId IS NULL OR (c.project IS NOT NULL AND c.project.id = :projectId)) "
            + "AND (:centerId IS NULL OR cc.id = :centerId) "
            + "AND (:search IS NULL OR :search = '' OR "
            + "     LOWER(COALESCE(c.documentNo, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(cc.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(cc.code, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(q.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "     LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    BigDecimal sumFilteredExpenses(@Param("search") String search,
                                   @Param("unit") BusinessUnit unit,
                                   @Param("period") String period,
                                   @Param("expenseType") ExpenseType expenseType,
                                   @Param("centerId") Long centerId,
                                   @Param("quarryId") Long quarryId,
                                   @Param("projectId") Long projectId);

    long countByExpensePeriod(String expensePeriod);
}
