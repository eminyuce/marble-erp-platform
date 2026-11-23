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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarryInventoryService {

    public static final String FUEL_TANK_ITEM_CODE = "OCAK-MAZOT-DEPO";
    public static final String FUEL_TANK_LOCATION_CODE = "OCK-MZ-01";
    public static final String CONSUMABLES_LOCATION_CODE = "OCK-SRF-01";

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
    // 1. MAZOT DEPOSU (FUEL INVENTORY)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StockItem getOrCreateFuelTankStockItem() {
        StockLocation tankLocation = getOrCreateLocation(
                FUEL_TANK_LOCATION_CODE,
                "Ocak Mazot Deposu",
                StockLocationType.QUARRY_FUEL_TANK,
                BusinessUnit.QUARRY
        );

        return stockItemRepository.findByItemCode(FUEL_TANK_ITEM_CODE)
                .map(tank -> {
                    if (tank.getQuarryCategory() == null) {
                        tank.setQuarryCategory(QuarryCategory.MAZOT);
                        return stockItemRepository.save(tank);
                    }
                    return tank;
                })
                .orElseGet(() -> stockItemRepository.save(StockItem.builder()
                        .itemCode(FUEL_TANK_ITEM_CODE)
                        .description("Ocak Mazot Deposu (Ana Tank)")
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
        return stockItemRepository.findByItemCode(FUEL_TANK_ITEM_CODE)
                .map(StockItem::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public StockItem addFuelStock(BigDecimal litres, BigDecimal netUnitPrice, Invoice invoice, String notes) {
        if (litres == null || litres.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Eklenecek mazot miktarı 0'dan büyük olmalıdır.");
        }

        StockItem tank = getOrCreateFuelTankStockItem();
        StockItem saved = InventoryBalances.addIncomingFuel(tank, litres, netUnitPrice, stockItemRepository);

        // Stok Giriş Hareketi (Gider yazılmaz, depoya giriş yapılır)
        String movCode = UniqueCodes.yearly("MOV-MZ", stockMovementRepository::existsByMovementCode);
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.INCOMING)
                .sourceDepartment(BusinessUnit.QUARRY)
                .targetDepartment(BusinessUnit.QUARRY)
                .toLocation(tank.getStockLocation())
                .stockItem(saved)
                .itemDescription("Ocak Mazot Tankı Dolumu (" + litres + " L)")
                .quantity(litres)
                .quantityUnit("litre")
                .invoice(invoice)
                .notes(notes != null ? notes : invoice != null ? "Fatura: " + invoice.getInvoiceNo() : "Mazot alımı")
                .build();
        stockMovementRepository.save(movement);

        log.info("Ocak Mazot Deposu dolumu yapıldı: +{} L. Yeni stok: {} L", litres, saved.getQuantity());
        return saved;
    }

    @Transactional
    public StockItem addFuelStock(BigDecimal litres, BigDecimal netUnitPrice, String invoiceNo) {
        return addFuelStock(litres, netUnitPrice, null, "Alış faturası: " + (invoiceNo != null ? invoiceNo : ""));
    }

    @Transactional
    public MachineFuelEntry dispenseFuel(Long machineId, BigDecimal litres, LocalDate entryDate,
                                         BigDecimal workingHoursOrKm, String receiptNo,
                                         String issuedBy, String receivedBy, String notes) {
        Objects.requireNonNull(machineId, "Makine seçimi zorunludur.");
        Objects.requireNonNull(litres, "Mazot miktarı zorunludur.");

        if (litres.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Çıkış yapılacak mazot miktarı 0'dan büyük olmalıdır.");
        }

        StockItem tank = getOrCreateFuelTankStockItem();
        BigDecimal currentStock = tank.getQuantity() != null ? tank.getQuantity() : BigDecimal.ZERO;

        if (currentStock.compareTo(litres) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Yetersiz mazot stoğu! Mazot Deposunda mevcut: %s litre, talep edilen: %s litre. Stok eksiye düşürülemez.",
                    currentStock, litres));
        }

        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("Makine bulunamadı: " + machineId));

        // 1. Mazot Deposu stok düşümü
        BigDecimal newStock = currentStock.subtract(litres);
        tank.setQuantity(newStock);
        stockItemRepository.save(tank);

        // 2. Birim Fiyat (KDV HARİÇ Net Fiyat)
        BigDecimal unitPrice = tank.getUnitPrice() != null && tank.getUnitPrice().compareTo(BigDecimal.ZERO) > 0
                ? tank.getUnitPrice()
                : new BigDecimal("40.00"); // varsayılan piyasa referansı
        InventoryBalances.SavedFuel savedFuel = InventoryBalances.recordFuelEntry(
                fuelEntryRepository,
                new InventoryBalances.FuelDraft(machine, entryDate, litres, unitPrice, workingHoursOrKm,
                        receiptNo, issuedBy, receivedBy, notes));
        MachineFuelEntry savedEntry = savedFuel.entry();
        BigDecimal totalNetCost = savedFuel.totalNetCost();

        // 4. Maliyet / Gider Oluşturma (Yalnızca tüketildiğinde ve KDV HARİÇ)
        CostCenter quarryCenter = costCenterRepository.findByCode("CC-001")
                .or(() -> costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.QUARRY))
                .or(() -> BoundedReads.all(costCenterRepository).stream().findFirst())
                .orElse(null);

        if (quarryCenter != null) {
            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    quarryCenter.getId(),
                    ExpenseType.DIESEL,
                    null,
                    BusinessUnit.QUARRY,
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
                    "Mazot Çıkışı: " + machine.getName() + " — " + litres + " L (Net: " + totalNetCost + " TL)"
            ));
        }

        // 5. Stok Hareketi (Tüketim Çıkışı)
        String movCode = UniqueCodes.yearly("MOV-MZ", stockMovementRepository::existsByMovementCode);
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.PRODUCTION_CONSUMPTION)
                .sourceDepartment(BusinessUnit.QUARRY)
                .fromLocation(tank.getStockLocation())
                .stockItem(tank)
                .itemDescription(machine.getName() + " Mazot İkmali")
                .quantity(litres)
                .quantityUnit("litre")
                .notes("Makine: " + machine.getName() + " (" + machine.getCode() + ") — " + litres + " L mazot verildi.")
                .build();
        stockMovementRepository.save(movement);

        log.info("Makineye mazot verildi: {} - {} L. Kalan mazot stoğu: {} L. Gider: {} TL",
                machine.getName(), litres, newStock, totalNetCost);

        return savedEntry;
    }

    // ─────────────────────────────────────────────────────────────
    // 2. SARF MALZEME DEPOSU (CONSUMABLES INVENTORY)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StockLocation getOrCreateConsumablesLocation() {
        return getOrCreateLocation(
                CONSUMABLES_LOCATION_CODE,
                "Ocak Sarf Malzeme Deposu",
                StockLocationType.QUARRY_CONSUMABLES_WAREHOUSE,
                BusinessUnit.QUARRY
        );
    }

    @Transactional(readOnly = true)
    public List<StockItem> getConsumableStockItems() {
        return stockItemRepository.findByStockLocation_LocationTypeAndStatus(
                StockLocationType.QUARRY_CONSUMABLES_WAREHOUSE, "AVAILABLE");
    }

    @Transactional
    public StockItem addConsumableStock(String itemCode, String description, BigDecimal quantity,
                                       String unit, BigDecimal netUnitPrice, Invoice invoice, String notes) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Malzeme miktarı 0'dan büyük olmalıdır.");
        }

        StockLocation loc = getOrCreateConsumablesLocation();
        String code = (itemCode != null && !itemCode.isBlank())
                ? itemCode.trim().toUpperCase()
                : UniqueCodes.yearly("SRF", stockItemRepository::existsByItemCode);

        StockItem saved = InventoryBalances.addIncomingConsumable(
                stockItemRepository, code, description, "Ocak Sarf Malzemesi",
                loc, unit, netUnitPrice, notes, quantity);

        // Stok Giriş Hareketi (Gider henüz oluşturulmaz)
        String movCode = UniqueCodes.yearly("MOV-SRF", stockMovementRepository::existsByMovementCode);
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.INCOMING)
                .sourceDepartment(BusinessUnit.QUARRY)
                .targetDepartment(BusinessUnit.QUARRY)
                .toLocation(loc)
                .stockItem(saved)
                .itemDescription("Sarf Malzeme Girişi: " + saved.getDescription())
                .quantity(quantity)
                .quantityUnit(saved.getUnit())
                .invoice(invoice)
                .notes(notes != null ? notes : "Sarf malzeme stoğa alındı")
                .build();
        stockMovementRepository.save(movement);

        log.info("Sarf malzeme stoğa alındı: {} (+{} {}). Yeni stok: {}", saved.getDescription(), quantity, saved.getUnit(), saved.getQuantity());
        return saved;
    }

    @Transactional
    public StockItem addConsumableStock(String description, BigDecimal quantity, String unit, BigDecimal netUnitPrice, String invoiceNo) {
        return addConsumableStock(null, description, quantity, unit, netUnitPrice, null, "Alış faturası: " + (invoiceNo != null ? invoiceNo : ""));
    }

    // ─────────────────────────────────────────────────────────────
    // 2.B. STOK KARTI YÖNETİMİ & MÜKERRER KAYIT ENGELLEME (MADDE 1 & 2)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StockItem createStockCard(QuarryCategory category, String description, String unit,
                                     BigDecimal initialQuantity, BigDecimal netUnitPrice,
                                     String itemCode, Boolean isExpense, String notes) {
        Objects.requireNonNull(category, "Kategori seçimi zorunludur.");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Stok kartı ürün tanımı / adı boş olamaz.");
        }

        String trimmedDesc = description.trim();
        String trimmedCode = itemCode != null && !itemCode.isBlank() ? itemCode.trim().toUpperCase() : null;

        // Duplicate kontrolü (Madde 2): Ocak stoğunda aynı isimde veya aynı kodda kayıt var mı?
        boolean descExists = stockItemRepository.existsByStockLocation_BusinessUnitAndDescriptionIgnoreCase(
                BusinessUnit.QUARRY, trimmedDesc);
        if (descExists) {
            throw new IllegalArgumentException(String.format(
                    "Bu ürün adıyla kayıtlı bir stok kartı zaten mevcut: '%s'. Mükerrer stok kartı oluşturulamaz.", trimmedDesc));
        }

        if (trimmedCode != null && stockItemRepository.existsByItemCode(trimmedCode)) {
            throw new IllegalArgumentException(String.format(
                    "Bu kodla kayıtlı bir stok kartı zaten mevcut: '%s'. Mükerrer stok kartı oluşturulamaz.", trimmedCode));
        }

        StockLocation location;
        StockProductType productType;
        String defaultPrefix;

        switch (category) {
            case MAZOT -> {
                location = getOrCreateLocation(FUEL_TANK_LOCATION_CODE, "Ocak Mazot Deposu", StockLocationType.QUARRY_FUEL_TANK, BusinessUnit.QUARRY);
                productType = StockProductType.FUEL;
                defaultPrefix = "MZ";
            }
            case SARF_MALZEME -> {
                location = getOrCreateConsumablesLocation();
                productType = StockProductType.CONSUMABLE;
                defaultPrefix = "SRF";
            }
            case ELEKTRIK -> {
                location = getOrCreateConsumablesLocation();
                productType = StockProductType.OTHER;
                defaultPrefix = "ELK";
            }
            default -> {
                location = getOrCreateConsumablesLocation();
                productType = StockProductType.OTHER;
                defaultPrefix = "DGR";
            }
        }

        String finalCode = trimmedCode != null
                ? trimmedCode
                : UniqueCodes.yearly(defaultPrefix, stockItemRepository::existsByItemCode);

        BigDecimal qty = (initialQuantity != null && initialQuantity.compareTo(BigDecimal.ZERO) >= 0)
                ? initialQuantity
                : BigDecimal.ZERO;

        BigDecimal price = (netUnitPrice != null && netUnitPrice.compareTo(BigDecimal.ZERO) >= 0)
                ? netUnitPrice
                : BigDecimal.ZERO;

        // Birim Tanımlamaları kuralı: Mazot alımlarında birim litre olmalıdır.
        String resolvedUnit;
        if (category == QuarryCategory.MAZOT) {
            resolvedUnit = "litre";
        } else if (unit != null && !unit.isBlank()) {
            resolvedUnit = unit.trim().toLowerCase();
        } else {
            resolvedUnit = "adet";
        }

        StockItem item = StockItem.builder()
                .itemCode(finalCode)
                .description(trimmedDesc)
                .productType(productType)
                .quarryCategory(category)
                .stockLocation(location)
                .quantity(qty)
                .unit(resolvedUnit)
                .unitPrice(price)
                .directExpense(Boolean.TRUE.equals(isExpense) || category == QuarryCategory.ELEKTRIK)
                .status("AVAILABLE")
                .productionDate(LocalDate.now())
                .notes(notes)
                .build();

        StockItem saved = stockItemRepository.save(item);

        if (qty.compareTo(BigDecimal.ZERO) > 0) {
            String movCode = UniqueCodes.yearly("MOV-OPN", stockMovementRepository::existsByMovementCode);
            StockMovement movement = StockMovement.builder()
                    .movementCode(movCode)
                    .movementDate(LocalDateTime.now())
                    .movementType(StockMovementType.INCOMING)
                    .sourceDepartment(BusinessUnit.QUARRY)
                    .targetDepartment(BusinessUnit.QUARRY)
                    .toLocation(location)
                    .stockItem(saved)
                    .itemDescription("Yeni Stok Kartı Açılışı: " + saved.getDescription())
                    .quantity(qty)
                    .quantityUnit(saved.getUnit())
                    .notes("Açılış stok kartı bakiyesi")
                    .build();
            stockMovementRepository.save(movement);
        }

        log.info("Yeni Ocak stok kartı oluşturuldu: {} [{}] - Kategori: {}, Gider: {}",
                saved.getDescription(), saved.getItemCode(), category, saved.getDirectExpense());
        return saved;
    }

    @Transactional
    public StockItem createStockCard(QuarryCategory category, String description, String unit,
                                     BigDecimal initialQuantity, BigDecimal netUnitPrice,
                                     String itemCode, String notes) {
        return createStockCard(category, description, unit, initialQuantity, netUnitPrice, itemCode, false, notes);
    }

    @Transactional(readOnly = true)
    public List<StockItem> getAllQuarryStockCards(QuarryCategory category) {
        List<StockItem> list = stockItemRepository.findByStockLocation_BusinessUnitAndStatus(BusinessUnit.QUARRY, "AVAILABLE");
        if (category == null) {
            return list;
        }
        return list.stream()
                .filter(item -> item.getQuarryCategory() == category)
                .toList();
    }

    @Transactional
    public StockItem consumeConsumable(Long stockItemId, BigDecimal quantity, Long machineId, String notes) {
        InventoryBalances.StockWithdrawal withdrawal = InventoryBalances.withdrawConsumable(
                stockItemRepository, machineRepository, stockItemId, quantity, machineId,
                "Yetersiz stok! Mevcut stok: %s %s, talep edilen: %s %s. Stok eksiye düşürülemez.");
        // Sıfır adet kart listede kalır; kayıt silinmez.
        StockItem item = withdrawal.item();
        StockItem saved = item;
        BigDecimal netCost = withdrawal.netCost();
        Machine machine = withdrawal.machine();

        // Maliyet / Gider Kaydı
        CostCenter quarryCenter = costCenterRepository.findByCode("CC-001")
                .or(() -> costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.QUARRY))
                .or(() -> BoundedReads.all(costCenterRepository).stream().findFirst())
                .orElse(null);

        if (quarryCenter != null && netCost.compareTo(BigDecimal.ZERO) > 0) {
            expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                    quarryCenter.getId(),
                    ExpenseType.CONSUMABLES,
                    null,
                    BusinessUnit.QUARRY,
                    netCost,
                    "TRY",
                    "SRF-" + saved.getId(),
                    LocalDate.now(),
                    LocalDate.now(),
                    YearMonth.now().toString(),
                    YearMonth.now().toString(),
                    null, null, null, null,
                    machine,
                    null, null,
                    machine != null ? machine.getCode() : "OCAK-SARF",
                    "Sarf Malzeme Kullanımı: " + item.getDescription() + " — " + quantity + " " + item.getUnit() + " (Net: " + netCost + " TL)"
            ));
        }

        // Stok Hareketi
        String movCode = UniqueCodes.yearly("MOV-SRF", stockMovementRepository::existsByMovementCode);
        StockMovement movement = StockMovement.builder()
                .movementCode(movCode)
                .movementDate(LocalDateTime.now())
                .movementType(StockMovementType.PRODUCTION_CONSUMPTION)
                .sourceDepartment(BusinessUnit.QUARRY)
                .fromLocation(item.getStockLocation())
                .stockItem(saved)
                .itemDescription("Sarf Malzeme Tüketimi: " + item.getDescription())
                .quantity(quantity)
                .quantityUnit(item.getUnit())
                .notes(notes != null ? notes : machine != null ? machine.getName() + " için kullanıldı." : "Ocak sahasında kullanıldı.")
                .build();
        stockMovementRepository.save(movement);

        log.info("Sarf malzeme kullanıldı: {} - {} {}. Kalan stok: {}. Net gider: {} TL",
                item.getDescription(), quantity, item.getUnit(), item.getQuantity(), netCost);

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    // 3. MAKİNE TANIMLAMA (QUARRY MACHINES)
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Machine> getQuarryMachines() {
        return machineRepository.findByBusinessUnitAndActiveTrueOrderByNameAsc(BusinessUnit.QUARRY);
    }

    @Transactional
    public Machine createQuarryMachine(String code, String name, String notes) {
        Objects.requireNonNull(name, "Makine adı zorunludur.");
        String c = (code != null && !code.isBlank())
                ? code.trim().toUpperCase()
                : UniqueCodes.yearly("OCK-MK", codeStr -> machineRepository.findByCode(codeStr).isPresent());

        Machine m = Machine.builder()
                .code(c)
                .name(name.trim())
                .businessUnit(BusinessUnit.QUARRY)
                .machineType(MachineType.QUARRY_MACHINE)
                .active(true)
                .notes(notes)
                .build();

        return machineRepository.save(m);
    }

    @Transactional(readOnly = true)
    public List<MachineFuelEntry> getRecentFuelEntries() {
        return fuelEntryRepository.findAllByOrderByEntryDateDesc();
    }

    @Transactional(readOnly = true)
    public List<StockMovement> getRecentQuarryMovements() {
        return stockMovementRepository.findRecentBySourceDepartment(BusinessUnit.QUARRY, LocalDateTime.now().minusDays(30));
    }

    // ─────────────────────────────────────────────────────────────
    // YARDIMCI METODLAR
    // ─────────────────────────────────────────────────────────────

    private StockLocation getOrCreateLocation(String code, String name, StockLocationType type, BusinessUnit unit) {
        return stockLocationRepository.findByLocationTypeAndActiveTrue(type)
                .or(() -> stockLocationRepository.findByCode(code))
                .orElseGet(() -> stockLocationRepository.save(StockLocation.builder()
                        .code(code)
                        .name(name)
                        .businessUnit(unit)
                        .locationType(type)
                        .active(true)
                        .build()));
    }

    /**
     * Same as {@code getOrCreateFuelTankStockItem}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView getOrCreateFuelTankStockItemView() {
        return stockItemMapper.toDto(getOrCreateFuelTankStockItem());
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
     * Same as {@code addFuelStock}, copied into a view inside the transaction.
     *
     * @param litres see {@code addFuelStock}
     * @param netUnitPrice see {@code addFuelStock}
     * @param invoiceNo see {@code addFuelStock}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView addFuelStockView(BigDecimal litres, BigDecimal netUnitPrice, String invoiceNo) {
        return stockItemMapper.toDto(addFuelStock(litres, netUnitPrice, invoiceNo));
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
     * @param category see {@code createStockCard}
     * @param description see {@code createStockCard}
     * @param unit see {@code createStockCard}
     * @param initialQuantity see {@code createStockCard}
     * @param netUnitPrice see {@code createStockCard}
     * @param itemCode see {@code createStockCard}
     * @param isExpense see {@code createStockCard}
     * @param notes see {@code createStockCard}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView createStockCardView(QuarryCategory category, String description, String unit,
                                     BigDecimal initialQuantity, BigDecimal netUnitPrice,
                                     String itemCode, Boolean isExpense, String notes) {
        return stockItemMapper.toDto(createStockCard(category, description, unit, initialQuantity, netUnitPrice, itemCode, isExpense, notes));
    }

    /**
     * Same as {@code createStockCard}, copied into a view inside the transaction.
     *
     * @param category see {@code createStockCard}
     * @param description see {@code createStockCard}
     * @param unit see {@code createStockCard}
     * @param initialQuantity see {@code createStockCard}
     * @param netUnitPrice see {@code createStockCard}
     * @param itemCode see {@code createStockCard}
     * @param notes see {@code createStockCard}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView createStockCardView(QuarryCategory category, String description, String unit,
                                     BigDecimal initialQuantity, BigDecimal netUnitPrice,
                                     String itemCode, String notes) {
        return stockItemMapper.toDto(createStockCard(category, description, unit, initialQuantity, netUnitPrice, itemCode, notes));
    }

    /**
     * Same as {@code getAllQuarryStockCards}, copied into a view inside the transaction.
     *
     * @param category see {@code getAllQuarryStockCards}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.StockItemView> getAllQuarryStockCardsViews(QuarryCategory category) {
        return getAllQuarryStockCards(category).stream().map(stockItemMapper::toDto).toList();
    }

    /**
     * Same as {@code getQuarryMachines}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.MachineRef> getQuarryMachinesViews() {
        return getQuarryMachines().stream().map(machineMapper::toDto).toList();
    }

    /**
     * Same as {@code createQuarryMachine}, copied into a view inside the transaction.
     *
     * @param code see {@code createQuarryMachine}
     * @param name see {@code createQuarryMachine}
     * @param notes see {@code createQuarryMachine}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.MachineRef createQuarryMachineView(String code, String name, String notes) {
        return machineMapper.toDto(createQuarryMachine(code, name, notes));
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
