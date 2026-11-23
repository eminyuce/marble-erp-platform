package com.ozerler.marble.service;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.repository.StockMovementRepository;

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
                null, null, null,
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

        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_FUEL_TANK))
                .thenReturn(Optional.of(fuelLocation));
        Mockito.when(stockItemRepository.findByItemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE))
                .thenReturn(Optional.of(tank));
        Mockito.when(stockItemRepository.save(ArgumentMatchers.any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        StockItem updated = inventoryService.addFuelStock(new BigDecimal("2000"), new BigDecimal("43.00"), "FAT-001");

        Assertions.assertThat(updated.getQuantity()).isEqualByComparingTo("3000");
        Assertions.assertThat(updated.getUnitPrice()).isEqualByComparingTo("42.00");
        Mockito.verify(stockMovementRepository).save(ArgumentMatchers.any(StockMovement.class));
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

        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_FUEL_TANK))
                .thenReturn(Optional.of(fuelLocation));
        Mockito.when(stockItemRepository.findByItemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE))
                .thenReturn(Optional.of(tank));
        Mockito.when(machineRepository.findById(5L)).thenReturn(Optional.of(machine));
        Mockito.when(fuelEntryRepository.save(ArgumentMatchers.any(MachineFuelEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(costCenterRepository.findByCode("CC-001")).thenReturn(Optional.of(quarryCostCenter));

        MachineFuelEntry entry = inventoryService.dispenseFuel(
                5L, new BigDecimal("250"), LocalDate.now(), new BigDecimal("12.5"),
                "FIS-123", "Ahmet Usta", "Mehmet Şoför", "Rutin ikmal");

        Assertions.assertThat(tank.getQuantity()).isEqualByComparingTo("1250");
        Mockito.verify(fuelEntryRepository).save(ArgumentMatchers.any(MachineFuelEntry.class));
        Mockito.verify(expenseService).recordExpense(ArgumentMatchers.any());
        Mockito.verify(stockMovementRepository).save(ArgumentMatchers.any(StockMovement.class));
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

        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_FUEL_TANK))
                .thenReturn(Optional.of(fuelLocation));
        Mockito.when(stockItemRepository.findByItemCode(QuarryInventoryService.FUEL_TANK_ITEM_CODE))
                .thenReturn(Optional.of(tank));

        Assertions.assertThatThrownBy(() -> inventoryService.dispenseFuel(5L, new BigDecimal("500"), LocalDate.now(), null, null, null, null, null))
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

        Mockito.when(stockItemRepository.findById(10L)).thenReturn(Optional.of(item));

        Assertions.assertThatThrownBy(() -> inventoryService.consumeConsumable(10L, new BigDecimal("20"), null, "Test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Yetersiz stok");
    }

    @Test
    @DisplayName("createQuarryMachine saves machine with QUARRY business unit")
    void createQuarryMachine_SavesMachine() {
        Mockito.when(machineRepository.save(ArgumentMatchers.any(Machine.class))).thenAnswer(inv -> inv.getArgument(0));

        Machine m = inventoryService.createQuarryMachine("OCK-MK-01", "Yeni Loder", "Ocak içi yükleme");

        Assertions.assertThat(m.getCode()).isEqualTo("OCK-MK-01");
        Assertions.assertThat(m.getName()).isEqualTo("Yeni Loder");
        Assertions.assertThat(m.getBusinessUnit()).isEqualTo(BusinessUnit.QUARRY);
        Assertions.assertThat(m.isActive()).isTrue();
    }

    @Test
    @DisplayName("createStockCard creates card under category and initializes opening stock")
    void createStockCard_Success() {
        Mockito.when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Yağ Filtresi"))
                .thenReturn(false);
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_CONSUMABLES_WAREHOUSE))
                .thenReturn(Optional.of(consumablesLocation));
        Mockito.when(stockItemRepository.save(ArgumentMatchers.any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        StockItem item = inventoryService.createStockCard(
                QuarryCategory.SARF_MALZEME, "Yağ Filtresi", "adet", new BigDecimal("10"), new BigDecimal("120.00"), null, "Açılış");

        Assertions.assertThat(item.getQuarryCategory()).isEqualTo(QuarryCategory.SARF_MALZEME);
        Assertions.assertThat(item.getDescription()).isEqualTo("Yağ Filtresi");
        Assertions.assertThat(item.getQuantity()).isEqualByComparingTo("10");
        Assertions.assertThat(item.getUnitPrice()).isEqualByComparingTo("120.00");
        Mockito.verify(stockMovementRepository).save(ArgumentMatchers.any(StockMovement.class));
    }

    @Test
    @DisplayName("createStockCard throws exception when duplicate product name exists in Quarry")
    void createStockCard_DuplicateName_ThrowsException() {
        Mockito.when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Motorin"))
                .thenReturn(true);

        Assertions.assertThatThrownBy(() -> inventoryService.createStockCard(
                QuarryCategory.MAZOT, "Motorin", "litre", BigDecimal.ZERO, BigDecimal.ZERO, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mükerrer stok kartı oluşturulamaz");
    }

    @Test
    @DisplayName("createStockCard with direct expense marks directExpense true")
    void createStockCard_WithDirectExpense_SetsDirectExpenseTrue() {
        Mockito.when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Danışmanlık Hizmeti"))
                .thenReturn(false);
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_CONSUMABLES_WAREHOUSE))
                .thenReturn(Optional.of(consumablesLocation));
        Mockito.when(stockItemRepository.save(ArgumentMatchers.any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        StockItem item = inventoryService.createStockCard(
                QuarryCategory.DIGER, "Danışmanlık Hizmeti", "adet", BigDecimal.ZERO, new BigDecimal("500.00"), null, true, null);

        Assertions.assertThat(item.getDirectExpense()).isTrue();
        Assertions.assertThat(item.getQuarryCategory()).isEqualTo(QuarryCategory.DIGER);
    }

    @Test
    @DisplayName("createStockCard with MAZOT forces unit to litre")
    void createStockCard_Mazot_EnforcesLitreUnit() {
        Mockito.when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Kışlık Motorin"))
                .thenReturn(false);
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_FUEL_TANK))
                .thenReturn(Optional.of(fuelLocation));
        Mockito.when(stockItemRepository.save(ArgumentMatchers.any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        StockItem item = inventoryService.createStockCard(
                QuarryCategory.MAZOT, "Kışlık Motorin", "varil", BigDecimal.ZERO, new BigDecimal("44.00"), null, false, null);

        Assertions.assertThat(item.getUnit()).isEqualTo("litre");
        Assertions.assertThat(item.getDirectExpense()).isFalse();
    }

    @Test
    @DisplayName("createStockCard with ELEKTRIK defaults directExpense to true")
    void createStockCard_Elektrik_DefaultsToDirectExpense() {
        Mockito.when(stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(BusinessUnit.QUARRY, "Trafo Elektrik Bedeli"))
                .thenReturn(false);
        Mockito.when(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.QUARRY_CONSUMABLES_WAREHOUSE))
                .thenReturn(Optional.of(consumablesLocation));
        Mockito.when(stockItemRepository.save(ArgumentMatchers.any(StockItem.class))).thenAnswer(inv -> inv.getArgument(0));

        StockItem item = inventoryService.createStockCard(
                QuarryCategory.ELEKTRIK, "Trafo Elektrik Bedeli", "kWh", BigDecimal.ZERO, new BigDecimal("4.50"), null, false, null);

        Assertions.assertThat(item.getDirectExpense()).isTrue();
    }
}
