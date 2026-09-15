package com.ozerler.marble.repository;

import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {

    Optional<Machine> findByCode(String code);

    List<Machine> findByBusinessUnitAndActiveTrueOrderByNameAsc(BusinessUnit businessUnit);

    List<Machine> findByMachineTypeAndActiveTrueOrderByNameAsc(MachineType machineType);

    List<Machine> findByActiveTrueOrderByNameAsc();

    List<Machine> findAllByOrderByCodeAsc();

    long countByActiveTrue();

    @Query("SELECT m FROM Machine m WHERE (:search IS NULL "
            + "OR LOWER(m.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
            + "OR LOWER(m.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) "
            + "AND (:unit IS NULL OR m.businessUnit = :unit)")
    Page<Machine> search(@Param("search") String search, @Param("unit") BusinessUnit unit, Pageable pageable);
}
