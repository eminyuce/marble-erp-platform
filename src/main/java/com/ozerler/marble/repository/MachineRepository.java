package com.ozerler.marble.repository;

import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {

    Optional<Machine> findByCode(String code);

    List<Machine> findByBusinessUnitAndActiveTrueOrderByNameAsc(BusinessUnit businessUnit);

    List<Machine> findByMachineTypeAndActiveTrueOrderByNameAsc(MachineType machineType);

    List<Machine> findByActiveTrueOrderByNameAsc();
}
