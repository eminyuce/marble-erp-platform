package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.MachineFuelDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.FuelPeriodSummary;
import com.ozerler.marble.dto.view.MachineFuelEntryView;
import com.ozerler.marble.dto.view.MachineRef;
import com.ozerler.marble.mapper.MachineFuelMapper;
import com.ozerler.marble.mapper.MachineMapper;
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
    private final MachineMapper machineMapper;
    private final MachineFuelMapper machineFuelMapper;
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
    public TabulatorResponse<MachineFuelDto> getFuelPaged(
            int page, int size, String search, String period, String unit, Long machineId, String sortField, String sortDir) {

        List<MachineFuelEntry> allEntries;
        if (period != null && !period.isBlank()) {
            allEntries = listByPeriod(period);
        } else {
            allEntries = listAll();
        }

        Stream<MachineFuelEntry> stream = allEntries.stream();

        if (machineId != null) {
            stream = stream.filter(e -> e.getMachine() != null && e.getMachine().getId().equals(machineId));
        }

        if (unit != null && !unit.isBlank()) {
            stream = stream.filter(e -> e.getMachine() != null && e.getMachine().getBusinessUnit() != null && e.getMachine().getBusinessUnit().name().equalsIgnoreCase(unit));
        }

        if (search != null && !search.isBlank()) {
            String s = search.trim().toLowerCase(Locale.ROOT);
            stream = stream.filter(e -> {
                String mName;
                if (e.getMachine() != null) {
                    mName = e.getMachine().getName().toLowerCase(Locale.ROOT);
                } else {
                    mName = "";
                }
                String mCode;
                if (e.getMachine() != null) {
                    mCode = e.getMachine().getCode().toLowerCase(Locale.ROOT);
                } else {
                    mCode = "";
                }
                String receipt;
                if (e.getReceiptNo() != null) {
                    receipt = e.getReceiptNo().toLowerCase(Locale.ROOT);
                } else {
                    receipt = "";
                }
                String issued;
                if (e.getIssuedBy() != null) {
                    issued = e.getIssuedBy().toLowerCase(Locale.ROOT);
                } else {
                    issued = "";
                }
                String received;
                if (e.getReceivedBy() != null) {
                    received = e.getReceivedBy().toLowerCase(Locale.ROOT);
                } else {
                    received = "";
                }
                String notes;
                if (e.getNotes() != null) {
                    notes = e.getNotes().toLowerCase(Locale.ROOT);
                } else {
                    notes = "";
                }
                return mName.contains(s) || mCode.contains(s) || receipt.contains(s) || issued.contains(s) || received.contains(s) || notes.contains(s);
            });
        }

        List<MachineFuelDto> dtos = stream
                .map(MachineFuelDto::fromEntity)
                .collect(Collectors.toList());

        // Sort
        Comparator<MachineFuelDto> comparator = Comparator.comparing(
                dto -> dto.getEntryDate(),
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        if ("litres".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparing(dto -> dto.getLitres(), Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("totalAmount".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparing(dto -> dto.getTotalAmount(), Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("machineName".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparing(dto -> dto.getMachineName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else if ("workingHoursOrKm".equalsIgnoreCase(sortField)) {
            comparator = Comparator.comparing(dto -> dto.getWorkingHoursOrKm(), Comparator.nullsLast(Comparator.naturalOrder()));
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

        List<MachineFuelDto> pageData = dtos.subList(fromIndex, toIndex);

        BigDecimal totalLitres = dtos.stream().map(dto -> dto.getLitres()).filter(value -> value != null).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal totalAmount = dtos.stream().map(dto -> dto.getTotalAmount()).filter(value -> value != null).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        Map<String, Object> meta = Map.of(
                "totalLitres", totalLitres,
                "totalAmount", totalAmount,
                "count", totalElements
        );

        return TabulatorResponse.of(pageData, totalPages, totalElements, meta);
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
    public MachineFuelEntryView recordFuelView(Long machineId, LocalDate entryDate, BigDecimal litres,
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
    public List<MachineFuelEntryView> listViewsByPeriod(String period) {
        return listByPeriod(period).stream().map(machineFuelMapper::toDto).toList();
    }

    /**
     * Same as {@code quarryAndFactoryMachines}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<MachineRef> quarryAndFactoryMachineViews() {
        return quarryAndFactoryMachines().stream().map(machineMapper::toDto).toList();
    }

    /**
     * Totals of a period's fuel entries for the summary cards.
     *
     * @param period period as {@code yyyy-MM}; blank means all entries
     * @return litres, amount, entry and machine counts
     */
    @Transactional(readOnly = true)
    public FuelPeriodSummary periodSummary(String period) {
        List<MachineFuelEntry> entries = listByPeriod(period);
        BigDecimal totalLitres = entries.stream().map(MachineFuelEntry::getLitres)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = entries.stream().map(MachineFuelEntry::getTotalAmount)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long machineCount = entries.stream().filter(entry -> entry.getMachine() != null)
                .map(entry -> entry.getMachine().getId()).distinct().count();
        return new FuelPeriodSummary(totalLitres, totalAmount, entries.size(), machineCount);
    }
}
