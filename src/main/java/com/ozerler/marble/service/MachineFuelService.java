package com.ozerler.marble.service;

import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MachineFuelService {

    private final MachineFuelEntryRepository fuelEntryRepository;
    private final MachineRepository machineRepository;
    private final CostCenterRepository costCenterRepository;
    private final ExpenseService expenseService;

    @Transactional
    public MachineFuelEntry recordFuel(Long machineId, LocalDate entryDate, BigDecimal litres,
                                       BigDecimal pricePerLitre, BigDecimal workingHoursOrKm, String receiptNo,
                                       String issuedBy, String receivedBy, String notes) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.machine.not_found", machineId)));
        if (machine.getBusinessUnit() != BusinessUnit.QUARRY && machine.getBusinessUnit() != BusinessUnit.FACTORY) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.fuel.not_quarry_machine"));
        }
        Objects.requireNonNull(litres, MessageUtils.getMessage("error.fuel.litres.required"));
        Objects.requireNonNull(pricePerLitre, MessageUtils.getMessage("error.fuel.price.required"));
        if (litres.compareTo(BigDecimal.ZERO) <= 0 || pricePerLitre.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.fuel.positive"));
        }
        MachineFuelEntry entry = MachineFuelEntry.builder()
                .machine(machine)
                .entryDate(entryDate != null ? entryDate : LocalDate.now())
                .litres(litres)
                .pricePerLitre(pricePerLitre)
                .workingHoursOrKm(workingHoursOrKm)
                .receiptNo(receiptNo)
                .issuedBy(issuedBy)
                .receivedBy(receivedBy)
                .notes(notes)
                .build();
        entry.calculateTotal();
        MachineFuelEntry saved = fuelEntryRepository.save(entry);

        costCenterRepository.findByCode("CC-001").ifPresent(center -> expenseService.recordExpense(
                new ExpenseService.ExpenseDraft(
                        center.getId(), ExpenseType.DIESEL, null, machine.getBusinessUnit(), saved.getTotalAmount(),
                        "TRY", receiptNo, entry.getEntryDate(), entry.getEntryDate(),
                        YearMonth.from(entry.getEntryDate()).toString(),
                        YearMonth.now().toString(),
                        null, null, null, null, machine, null, null, machine.getCode(),
                        "Mazot: " + machine.getName() + " / " + litres + " L (" + (workingHoursOrKm != null ? workingHoursOrKm + " saat/km" : "Saat belirtilmedi") + ")")));
        return saved;
    }

    @Transactional
    public void deleteFuel(Long id) {
        fuelEntryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<MachineFuelEntry> listAll() {
        return fuelEntryRepository.findAllByOrderByEntryDateDesc();
    }

    @Transactional(readOnly = true)
    public List<MachineFuelEntry> listByPeriod(String period) {
        if (period == null || period.isBlank()) {
            return listAll();
        }
        YearMonth ym = YearMonth.parse(period);
        return fuelEntryRepository.findByEntryDateBetweenOrderByEntryDateDesc(ym.atDay(1), ym.plusMonths(1).atDay(1));
    }

    public record MachineFuelReportItem(String machineName, BigDecimal totalLitres, BigDecimal totalAmount, long entryCount) {}

    @Transactional(readOnly = true)
    public List<MachineFuelReportItem> getMonthlyReport(String period) {
        YearMonth ym = (period != null && !period.isBlank()) ? YearMonth.parse(period) : YearMonth.now();
        List<Object[]> rows = fuelEntryRepository.sumFuelByMachineBetween(ym.atDay(1), ym.plusMonths(1).atDay(1));
        return rows.stream()
                .map(r -> new MachineFuelReportItem((String) r[0], (BigDecimal) r[1], (BigDecimal) r[2], ((Number) r[3]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Machine> quarryAndFactoryMachines() {
        return machineRepository.findAll().stream()
                .filter(m -> m.isActive() && (m.getBusinessUnit() == BusinessUnit.QUARRY || m.getBusinessUnit() == BusinessUnit.FACTORY))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Machine> quarryMachines() {
        return machineRepository.findByBusinessUnitAndActiveTrueOrderByNameAsc(BusinessUnit.QUARRY);
    }
}
