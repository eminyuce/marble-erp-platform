package com.ozerler.marble.repository;

import com.ozerler.marble.model.CostPeriodClose;
import com.ozerler.marble.model.enums.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CostPeriodCloseRepository extends JpaRepository<CostPeriodClose, Long> {
    Optional<CostPeriodClose> findByBusinessUnitAndExpensePeriod(BusinessUnit businessUnit, String expensePeriod);

    boolean existsByBusinessUnitAndExpensePeriod(BusinessUnit businessUnit, String expensePeriod);
}
