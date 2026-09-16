package com.ozerler.marble.repository;

import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
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
}
