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
    public com.ozerler.marble.dto.TabulatorResponse<com.ozerler.marble.dto.MachineFuelDto> getFuelPaged(
            int page, int size, String search, String period, String unit, Long machineId, String sortField, String sortDir) {

        List<MachineFuelEntry> allEntries = (period != null && !period.isBlank())
                ? listByPeriod(period)
                : listAll();

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
                String mName = e.getMachine() != null ? e.getMachine().getName().toLowerCase(java.util.Locale.ROOT) : "";
                String mCode = e.getMachine() != null ? e.getMachine().getCode().toLowerCase(java.util.Locale.ROOT) : "";
                String receipt = e.getReceiptNo() != null ? e.getReceiptNo().toLowerCase(java.util.Locale.ROOT) : "";
                String issued = e.getIssuedBy() != null ? e.getIssuedBy().toLowerCase(java.util.Locale.ROOT) : "";
                String received = e.getReceivedBy() != null ? e.getReceivedBy().toLowerCase(java.util.Locale.ROOT) : "";
                String notes = e.getNotes() != null ? e.getNotes().toLowerCase(java.util.Locale.ROOT) : "";
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
        int safeSize = size > 0 ? size : 25;
        int safePage = Math.max(1, page);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = Math.min((safePage - 1) * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);

        List<com.ozerler.marble.dto.MachineFuelDto> pageData = dtos.subList(fromIndex, toIndex);

        BigDecimal totalLitres = dtos.stream().map(dto -> dto.getLitres()).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal totalAmount = dtos.stream().map(dto -> dto.getTotalAmount()).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, (a, b) -> a.add(b));

        java.util.Map<String, Object> meta = java.util.Map.of(
                "totalLitres", totalLitres,
                "totalAmount", totalAmount,
                "count", totalElements
        );

        return com.ozerler.marble.dto.TabulatorResponse.of(pageData, totalPages, totalElements, meta);
    }
}
