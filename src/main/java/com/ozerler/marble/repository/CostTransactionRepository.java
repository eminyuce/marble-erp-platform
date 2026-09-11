package com.ozerler.marble.repository;

import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.enums.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CostTransactionRepository extends JpaRepository<CostTransaction, Long> {

    List<CostTransaction> findByCostCenterId(Long centerId);

    List<CostTransaction> findByBlockId(Long blockId);

    List<CostTransaction> findByProjectId(Long projectId);

    List<CostTransaction> findByExpenseType(ExpenseType expenseType);

    @Query("SELECT c.costCenter.name, SUM(c.amount) FROM CostTransaction c GROUP BY c.costCenter.name")
    List<Object[]> getCostDistributionByCenter();

    @Query("SELECT c.expenseType, SUM(c.amount) FROM CostTransaction c GROUP BY c.expenseType")
    List<Object[]> getCostDistributionByType();

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CostTransaction c")
    BigDecimal getTotalCostAmount();
}
