package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactoryInventoryService {

    public static final String FACTORY_FUEL_TANK_LOCATION_CODE = "FB-MZ-01";
    public static final String FACTORY_FUEL_TANK_ITEM_CODE = "FABRIKA-MAZOT-DEPO";
    public static final String FACTORY_CONSUMABLES_LOCATION_CODE = "FB-SRF-01";

    private final StockItemRepository stockItemRepository;
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
        BigDecimal oldQty = tank.getQuantity() != null ? tank.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQty = oldQty.add(litres);
        tank.setQuantity(newQty);

        if (netUnitPrice != null && netUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal oldCost = oldQty.multiply(tank.getUnitPrice() != null ? tank.getUnitPrice() : BigDecimal.ZERO);
            BigDecimal newCost = litres.multiply(netUnitPrice);
            BigDecimal avgPrice = (newCost.add(oldCost)).divide(newQty, 2, RoundingMode.HALF_UP);
            tank.setUnitPrice(avgPrice);
        }

        StockItem saved = stockItemRepository.save(tank);

        String movCode = UniqueCodes.yearly("MOV-FB-MZ", stockMovementRepository::existsByMovementCode);
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
                .notes(notes != null ? notes : (invoice != null ? "Fatura: " + invoice.getInvoiceNo() : "Fabrika Mazot Alımı"))
                .build();
        stockMovementRepository.save(movement);

        log.info("Fabrika Mazot Deposu dolumu yapıldı: +{} L. Yeni stok: {} L", litres, newQty);
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
        BigDecimal currentStock = tank.getQuantity() != null ? tank.getQuantity() : BigDecimal.ZERO;

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

        BigDecimal unitPrice = tank.getUnitPrice() != null && tank.getUnitPrice().compareTo(BigDecimal.ZERO) > 0
                ? tank.getUnitPrice()
                : new BigDecimal("42.00");
        BigDecimal totalNetCost = litres.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        MachineFuelEntry entry = MachineFuelEntry.builder()
                .machine(machine)
                .entryDate(entryDate != null ? entryDate : LocalDate.now())
                .litres(litres)
                .pricePerLitre(unitPrice)
                .workingHoursOrKm(workingHoursOrKm)
                .receiptNo(receiptNo)
                .issuedBy(issuedBy)
                .receivedBy(receivedBy)
                .notes(notes)
                .build();
        entry.calculateTotal();
        MachineFuelEntry savedEntry = fuelEntryRepository.save(entry);

        CostCenter factoryCenter = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.FACTORY)
                .or(() -> costCenterRepository.findByCode("CC-002"))
                .orElse(null);

        if (factoryCenter != null && totalNetCost.compareTo(BigDecimal.ZERO) > 0) {
            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    factoryCenter.getId(),
                    ExpenseType.DIESEL,
                    null,
                    BusinessUnit.FACTORY,
                    totalNetCost,
                    "TRY",
                    receiptNo != null ? receiptNo : savedEntry.getId().toString(),
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
        return machineRepository.findAll().stream()
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
        String trimmedCode = itemCode != null && !itemCode.isBlank() ? itemCode.trim().toUpperCase() : null;

        boolean descExists = stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(
                BusinessUnit.FACTORY, trimmedDesc);
        if (descExists) {
            throw new IllegalArgumentException("Bu ürün adıyla kayıtlı bir fabrika stok kartı zaten mevcut: " + trimmedDesc);
        }
        if (trimmedCode != null && stockItemRepository.existsByItemCode(trimmedCode)) {
            throw new IllegalArgumentException("Bu kodla kayıtlı bir stok kartı zaten mevcut: " + trimmedCode);
        }

        StockLocation loc = getOrCreateFactoryConsumablesLocation();
        String finalCode = trimmedCode != null ? trimmedCode : UniqueCodes.yearly("FB-SRF", stockItemRepository::existsByItemCode);
        BigDecimal qty = (initialQuantity != null && initialQuantity.compareTo(BigDecimal.ZERO) >= 0) ? initialQuantity : BigDecimal.ZERO;
        BigDecimal price = (netUnitPrice != null && netUnitPrice.compareTo(BigDecimal.ZERO) >= 0) ? netUnitPrice : BigDecimal.ZERO;
        String resolvedUnit = (unit != null && !unit.isBlank()) ? unit.trim().toLowerCase() : "adet";

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
        String code = (itemCode != null && !itemCode.isBlank())
                ? itemCode.trim().toUpperCase()
                : UniqueCodes.yearly("FB-SRF", stockItemRepository::existsByItemCode);

        StockItem item = stockItemRepository.findByItemCode(code)
                .orElseGet(() -> StockItem.builder()
                        .itemCode(code)
                        .description(description != null ? description.trim() : "Fabrika Sarf Malzemesi")
                        .productType(StockProductType.CONSUMABLE)
                        .stockLocation(loc)
                        .quantity(BigDecimal.ZERO)
                        .unit(unit != null && !unit.isBlank() ? unit.trim() : "adet")
                        .unitPrice(netUnitPrice != null ? netUnitPrice : BigDecimal.ZERO)
                        .status("AVAILABLE")
                        .productionDate(LocalDate.now())
                        .notes(notes)
                        .build());

        BigDecimal oldQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        BigDecimal newQty = oldQty.add(quantity);
        item.setQuantity(newQty);
        if (netUnitPrice != null && netUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
            item.setUnitPrice(netUnitPrice);
        }
        StockItem saved = stockItemRepository.save(item);

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
                .notes(notes != null ? notes : "Fabrika sarf malzeme stoğa alındı")
                .build();
        stockMovementRepository.save(movement);
        return saved;
    }

    @Transactional
    public StockItem consumeConsumable(Long stockItemId, BigDecimal quantity, Long machineId, String notes) {
        Objects.requireNonNull(stockItemId, "Sarf malzeme seçilmelidir.");
        Objects.requireNonNull(quantity, "Kullanılan miktar girilmelidir.");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kullanılan miktar 0'dan büyük olmalıdır.");
        }

        StockItem item = stockItemRepository.findById(stockItemId)
                .orElseThrow(() -> new IllegalArgumentException("Sarf malzeme bulunamadı: " + stockItemId));

        BigDecimal currentStock = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
        if (currentStock.compareTo(quantity) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Yetersiz stok! Mevcut: %s %s, talep edilen: %s %s. Stok eksiye düşürülemez.",
                    currentStock, item.getUnit(), quantity, item.getUnit()));
        }

        BigDecimal newStock = currentStock.subtract(quantity);
        item.setQuantity(newStock);
        StockItem saved = stockItemRepository.save(item);

        BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal netCost = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

        Machine machine = machineId != null ? machineRepository.findById(machineId).orElse(null) : null;

        CostCenter factoryCenter = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.FACTORY)
                .or(() -> costCenterRepository.findByCode("CC-002"))
                .orElse(null);

        if (factoryCenter != null && netCost.compareTo(BigDecimal.ZERO) > 0) {
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
                    machine != null ? machine.getCode() : "FAB-SARF",
                    "Fabrika Sarf Malzeme Kullanımı: " + item.getDescription() + " — " + quantity + " " + item.getUnit() + " (Net: " + netCost + " TL)"
            ));
        }

        String movCode = UniqueCodes.yearly("MOV-FB-SRF", stockMovementRepository::existsByMovementCode);
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
                .notes(notes != null ? notes : (machine != null ? machine.getName() + " için kullanıldı." : "Fabrikada kullanıldı."))
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
}
