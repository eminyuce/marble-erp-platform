package com.ozerler.marble.repository;

import com.ozerler.marble.model.MachineFuelEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MachineFuelEntryRepository extends JpaRepository<MachineFuelEntry, Long> {

    List<MachineFuelEntry> findByMachineIdOrderByEntryDateDesc(Long machineId);

    @EntityGraph(attributePaths = "machine")
    List<MachineFuelEntry> findAllByOrderByEntryDateDesc();

    @EntityGraph(attributePaths = "machine")
    List<MachineFuelEntry> findByEntryDateBetweenOrderByEntryDateDesc(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM MachineFuelEntry e WHERE e.entryDate >= :start AND e.entryDate < :end")
    BigDecimal sumAmountBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT e.machine.name, COALESCE(SUM(e.litres), 0), COALESCE(SUM(e.totalAmount), 0), COUNT(e) " +
           "FROM MachineFuelEntry e WHERE e.entryDate >= :start AND e.entryDate < :end GROUP BY e.machine.name ORDER BY SUM(e.litres) DESC")
    List<Object[]> sumFuelByMachineBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    boolean existsByMachineId(Long machineId);
}
