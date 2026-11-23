package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.StockMovementType;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.repository.StockMovementRepository;
import com.ozerler.marble.util.UniqueCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactoryInventoryService {

    public static final String FACTORY_FUEL_TANK_LOCATION_CODE = "FB-MZ-01";
    public static final String FACTORY_FUEL_TANK_ITEM_CODE = "FABRIKA-MAZOT-DEPO";
    public static final String FACTORY_CONSUMABLES_LOCATION_CODE = "FB-SRF-01";

    private final StockItemRepository stockItemRepository;
    private final com.ozerler.marble.mapper.MachineFuelMapper machineFuelMapper;
    private final com.ozerler.marble.mapper.MachineMapper machineMapper;
    private final com.ozerler.marble.mapper.StockItemMapper stockItemMapper;
    private final StockLocationRepository stockLocationRepository;
    private final MachineRepository machineRepository;
    private final MachineFuelEntryRepository fuelEntryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CostCenterRepository costCenterRepository;
    private final ExpenseService expenseService;

    // ─────────────────────────────────────────────────────────────
    // 1. FABRİKA MAZOT DEPOSU (FUEL INVENTORY)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StockItem getOrCreateFactoryFuelTankStockItem() {
        StockLocation tankLocation = getOrCreateLocation(
                FACTORY_FUEL_TANK_LOCATION_CODE,
                "Fabrika Mazot Deposu",
                StockLocationType.FACTORY_FUEL_TANK,
                BusinessUnit.FACTORY
        );

        return stockItemRepository.findByItemCode(FACTORY_FUEL_TANK_ITEM_CODE)
                .map(tank -> {
                    if (tank.getQuarryCategory() == null) {
                        tank.setQuarryCategory(QuarryCategory.MAZOT);
                        return stockItemRepository.save(tank);
                    }
                    return tank;
                })
                .orElseGet(() -> stockItemRepository.save(StockItem.builder()
                        .itemCode(FACTORY_FUEL_TANK_ITEM_CODE)
                        .description("Fabrika Mazot Deposu (Ana Tank)")
                        .productType(StockProductType.FUEL)
                        .quarryCategory(QuarryCategory.MAZOT)
                        .stockLocation(tankLocation)
                        .quantity(BigDecimal.ZERO)
                        .unit("litre")
                        .unitPrice(BigDecimal.ZERO)
                        .status("AVAILABLE")
                        .productionDate(LocalDate.now())
                        .build()));
    }

    @Transactional(readOnly = true)
    public BigDecimal getFuelStockLiters() {
        return stockItemRepository.findByItemCode(FACTORY_FUEL_TANK_ITEM_CODE)
                .map(StockItem::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public StockItem addFuelStock(BigDecimal litres, BigDecimal netUnitPrice, Invoice invoice, String notes) {
        if (litres == null || litres.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Eklenecek mazot miktarı 0'dan büyük olmalıdır.");
        }

        StockItem tank = getOrCreateFactoryFuelTankStockItem();
        StockItem saved = InventoryBalances.addIncomingFuel(tank, litres, netUnitPrice, stockItemRepository);

        String movCode = UniqueCodes.yearly("MOV-FB-MZ", stockMovementRepository::existsByMovementCode);
        String notesValue;
        if (notes != null) {
            notesValue = notes;
        } else {
            if (invoice != null) {
                notesValue = "Fatura: " + invoice.getInvoiceNo();
            } else {
                notesValue = "Fabrika Mazot Alımı";
            }
        }
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.INCOMING)
                .sourceDepartment(BusinessUnit.FACTORY)
                .targetDepartment(BusinessUnit.FACTORY)
                .toLocation(tank.getStockLocation())
                .stockItem(saved)
                .itemDescription("Fabrika Mazot Tankı Dolumu (" + litres + " L)")
                .quantity(litres)
                .quantityUnit("litre")
                .invoice(invoice)
                .notes(notesValue)
                .build();
        stockMovementRepository.save(movement);

        log.info("Fabrika Mazot Deposu dolumu yapıldı: +{} L. Yeni stok: {} L", litres, saved.getQuantity());
        return saved;
    }

    @Transactional
    public MachineFuelEntry dispenseFuel(Long machineId, BigDecimal litres, LocalDate entryDate,
                                         BigDecimal workingHoursOrKm, String receiptNo,
                                         String issuedBy, String receivedBy, String notes) {
        Objects.requireNonNull(machineId, "Araç / Makine seçimi zorunludur.");
        Objects.requireNonNull(litres, "Mazot miktarı zorunludur.");

        if (litres.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Çıkış yapılacak mazot miktarı 0'dan büyük olmalıdır.");
        }

        StockItem tank = getOrCreateFactoryFuelTankStockItem();
        BigDecimal currentStock;
        if (tank.getQuantity() != null) {
            currentStock = tank.getQuantity();
        } else {
            currentStock = BigDecimal.ZERO;
        }

        if (currentStock.compareTo(litres) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Yetersiz mazot stoğu! Fabrika Mazot Deposunda mevcut: %s litre, talep edilen: %s litre. Stok eksiye düşürülemez.",
                    currentStock, litres));
        }

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Araç / Makine bulunamadı: " + machineId));

        BigDecimal newStock = currentStock.subtract(litres);
        tank.setQuantity(newStock);
        stockItemRepository.save(tank);

        BigDecimal unitPrice;
        if (tank.getUnitPrice() != null && tank.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
            unitPrice = tank.getUnitPrice();
        } else {
            unitPrice = new BigDecimal("42.00");
        }
        InventoryBalances.SavedFuel savedFuel = InventoryBalances.recordFuelEntry(
                fuelEntryRepository,
                new InventoryBalances.FuelDraft(machine, entryDate, litres, unitPrice, workingHoursOrKm,
                        receiptNo, issuedBy, receivedBy, notes));
        MachineFuelEntry savedEntry = savedFuel.entry();
        BigDecimal totalNetCost = savedFuel.totalNetCost();

        CostCenter factoryCenter = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.FACTORY)
                .or(() -> costCenterRepository.findByCode("CC-002"))
                .orElse(null);

        if (factoryCenter != null && totalNetCost.compareTo(BigDecimal.ZERO) > 0) {
            String receiptNoValue;
            if (receiptNo != null) {
                receiptNoValue = receiptNo;
            } else {
                receiptNoValue = savedEntry.getId().toString();
            }
            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    factoryCenter.getId(),
                    ExpenseType.DIESEL,
                    null,
                    BusinessUnit.FACTORY,
                    totalNetCost,
                    "TRY",
                    receiptNoValue,
                    savedEntry.getEntryDate(),
                    savedEntry.getEntryDate(),
                    YearMonth.from(savedEntry.getEntryDate()).toString(),
                    YearMonth.now().toString(),
                    null, null, null, null,
                    machine,
                    null, null,
                    machine.getCode(),
                    "Fabrika Mazot Çıkışı: " + machine.getName() + " — " + litres + " L (Net: " + totalNetCost + " TL)"
            ));
        }

        String movCode = UniqueCodes.yearly("MOV-FB-MZ", stockMovementRepository::existsByMovementCode);
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.PRODUCTION_CONSUMPTION)
                .sourceDepartment(BusinessUnit.FACTORY)
                .fromLocation(tank.getStockLocation())
                .stockItem(tank)
                .itemDescription(machine.getName() + " Mazot İkmali")
                .quantity(litres)
                .quantityUnit("litre")
                .notes("Araç: " + machine.getName() + " (" + machine.getCode() + ") — " + litres + " L mazot verildi.")
                .build();
        stockMovementRepository.save(movement);

        log.info("Fabrika aracına mazot verildi: {} - {} L. Kalan fabrika mazot stoğu: {} L. Gider: {} TL",
                machine.getName(), litres, newStock, totalNetCost);

        return savedEntry;
    }

    @Transactional(readOnly = true)
    public List<Machine> getFactoryVehicles() {
        return BoundedReads.all(machineRepository).stream()
                .filter(m -> m.isActive() && m.getBusinessUnit() == BusinessUnit.FACTORY)
                .filter(m -> m.getMachineType() == null || !m.getMachineType().isElectric())
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MachineFuelEntry> getRecentFuelEntries() {
        return fuelEntryRepository.findAllByOrderByEntryDateDesc().stream()
                .filter(e -> e.getMachine() != null && e.getMachine().getBusinessUnit() == BusinessUnit.FACTORY)
                .limit(50)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // 2. FABRİKA SARF MALZEME DEPOSU (CONSUMABLES INVENTORY)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StockLocation getOrCreateFactoryConsumablesLocation() {
        return getOrCreateLocation(
                FACTORY_CONSUMABLES_LOCATION_CODE,
                "Fabrika Sarf Malzeme Deposu",
                StockLocationType.FACTORY_CONSUMABLES_WAREHOUSE,
                BusinessUnit.FACTORY
        );
    }

    @Transactional(readOnly = true)
    public List<StockItem> getAllFactoryConsumables() {
        return stockItemRepository.findByStockLocation_LocationTypeAndStatus(
                StockLocationType.FACTORY_CONSUMABLES_WAREHOUSE, "AVAILABLE");
    }

    @Transactional
    public StockItem createStockCard(String description, String unit, BigDecimal initialQuantity,
                                     BigDecimal netUnitPrice, String itemCode, String notes) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Stok kartı ürün tanımı / adı boş olamaz.");
        }
        String trimmedDesc = description.trim();
        String trimmedCode;
        if (itemCode != null && !itemCode.isBlank()) {
            trimmedCode = itemCode.trim().toUpperCase();
        } else {
            trimmedCode = null;
        }

        boolean descExists = stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(
                BusinessUnit.FACTORY, trimmedDesc);
        if (descExists) {
            throw new IllegalArgumentException("Bu ürün adıyla kayıtlı bir fabrika stok kartı zaten mevcut: " + trimmedDesc);
        }
        if (trimmedCode != null && stockItemRepository.existsByItemCode(trimmedCode)) {
            throw new IllegalArgumentException("Bu kodla kayıtlı bir stok kartı zaten mevcut: " + trimmedCode);
        }

        StockLocation loc = getOrCreateFactoryConsumablesLocation();
        String finalCode;
        if (trimmedCode != null) {
            finalCode = trimmedCode;
        } else {
            finalCode = UniqueCodes.yearly("FB-SRF", stockItemRepository::existsByItemCode);
        }
        BigDecimal qty;
        if (initialQuantity != null && initialQuantity.compareTo(BigDecimal.ZERO) >= 0) {
            qty = initialQuantity;
        } else {
            qty = BigDecimal.ZERO;
        }
        BigDecimal price;
        if (netUnitPrice != null && netUnitPrice.compareTo(BigDecimal.ZERO) >= 0) {
            price = netUnitPrice;
        } else {
            price = BigDecimal.ZERO;
        }
        String resolvedUnit;
        if (unit != null && !unit.isBlank()) {
            resolvedUnit = unit.trim().toLowerCase();
        } else {
            resolvedUnit = "adet";
        }

        StockItem item = StockItem.builder()
                .itemCode(finalCode)
                .description(trimmedDesc)
                .productType(StockProductType.CONSUMABLE)
                .stockLocation(loc)
                .quantity(qty)
                .unit(resolvedUnit)
                .unitPrice(price)
                .status("AVAILABLE")
                .productionDate(LocalDate.now())
                .notes(notes)
                .build();
        StockItem saved = stockItemRepository.save(item);

        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            String movCode = UniqueCodes.yearly("MOV-FB-OPN", stockMovementRepository::existsByMovementCode);
            StockMovement movement = StockMovement.builder()
                    .movementCode(movCode)
                    .movementDate(LocalDateTime.now())
                    .movementType(StockMovementType.INCOMING)
                    .sourceDepartment(BusinessUnit.FACTORY)
                    .targetDepartment(BusinessUnit.FACTORY)
                    .toLocation(loc)
                    .stockItem(saved)
                    .itemDescription("Yeni Fabrika Stok Kartı Açılışı: " + saved.getDescription())
                    .quantity(qty)
                    .quantityUnit(saved.getUnit())
                    .notes("Açılış fabrika stok kartı bakiyesi")
                    .build();
            stockMovementRepository.save(movement);
        }
        return saved;
    }

    @Transactional
    public StockItem addConsumableStock(String itemCode, String description, BigDecimal quantity,
                                        String unit, BigDecimal netUnitPrice, Invoice invoice, String notes) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Malzeme miktarı 0'dan büyük olmalıdır.");
        }
        StockLocation loc = getOrCreateFactoryConsumablesLocation();
        String code;
        if (itemCode != null && !itemCode.isBlank()) {
            code = itemCode.trim().toUpperCase();
        } else {
            code = UniqueCodes.yearly("FB-SRF", stockItemRepository::existsByItemCode);
        }

        StockItem saved = InventoryBalances.addIncomingConsumable(
                stockItemRepository, code, description, "Fabrika Sarf Malzemesi",
                loc, unit, netUnitPrice, notes, quantity);

        String movCode = UniqueCodes.yearly("MOV-FB-SRF", stockMovementRepository::existsByMovementCode);
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.INCOMING)
                .sourceDepartment(BusinessUnit.FACTORY)
                .targetDepartment(BusinessUnit.FACTORY)
                .toLocation(loc)
                .stockItem(saved)
                .itemDescription("Fabrika Sarf Girişi: " + saved.getDescription())
                .quantity(quantity)
                .quantityUnit(saved.getUnit())
                .invoice(invoice)
                .notes(Objects.requireNonNullElse(notes, "Fabrika sarf malzeme stoğa alındı"))
                .build();
        stockMovementRepository.save(movement);
        return saved;
    }

    @Transactional
    public StockItem consumeConsumable(Long stockItemId, BigDecimal quantity, Long machineId, String notes) {
        InventoryBalances.StockWithdrawal withdrawal = InventoryBalances.withdrawConsumable(
                stockItemRepository, machineRepository, stockItemId, quantity, machineId,
                "Yetersiz stok! Mevcut: %s %s, talep edilen: %s %s. Stok eksiye düşürülemez.");
        StockItem item = withdrawal.item();
        StockItem saved = item;
        BigDecimal netCost = withdrawal.netCost();
        Machine machine = withdrawal.machine();

        CostCenter factoryCenter = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.FACTORY)
                .or(() -> costCenterRepository.findByCode("CC-002"))
                .orElse(null);

        if (factoryCenter != null && netCost.compareTo(BigDecimal.ZERO) > 0) {
            String code;
            if (machine != null) {
                code = machine.getCode();
            } else {
                code = "FAB-SARF";
            }
            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    factoryCenter.getId(),
                    ExpenseType.CONSUMABLES,
                    null,
                    BusinessUnit.FACTORY,
                    netCost,
                    "TRY",
                    "FB-SRF-" + saved.getId(),
                    LocalDate.now(),
                    LocalDate.now(),
                    YearMonth.now().toString(),
                    YearMonth.now().toString(),
                    null, null, null, null,
                    machine,
                    null, null,
                    code,
                    "Fabrika Sarf Malzeme Kullanımı: " + item.getDescription() + " — " + quantity + " " + item.getUnit() + " (Net: " + netCost + " TL)"
            ));
        }

        String movCode = UniqueCodes.yearly("MOV-FB-SRF", stockMovementRepository::existsByMovementCode);
        String notesValue;
        if (notes != null) {
            notesValue = notes;
        } else {
            if (machine != null) {
                notesValue = machine.getName() + " için kullanıldı.";
            } else {
                notesValue = "Fabrikada kullanıldı.";
            }
        }
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.PRODUCTION_CONSUMPTION)
                .sourceDepartment(BusinessUnit.FACTORY)
                .fromLocation(item.getStockLocation())
                .stockItem(saved)
                .itemDescription("Fabrika Sarf Tüketimi: " + item.getDescription())
                .quantity(quantity)
                .quantityUnit(item.getUnit())
                .notes(notesValue)
                .build();
        stockMovementRepository.save(movement);

        log.info("Fabrika sarf malzemesi kullanıldı: {} (-{} {}). Gider: {} TL",
                item.getDescription(), quantity, item.getUnit(), netCost);

        return saved;
    }

    private StockLocation getOrCreateLocation(String code, String name, StockLocationType type, BusinessUnit unit) {
        return stockLocationRepository.findByCode(code)
                .orElseGet(() -> stockLocationRepository.save(StockLocation.builder()
                        .code(code)
                        .name(name)
                        .businessUnit(unit)
                        .locationType(type)
                        .active(true)
                        .build()));
    }

    /**
     * Same as {@code getOrCreateFactoryFuelTankStockItem}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView getOrCreateFactoryFuelTankStockItemView() {
        return stockItemMapper.toDto(getOrCreateFactoryFuelTankStockItem());
    }

    /**
     * Same as {@code addFuelStock}, copied into a view inside the transaction.
     *
     * @param litres see {@code addFuelStock}
     * @param netUnitPrice see {@code addFuelStock}
     * @param invoice see {@code addFuelStock}
     * @param notes see {@code addFuelStock}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView addFuelStockView(BigDecimal litres, BigDecimal netUnitPrice, Invoice invoice, String notes) {
        return stockItemMapper.toDto(addFuelStock(litres, netUnitPrice, invoice, notes));
    }

    /**
     * Same as {@code consumeConsumable}, copied into a view inside the transaction.
     *
     * @param stockItemId see {@code consumeConsumable}
     * @param quantity see {@code consumeConsumable}
     * @param machineId see {@code consumeConsumable}
     * @param notes see {@code consumeConsumable}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView consumeConsumableView(Long stockItemId, BigDecimal quantity, Long machineId, String notes) {
        return stockItemMapper.toDto(consumeConsumable(stockItemId, quantity, machineId, notes));
    }

    /**
     * Same as {@code createStockCard}, copied into a view inside the transaction.
     *
     * @param description see {@code createStockCard}
     * @param unit see {@code createStockCard}
     * @param initialQuantity see {@code createStockCard}
     * @param netUnitPrice see {@code createStockCard}
     * @param itemCode see {@code createStockCard}
     * @param notes see {@code createStockCard}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView createStockCardView(String description, String unit, BigDecimal initialQuantity,
                                     BigDecimal netUnitPrice, String itemCode, String notes) {
        return stockItemMapper.toDto(createStockCard(description, unit, initialQuantity, netUnitPrice, itemCode, notes));
    }

    /**
     * Same as {@code getRecentFuelEntries}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.MachineFuelEntryView> getRecentFuelEntryViews() {
        return getRecentFuelEntries().stream().map(machineFuelMapper::toDto).toList();
    }

    /**
     * Same as {@code getAllFactoryConsumables}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.StockItemView> getAllFactoryConsumablesViews() {
        return getAllFactoryConsumables().stream().map(stockItemMapper::toDto).toList();
    }

    /**
     * Same as {@code getFactoryVehicles}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.MachineRef> getFactoryVehiclesViews() {
        return getFactoryVehicles().stream().map(machineMapper::toDto).toList();
    }

    /**
     * Same as {@code dispenseFuel}, copied into a view inside the transaction.
     *
     * @param machineId see {@code dispenseFuel}
     * @param litres see {@code dispenseFuel}
     * @param entryDate see {@code dispenseFuel}
     * @param workingHoursOrKm see {@code dispenseFuel}
     * @param receiptNo see {@code dispenseFuel}
     * @param issuedBy see {@code dispenseFuel}
     * @param receivedBy see {@code dispenseFuel}
     * @param notes see {@code dispenseFuel}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.MachineFuelEntryView dispenseFuelView(Long machineId, BigDecimal litres, LocalDate entryDate,
                                         BigDecimal workingHoursOrKm, String receiptNo,
                                         String issuedBy, String receivedBy, String notes) {
        return machineFuelMapper.toDto(dispenseFuel(machineId, litres, entryDate, workingHoursOrKm, receiptNo, issuedBy, receivedBy, notes));
    }
}
