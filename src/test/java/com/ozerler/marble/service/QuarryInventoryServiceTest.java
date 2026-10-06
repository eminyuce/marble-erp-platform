package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuarryInventoryServiceTest {

    @Mock
    private StockItemRepository stockItemRepository;
    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private MachineRepository machineRepository;
    @Mock
    private MachineFuelEntryRepository fuelEntryRepository;
    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private ExpenseService expenseService;

    private QuarryInventoryService inventoryService;

    private StockLocation fuelLocation;
    private StockLocation consumablesLocation;

    @BeforeEach
    void setUp() {
        inventoryService = new QuarryInventoryService(
                stockItemRepository,
                stockLocationRepository,
                machineRepository,
                fuelEntryRepository,
                stockMovementRepository,
                costCenterRepository,
                expenseService
        );

        fuelLocation = StockLocation.builder()
                .id(101L)
                .code(QuarryInventoryService.FUEL_TANK_LOCATION_CODE)
                .name("Ocak Mazot Deposu")
                .locationType(StockLocationType.QUARRY_FUEL_TANK)
                .businessUnit(BusinessUnit.QUARRY)
                .build();

        consumablesLocation = StockLocation.builder()
                .id(102L)
                .code(QuarryInventoryService.CONSUMABLES_LOCATION_CODE)
                .name("Ocak Sarf Malzeme Deposu")
                .locationType(StockLocationType.QUARRY_CONSUMABLES_WAREHOUSE)
                .businessUnit(BusinessUnit.QUARRY)
                .build();
    }

    @Test
    @DisplayName("addFuelStock increments tank quantity and updates weighted average unit price")
    void addFuelStock_IncrementsQuantity() {
        StockItem tank = StockItem.builder()
                .id(1L)
                .itemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE)
                .productType(StockProductType.FUEL)
                .stockLocation(fuelLocation)
                .quantity(new BigDecimal("1000"))
                .unitPrice(new BigDecimal("40.00"))
                .build();

        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_FUEL_TANK))
                .thenReturn(Optional.of(fuelLocation));
        when(stockItemRepository.findByItemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE))
                .thenReturn(Optional.of(tank));
        when(stockItemRepository.save(any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        StockItem updated = inventoryService.addFuelStock(new BigDecimal("2000"), new BigDecimal("43.00"), "FAT-001");

        assertThat(updated.getQuantity()).isEqualByComparingTo("3000");
        assertThat(updated.getUnitPrice()).isEqualByComparingTo("42.00");
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("dispenseFuel deducts diesel stock, records machine entry and expense")
    void dispenseFuel_DeductsStockAndCreatesExpense() {
        StockItem tank = StockItem.builder()
                .id(1L)
                .itemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE)
                .productType(StockProductType.FUEL)
                .quarryCategory(QuarryCategory.MAZOT)
                .stockLocation(fuelLocation)
                .quantity(new BigDecimal("1500"))
                .unitPrice(new BigDecimal("40.00"))
                .build();

        Machine machine = Machine.builder()
                .id(5L)
                .code("OCK-EX-01")
                .name("Ekskavatör 1")
                .businessUnit(BusinessUnit.QUARRY)
                .build();

        CostCenter quarryCostCenter = CostCenter.builder()
                .id(1L)
                .code("CC-001")
                .name("Ocak Maliyet Merkezi")
                .businessUnit(BusinessUnit.QUARRY)
                .build();

        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_FUEL_TANK))
                .thenReturn(Optional.of(fuelLocation));
        when(stockItemRepository.findByItemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE))
                .thenReturn(Optional.of(tank));
        when(machineRepository.findById(5L)).thenReturn(Optional.of(machine));
        when(fuelEntryRepository.save(any(MachineFuelEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(costCenterRepository.findByCode("CC-001")).thenReturn(Optional.of(quarryCostCenter));

        MachineFuelEntry entry = inventoryService.dispenseFuel(
                5L, new BigDecimal("250"), LocalDate.now(), new BigDecimal("12.5"),
                "FIS-123", "Ahmet Usta", "Mehmet Şoför", "Rutin ikmal");

        assertThat(tank.getQuantity()).isEqualByComparingTo("1250");
        verify(fuelEntryRepository).save(any(MachineFuelEntry.class));
        verify(expenseService).recordExpense(any());
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("dispenseFuel throws exception when diesel quantity exceeds available stock")
    void dispenseFuel_InsufficientStock_ThrowsException() {
        StockItem tank = StockItem.builder()
                .id(1L)
                .itemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE)
                .productType(StockProductType.FUEL)
                .quarryCategory(QuarryCategory.MAZOT)
                .stockLocation(fuelLocation)
                .quantity(new BigDecimal("100"))
                .build();

        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_FUEL_TANK))
                .thenReturn(Optional.of(fuelLocation));
        when(stockItemRepository.findByItemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE))
                .thenReturn(Optional.of(tank));

        assertThatThrownBy(() -> inventoryService.dispenseFuel(5L, new BigDecimal("500"), LocalDate.now(), null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Yetersiz mazot stoğu");
    }

    @Test
    @DisplayName("consumeConsumable throws exception when quantity exceeds available stock")
    void consumeConsumable_InsufficientStock_ThrowsException() {
        StockItem item = StockItem.builder()
                .id(10L)
                .itemCode("SRF-01")
                .description("Elmas Tel")
                .quantity(new BigDecimal("5"))
                .unit("metre")
                .build();

        when(stockItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> inventoryService.consumeConsumable(10L, new BigDecimal("20"), null, "Test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Yetersiz stok");
    }

    @Test
    @DisplayName("createQuarryMachine saves machine with QUARRY business unit")
    void createQuarryMachine_SavesMachine() {
        when(machineRepository.save(any(Machine.class))).thenAnswer(inv -> inv.getArgument(0));

        Machine m = inventoryService.createQuarryMachine("OCK-MK-01", "Yeni Loder", "Ocak içi yükleme");

        assertThat(m.getCode()).isEqualTo("OCK-MK-01");
        assertThat(m.getName()).isEqualTo("Yeni Loder");
        assertThat(m.getBusinessUnit()).isEqualTo(BusinessUnit.QUARRY);
        assertThat(m.isActive()).isTrue();
    }

    @Test
    @DisplayName("createStockCard creates card under category and initializes opening stock")
    void createStockCard_Success() {
        when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Yağ Filtresi"))
                .thenReturn(false);
        when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_CONSUMABLES_WAREHOUSE))
                .thenReturn(Optional.of(consumablesLocation));
        when(stockItemRepository.save(any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        StockItem item = inventoryService.createStockCard(
                QuarryCategory.SARF_MALZEME, "Yağ Filtresi", "adet", new BigDecimal("10"), new BigDecimal("120.00"), null, "Açılış");

        assertThat(item.getQuarryCategory()).isEqualTo(QuarryCategory.SARF_MALZEME);
        assertThat(item.getDescription()).isEqualTo("Yağ Filtresi");
        assertThat(item.getQuantity()).isEqualByComparingTo("10");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("120.00");
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("createStockCard throws exception when duplicate product name exists in Quarry")
    void createStockCard_DuplicateName_ThrowsException() {
        when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Motorin"))
                .thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createStockCard(
                QuarryCategory.MAZOT, "Motorin", "litre", BigDecimal.ZERO, BigDecimal.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mükerrer stok kartı oluşturulamaz");
    }

    @Test
    @DisplayName("createStockCard throws exception when duplicate code exists")
    void createStockCard_DuplicateCode_ThrowsException() {
        when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Özel Kablo"))
                .thenReturn(false);
        when(stockItemRepository.existsByItemCode("ELK-999"))
                .thenReturn(true);

        assertThatThrownBy(() -> inventoryService.createStockCard(
                QuarryCategory.ELEKTRIK, "Özel Kablo", "metre", BigDecimal.ZERO, BigDecimal.ZERO, "ELK-999", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mükerrer stok kartı oluşturulamaz");
    }
}
