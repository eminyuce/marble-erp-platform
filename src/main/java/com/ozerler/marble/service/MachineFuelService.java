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
                                       BigDecimal pricePerLitre, String receiptNo,
                                       String issuedBy, String receivedBy, String notes) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.machine.not_found", machineId)));
        if (machine.getBusinessUnit() != BusinessUnit.QUARRY) {
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
                .receiptNo(receiptNo)
                .issuedBy(issuedBy)
                .receivedBy(receivedBy)
                .notes(notes)
                .build();
        entry.calculateTotal();
        MachineFuelEntry saved = fuelEntryRepository.save(entry);

        costCenterRepository.findByCode("CC-001").ifPresent(center -> expenseService.recordExpense(
                new ExpenseService.ExpenseDraft(
                        center.getId(), ExpenseType.DIESEL, null, BusinessUnit.QUARRY, saved.getTotalAmount(),
                        "TRY", receiptNo, entry.getEntryDate(), entry.getEntryDate(),
                        YearMonth.from(entry.getEntryDate()).toString(),
                        YearMonth.now().toString(),
                        null, null, null, machine, null, null, machine.getCode(),
                        "Mazot: " + machine.getName() + " / " + litres + " L")));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MachineFuelEntry> listAll() {
        return fuelEntryRepository.findAllByOrderByEntryDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Machine> quarryMachines() {
        return machineRepository.findByBusinessUnitAndActiveTrueOrderByNameAsc(BusinessUnit.QUARRY);
    }
}
