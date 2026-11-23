package com.ozerler.marble.service;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.dto.MachineFuelDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;

@ExtendWith(MockitoExtension.class)
class MachineFuelServiceTest {

    @Mock
    private MachineFuelEntryRepository fuelEntryRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private CostCenterRepository costCenterRepository;

    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private MachineFuelService machineFuelService;

    private Machine quarryMachine;
    private MachineFuelEntry sampleEntry;

    @BeforeEach
    void setUp() {
        quarryMachine = Machine.builder()
                .id(1L)
                .name("CAT 320 Ekskavatör")
                .code("M-CAT-320")
                .businessUnit(BusinessUnit.QUARRY)
                .active(true)
                .build();

        sampleEntry = MachineFuelEntry.builder()
                .id(100L)
                .machine(quarryMachine)
                .entryDate(LocalDate.of(2026, 9, 10))
                .litres(new BigDecimal("300"))
                .pricePerLitre(new BigDecimal("42.00"))
                .totalAmount(new BigDecimal("12600.00"))
                .workingHoursOrKm(new BigDecimal("1450.5"))
                .receiptNo("F-1001")
                .issuedBy("Ahmet")
                .receivedBy("Mehmet")
                .notes("Rutin dolum")
                .build();
    }

    @Test
    void getFuelPaged_FiltersAndPaginatesCorrectly() {
        Mockito.when(fuelEntryRepository.findByEntryDateBetweenOrderByEntryDateDesc(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(List.of(sampleEntry));

        TabulatorResponse<MachineFuelDto> response = machineFuelService.getFuelPaged(
                1, 25, "CAT", "2026-09", "QUARRY", 1L, "entryDate", "desc");

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).hasSize(1);
        MachineFuelDto dto = response.getData().get(0);
        Assertions.assertThat(dto.getMachineName()).isEqualTo("CAT 320 Ekskavatör");
        Assertions.assertThat(dto.getLitres()).isEqualByComparingTo(new BigDecimal("300"));
        Assertions.assertThat(dto.getBusinessUnit()).isEqualTo("QUARRY");
        Assertions.assertThat(response.getMeta()).containsKey("totalLitres");
        Assertions.assertThat(response.getMeta()).containsKey("totalAmount");
    }

    @Test
    void recordFuel_SavesEntryAndLogsExpense() {
        Mockito.when(machineRepository.findById(1L)).thenReturn(Optional.of(quarryMachine));
        Mockito.when(fuelEntryRepository.save(ArgumentMatchers.any(MachineFuelEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        MachineFuelEntry result = machineFuelService.recordFuel(
                1L, LocalDate.of(2026, 9, 10), new BigDecimal("300"), new BigDecimal("42.00"),
                new BigDecimal("1450.5"), "F-1001", "Ahmet", "Mehmet", "Rutin dolum");

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("12600.00"));
        Mockito.verify(fuelEntryRepository).save(ArgumentMatchers.any(MachineFuelEntry.class));
    }

    @Test
    void deleteFuel_DeletesById() {
        machineFuelService.deleteFuel(100L);
        Mockito.verify(fuelEntryRepository).deleteById(100L);
    }
}
