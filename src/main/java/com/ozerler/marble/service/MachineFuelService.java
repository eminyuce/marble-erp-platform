package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.util.MessageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MachineFuelService {

    private static final int DEFAULT_PAGE_SIZE = 25;

    private final MachineFuelEntryRepository fuelEntryRepository;
    private final com.ozerler.marble.mapper.MachineMapper machineMapper;
    private final com.ozerler.marble.mapper.MachineFuelMapper machineFuelMapper;
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
        LocalDate entryDateValue;
        if (entryDate != null) {
            entryDateValue = entryDate;
        } else {
            entryDateValue = LocalDate.now();
        }
        MachineFuelEntry entry = MachineFuelEntry.builder()
                .machine(machine)
                .entryDate(entryDateValue)
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

        CostCenter center = null;
        if (machine.getBusinessUnit() == BusinessUnit.FACTORY) {
            center = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.FACTORY)
                    .or(() -> costCenterRepository.findByCode("CC-002"))
                    .orElse(null);
        } else {
            center = costCenterRepository.findByCode("CC-001")
                    .or(() -> costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.QUARRY))
                    .orElse(null);
        }

        if (center != null) {
            CostCenter finalCenter = center;
            String value;
            if (workingHoursOrKm != null) {
                value = workingHoursOrKm + " saat/km";
            } else {
                value = "Saat belirtilmedi";
            }
            expenseService.recordExpense(
                    new ExpenseService.ExpenseDraft(
                            finalCenter.getId(), ExpenseType.DIESEL, null, machine.getBusinessUnit(), saved.getTotalAmount(),
                            "TRY", receiptNo, entry.getEntryDate(), entry.getEntryDate(),
                            YearMonth.from(entry.getEntryDate()).toString(),
                            YearMonth.now().toString(),
                            null, null, null, null, machine, null, null, machine.getCode(),
                            "Mazot: " + machine.getName() + " / " + litres + " L (" + value + ")"));
        }
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

    public record MachineFuelReportItem(String machineName, BigDecimal totalLitres, BigDecimal totalAmount, long entryCount) {
    }

    @Transactional(readOnly = true)
    public List<MachineFuelReportItem> getMonthlyReport(String period) {
        YearMonth ym;
        if (period != null && !period.isBlank()) {
            ym = YearMonth.parse(period);
        } else {
            ym = YearMonth.now();
        }
        List<Object[]> rows = fuelEntryRepository.sumFuelByMachineBetween(ym.atDay(1), ym.plusMonths(1).atDay(1));
        return rows.stream()
                .map(r -> new MachineFuelReportItem((String) r[0], (BigDecimal) r[1], (BigDecimal) r[2], ((Number) r[3]).longValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Machine> quarryAndFactoryMachines() {
        return BoundedReads.all(machineRepository).stream()
                .filter(m -> m.isActive() && (m.getBusinessUnit() == BusinessUnit.QUARRY || m.getBusinessUnit() == BusinessUnit.FACTORY))
                .filter(m -> m.getMachineType() == null || !m.getMachineType().isElectric())
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public com.ozerler.marble.dto.TabulatorResponse<com.ozerler.marble.dto.MachineFuelDto> getFuelPaged(
            int page, int size, String search, String period, String unit, Long machineId, String sortField, String sortDir) {

        List<MachineFuelEntry> allEntries;
        if (period != null && !period.isBlank()) {
            allEntries = listByPeriod(period);
        } else {
            allEntries = listAll();
        }

        java.util.stream.Stream<MachineFuelEntry> stream = allEntries.stream();

        if (machineId != null) {
            stream = stream.filter(e -> e.getMachine() != null && e.getMachine().getId().equals(machineId));
        }

        if (unit != null && !unit.isBlank()) {
            stream = stream.filter(e -> e.getMachine() != null && e.getMachine().getBusinessUnit() != null && e.getMachine().getBusinessUnit().name().equalsIgnoreCase(unit));
        }

        if (search != null && !search.isBlank()) {
            String s = search.trim().toLowerCase(java.util.Locale.ROOT);
            stream = stream.filter(e -> {
                String mName;
                if (e.getMachine() != null) {
                    mName = e.getMachine().getName().toLowerCase(java.util.Locale.ROOT);
                } else {
                    mName = "";
                }
                String mCode;
                if (e.getMachine() != null) {
                    mCode = e.getMachine().getCode().toLowerCase(java.util.Locale.ROOT);
                } else {
                    mCode = "";
                }
                String receipt;
                if (e.getReceiptNo() != null) {
                    receipt = e.getReceiptNo().toLowerCase(java.util.Locale.ROOT);
                } else {
                    receipt = "";
                }
                String issued;
                if (e.getIssuedBy() != null) {
                    issued = e.getIssuedBy().toLowerCase(java.util.Locale.ROOT);
                } else {
                    issued = "";
                }
                String received;
                if (e.getReceivedBy() != null) {
                    received = e.getReceivedBy().toLowerCase(java.util.Locale.ROOT);
                } else {
                    received = "";
                }
                String notes;
                if (e.getNotes() != null) {
                    notes = e.getNotes().toLowerCase(java.util.Locale.ROOT);
                } else {
                    notes = "";
                }
                return mName.contains(s) || mCode.contains(s) || receipt.contains(s) || issued.contains(s) || received.contains(s) || notes.contains(s);
            });
        }

        List<com.ozerler.marble.dto.MachineFuelDto> dtos = stream
                .map(com.ozerler.marble.dto.MachineFuelDto::fromEntity)
                .collect(java.util.stream.Collectors.toList());

        // Sort
        java.util.Comparator<com.ozerler.marble.dto.MachineFuelDto> comparator = java.util.Comparator.comparing(
                dto -> dto.getEntryDate(),
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
        );

        if ("litres".equalsIgnoreCase(sortField)) {
            comparator = java.util.Comparator.comparing(dto -> dto.getLitres(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        } else if ("totalAmount".equalsIgnoreCase(sortField)) {
            comparator = java.util.Comparator.comparing(dto -> dto.getTotalAmount(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        } else if ("machineName".equalsIgnoreCase(sortField)) {
            comparator = java.util.Comparator.comparing(dto -> dto.getMachineName(), java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else if ("workingHoursOrKm".equalsIgnoreCase(sortField)) {
            comparator = java.util.Comparator.comparing(dto -> dto.getWorkingHoursOrKm(), java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        }

        if ("desc".equalsIgnoreCase(sortDir) && !"entryDate".equalsIgnoreCase(sortField)) {
            comparator = comparator.reversed();
        } else if ("asc".equalsIgnoreCase(sortDir) && "entryDate".equalsIgnoreCase(sortField)) {
            comparator = comparator.reversed();
        }

        dtos.sort(comparator);

        int totalElements = dtos.size();
        int safeSize;
        if (size > 0) {
            safeSize = size;
        } else {
            safeSize = DEFAULT_PAGE_SIZE;
        }
        int safePage = Math.max(1, page);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = Math.min((safePage - 1) * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<com.ozerler.marble.dto.MachineFuelDto> pageData = dtos.subList(fromIndex, toIndex);

        BigDecimal totalLitres = dtos.stream().map(dto -> dto.getLitres()).filter(value -> value != null).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal totalAmount = dtos.stream().map(dto -> dto.getTotalAmount()).filter(value -> value != null).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        java.util.Map<String, Object> meta = java.util.Map.of(
                "totalLitres", totalLitres,
                "totalAmount", totalAmount,
                "count", totalElements
        );

        return com.ozerler.marble.dto.TabulatorResponse.of(pageData, totalPages, totalElements, meta);
    }

    /**
     * Same as {@code recordFuel}, copied into a view inside the transaction.
     *
     * @param machineId see {@code recordFuel}
     * @param entryDate see {@code recordFuel}
     * @param litres see {@code recordFuel}
     * @param pricePerLitre see {@code recordFuel}
     * @param workingHoursOrKm see {@code recordFuel}
     * @param receiptNo see {@code recordFuel}
     * @param issuedBy see {@code recordFuel}
     * @param receivedBy see {@code recordFuel}
     * @param notes see {@code recordFuel}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.MachineFuelEntryView recordFuelView(Long machineId, LocalDate entryDate, BigDecimal litres,
                                       BigDecimal pricePerLitre, BigDecimal workingHoursOrKm, String receiptNo,
                                       String issuedBy, String receivedBy, String notes) {
        return machineFuelMapper.toDto(recordFuel(machineId, entryDate, litres, pricePerLitre, workingHoursOrKm, receiptNo, issuedBy, receivedBy, notes));
    }

    /**
     * Same as {@code listByPeriod}, copied into a view inside the transaction.
     *
     * @param period see {@code listByPeriod}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.MachineFuelEntryView> listViewsByPeriod(String period) {
        return listByPeriod(period).stream().map(machineFuelMapper::toDto).toList();
    }

    /**
     * Same as {@code quarryAndFactoryMachines}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.MachineRef> quarryAndFactoryMachineViews() {
        return quarryAndFactoryMachines().stream().map(machineMapper::toDto).toList();
    }

    /**
     * Totals of a period's fuel entries for the summary cards.
     *
     * @param period period as {@code yyyy-MM}; blank means all entries
     * @return litres, amount, entry and machine counts
     */
    @Transactional(readOnly = true)
    public com.ozerler.marble.dto.view.FuelPeriodSummary periodSummary(String period) {
        List<MachineFuelEntry> entries = listByPeriod(period);
        BigDecimal totalLitres = entries.stream().map(MachineFuelEntry::getLitres)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = entries.stream().map(MachineFuelEntry::getTotalAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long machineCount = entries.stream().filter(entry -> entry.getMachine() != null)
                .map(entry -> entry.getMachine().getId()).distinct().count();
        return new com.ozerler.marble.dto.view.FuelPeriodSummary(totalLitres, totalAmount, entries.size(), machineCount);
    }
}
