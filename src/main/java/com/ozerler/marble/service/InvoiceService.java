package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BlockRepository blockRepository;
    private final StockItemRepository stockItemRepository;
    private final SlabRepository slabRepository;
    private final ProjectRepository projectRepository;
    private final StockLocationRepository stockLocationRepository;
    private final BlockLocationMovementRepository movementRepository;
    private final CostTransactionRepository costTransactionRepository;
    private final CostCenterRepository costCenterRepository;
    private final StockMovementRepository stockMovementRepository;
    private final QuarryInventoryService quarryInventoryService;

    public record InvoiceItemForm(String productName, String description, BigDecimal quantity, String unit,
                                  BigDecimal unitPrice, Long blockId, Long slabId, Long stockItemId,
                                  BigDecimal widthCm, BigDecimal calculatedM2) {
        public InvoiceItemForm(String productName, String description, BigDecimal quantity, String unit, BigDecimal unitPrice, Long blockId, Long stockItemId) {
            this(productName, description, quantity, unit, unitPrice, blockId, null, stockItemId, null, null);
        }
    }

    @Transactional
    public Invoice createInvoice(String invoiceNo, LocalDate invoiceDate, LocalDate dueDate,
                                 InvoiceType invoiceType, BusinessUnit department, Long customerId,
                                 Long supplierId, BusinessUnit targetDepartment, Long projectId,
                                 BigDecimal taxRate, Boolean directExpense, String partyName, String notes,
                                 List<InvoiceItemForm> itemForms) {

        Objects.requireNonNull(invoiceType, "Fatura tipi seçilmelidir.");
        Objects.requireNonNull(department, "Departman seçilmelidir.");

        String no = (invoiceNo != null && !invoiceNo.isBlank())
                ? invoiceNo.trim().toUpperCase()
                : UniqueCodes.yearly(invoiceType == InvoiceType.PURCHASE ? "ALF" : "STF", invoiceRepository::existsByInvoiceNo);

        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        Supplier supplier = supplierId != null ? supplierRepository.findById(supplierId).orElse(null) : null;
        Project project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;

        String resolvedParty = partyName;
        if (resolvedParty == null || resolvedParty.isBlank()) {
            if (targetDepartment != null) {
                resolvedParty = "Dahili Transfer: " + targetDepartment.getDisplayName();
            } else if (customer != null) {
                resolvedParty = customer.getCompanyName();
            } else if (supplier != null) {
                resolvedParty = supplier.getCompanyName();
            } else {
                resolvedParty = "—";
            }
        }

        BigDecimal effectiveTaxRate = taxRate != null ? taxRate : new BigDecimal("20.00");

        Invoice invoice = Invoice.builder()
                .invoiceNo(no)
                .invoiceDate(invoiceDate != null ? invoiceDate : LocalDate.now())
                .dueDate(dueDate)
                .invoiceType(invoiceType)
                .department(department)
                .targetDepartment(targetDepartment)
                .customer(customer)
                .supplier(supplier)
                .project(project)
                .partyName(resolvedParty)
                .taxRate(effectiveTaxRate)
                .status(InvoiceStatus.ISSUED)
                .notes(notes)
                .items(new ArrayList<>())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        if (itemForms != null && !itemForms.isEmpty()) {
            for (InvoiceItemForm f : itemForms) {
                if (f.productName() == null || f.productName().isBlank()) continue;
                BigDecimal qty = f.quantity() != null ? f.quantity() : BigDecimal.ONE;
                BigDecimal price = f.unitPrice() != null ? f.unitPrice() : BigDecimal.ZERO;
                BigDecimal lineTot = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);

                Block block = f.blockId() != null ? blockRepository.findById(f.blockId()).orElse(null) : null;
                Slab slab = f.slabId() != null ? slabRepository.findById(f.slabId()).orElse(null) : null;
                StockItem stockItem = f.stockItemId() != null ? stockItemRepository.findById(f.stockItemId()).orElse(null) : null;

                BigDecimal width = f.widthCm();
                BigDecimal calcM2 = f.calculatedM2();
                String unit = f.unit() != null && !f.unit().isBlank() ? f.unit() : "m2";
                boolean isRunningMeter = unit.equalsIgnoreCase("m.t.") || unit.equalsIgnoreCase("metretül") || unit.equalsIgnoreCase("mt");

                if (isRunningMeter && width != null && width.compareTo(BigDecimal.ZERO) > 0) {
                    calcM2 = qty.multiply(width).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                } else if ("m2".equalsIgnoreCase(unit) || "m²".equalsIgnoreCase(unit)) {
                    calcM2 = qty;
                }

                InvoiceItem item = InvoiceItem.builder()
                        .invoice(invoice)
                        .productName(f.productName().trim())
                        .description(f.description())
                        .quantity(qty)
                        .unit(unit)
                        .widthCm(width)
                        .calculatedM2(calcM2)
                        .unitPrice(price)
                        .lineTotal(lineTot)
                        .block(block)
                        .slab(slab)
                        .stockItem(stockItem)
                        .build();

                invoice.getItems().add(item);
                subtotal = subtotal.add(lineTot);
            }
        }

        invoice.setSubtotalAmount(subtotal);
        if (effectiveTaxRate != null && effectiveTaxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taxAmount = subtotal.multiply(effectiveTaxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            invoice.setTaxAmount(taxAmount);
            invoice.setTotalAmount(subtotal.add(taxAmount));
        } else {
            invoice.setTaxAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(subtotal);
        }

        Invoice saved = invoiceRepository.save(invoice);

        // 1 & 4 & 6 & 11: Stok hareketleri ve transferleri
        if (invoiceType == InvoiceType.SALES) {
            for (InvoiceItem item : saved.getItems()) {
                if (targetDepartment != null) {
                    // Dahili departman transferi
                    if (item.getBlock() != null) {
                        Block b = item.getBlock();
                        StockLocation prevLoc = b.getCurrentLocation();
                        if (targetDepartment == BusinessUnit.FACTORY) {
                            // Ocaktan Fabrikaya sevkiyat: blok ocak stoğundan düşer, Yolda (IN_TRANSIT) olur.
                            // Fabrika kullanıcısı 'Stoğa Al' diyene kadar fabrika stoğunda görünmez.
                            b.setStatus(BlockStatus.IN_TRANSIT);
                            b.setCurrentLocation(null);
                            recordBlockMovement(b, prevLoc, null, "Dahili Transfer Faturası (" + saved.getInvoiceNo() + ") ile Fabrikaya sevk edildi (Yolda)");
                        } else if (targetDepartment == BusinessUnit.WORKSHOP) {
                            b.setStatus(BlockStatus.AT_FACTORY);
                            StockLocation wsYard = StockLocations.require(stockLocationRepository, StockLocationType.WORKSHOP_STOCK);
                            recordBlockMovement(b, prevLoc, wsYard, "Dahili Transfer Faturası (" + saved.getInvoiceNo() + ") ile Atölyeye sevk edildi");
                            b.setCurrentLocation(wsYard);
                        }
                        blockRepository.save(b);

                        String movCode = UniqueCodes.yearly("MOV-TRF", stockMovementRepository::existsByMovementCode);
                        stockMovementRepository.save(StockMovement.builder()
                                .movementCode(movCode)
                                .movementDate(LocalDateTime.now())
                                .movementType(StockMovementType.TRANSFER)
                                .sourceDepartment(department)
                                .targetDepartment(targetDepartment)
                                .fromLocation(prevLoc)
                                .block(b)
                                .itemDescription("Dahili Sevk: " + b.getBlockCode())
                                .quantity(BigDecimal.ONE)
                                .quantityUnit("adet")
                                .tonnage(b.getEffectiveTonnage())
                                .invoice(saved)
                                .notes("Dahili Transfer Faturası: " + saved.getInvoiceNo())
                                .build());
                    }
                    if (item.getSlab() != null && targetDepartment == BusinessUnit.WORKSHOP) {
                        Slab s = item.getSlab();
                        s.setStatus(SlabStatus.IN_CUTTING);
                        slabRepository.save(s);
                    }
                    if (item.getStockItem() != null && targetDepartment == BusinessUnit.WORKSHOP) {
                        StockItem st = item.getStockItem();
                        StockLocation wsYard = StockLocations.require(stockLocationRepository, StockLocationType.WORKSHOP_STOCK);
                        st.setStockLocation(wsYard);
                        stockItemRepository.save(st);
                    }
                } else {
                    // Dış Müşteriye Satış: Stoktan düş
                    if (item.getBlock() != null) {
                        Block b = item.getBlock();
                        if (b.getStatus() == BlockStatus.SOLD) {
                            throw new IllegalArgumentException("Hata: " + b.getBlockCode() + " kodlu blok zaten satılmış veya stokta bulunmuyor.");
                        }
                        StockLocation prevLoc = b.getCurrentLocation();
                        b.setStatus(BlockStatus.SOLD);
                        if (customer != null) b.setSoldCustomer(customer);
                        recordBlockMovement(b, prevLoc, null, "Satış Faturası (" + saved.getInvoiceNo() + ") ile satıldı");
                        blockRepository.save(b);

                        String movCode = UniqueCodes.yearly("MOV-SLS", stockMovementRepository::existsByMovementCode);
                        stockMovementRepository.save(StockMovement.builder()
                                .movementCode(movCode)
                                .movementDate(LocalDateTime.now())
                                .movementType(StockMovementType.OUTGOING)
                                .sourceDepartment(department)
                                .fromLocation(prevLoc)
                                .block(b)
                                .customer(customer)
                                .itemDescription("Blok Satışı: " + b.getBlockCode())
                                .quantity(BigDecimal.ONE)
                                .quantityUnit("adet")
                                .tonnage(b.getEffectiveTonnage())
                                .invoice(saved)
                                .notes("Satış Faturası: " + saved.getInvoiceNo() + ", Alıcı: " + saved.getPartyName())
                                .build());
                    }
                    if (item.getSlab() != null) {
                        Slab s = item.getSlab();
                        if (s.getStatus() == SlabStatus.SOLD) {
                            throw new IllegalArgumentException("Hata: " + s.getSlabCode() + " kodlu plaka zaten satılmış.");
                        }
                        s.setStatus(SlabStatus.SOLD);
                        if (customer != null) s.setCustomer(customer);
                        slabRepository.save(s);

                        String movCode = UniqueCodes.yearly("MOV-SLS", stockMovementRepository::existsByMovementCode);
                        stockMovementRepository.save(StockMovement.builder()
                                .movementCode(movCode)
                                .movementDate(LocalDateTime.now())
                                .movementType(StockMovementType.OUTGOING)
                                .sourceDepartment(department)
                                .slab(s)
                                .customer(customer)
                                .itemDescription("Plaka Satışı: " + s.getSlabCode())
                                .quantity(s.getSurfaceAreaM2() != null ? s.getSurfaceAreaM2() : BigDecimal.ONE)
                                .quantityUnit("m2")
                                .invoice(saved)
                                .notes("Satış Faturası: " + saved.getInvoiceNo() + ", Alıcı: " + saved.getPartyName())
                                .build());
                    }
                    if (item.getStockItem() != null) {
                        StockItem st = item.getStockItem();
                        BigDecimal deduction = item.getCalculatedM2() != null && item.getCalculatedM2().compareTo(BigDecimal.ZERO) > 0
                                ? item.getCalculatedM2()
                                : item.getQuantity();
                        if (deduction == null || deduction.compareTo(BigDecimal.ZERO) <= 0) {
                            deduction = BigDecimal.ONE;
                        }
                        BigDecimal currentQty = st.getQuantity() != null ? st.getQuantity() : BigDecimal.ZERO;
                        if (currentQty.compareTo(deduction) < 0) {
                            throw new IllegalArgumentException(String.format(
                                    "Yetersiz stok! '%s' için mevcut stok: %s %s, çıkış yapılmak istenen: %s %s. Stok miktarı eksiye düşürülemez.",
                                    st.getDescription() != null ? st.getDescription() : (st.getItemCode() != null ? st.getItemCode() : "Ürün"),
                                    currentQty.toPlainString(),
                                    st.getUnit() != null ? st.getUnit() : "adet",
                                    deduction.toPlainString(),
                                    st.getUnit() != null ? st.getUnit() : "adet"));
                        }
                        BigDecimal newQty = currentQty.subtract(deduction);
                        st.setQuantity(newQty);
                        if (newQty.compareTo(BigDecimal.ZERO) == 0 && (st.getProductType() == StockProductType.SLAB || st.getProductType() == StockProductType.SIZED)) {
                            st.setStatus("SOLD");
                        }
                        stockItemRepository.save(st);

                        String movCode = UniqueCodes.yearly("MOV-SLS", stockMovementRepository::existsByMovementCode);
                        stockMovementRepository.save(StockMovement.builder()
                                .movementCode(movCode)
                                .movementDate(LocalDateTime.now())
                                .movementType(StockMovementType.OUTGOING)
                                .sourceDepartment(department)
                                .fromLocation(st.getStockLocation())
                                .stockItem(st)
                                .customer(customer)
                                .itemDescription("Fatura Satışı: " + (st.getDescription() != null ? st.getDescription() : st.getItemCode()))
                                .quantity(deduction)
                                .quantityUnit(st.getUnit() != null ? st.getUnit() : "adet")
                                .invoice(saved)
                                .notes("Fatura no: " + saved.getInvoiceNo() + ", Alıcı: " + saved.getPartyName())
                                .build());
                    }
                }
            }
        }

        // 6 & 10: Alış Faturalarında Doğrudan Gider veya Stok Girişi
        if (invoiceType == InvoiceType.PURCHASE) {
            if (Boolean.TRUE.equals(directExpense)) {
                // Doğrudan Giderleştir (KDV Hariç Net Tutar - Madde 6.B & Madde 10)
                if (subtotal.compareTo(BigDecimal.ZERO) > 0) {
                    CostCenter cc = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(department)
                            .or(() -> costCenterRepository.findByCode("CC-001"))
                            .or(() -> costCenterRepository.findAll().stream().findFirst())
                            .orElse(null);
                    if (cc != null) {
                        CostTransaction tx = CostTransaction.builder()
                                .costCenter(cc)
                                .project(project)
                                .expenseType(ExpenseType.OVERHEAD)
                                .expenseCategory(ExpenseCategory.OTHER)
                                .amount(subtotal)
                                .businessUnit(department)
                                .documentNo(saved.getInvoiceNo())
                                .invoiceDate(saved.getInvoiceDate())
                                .entryDate(LocalDate.now())
                                .expensePeriod(YearMonth.from(saved.getInvoiceDate()).toString())
                                .description("Doğrudan Alış Faturası Gideri: " + saved.getInvoiceNo() + (saved.getNotes() != null && !saved.getNotes().isBlank() ? " — " + saved.getNotes() : ""))
                                .build();
                        costTransactionRepository.save(tx);
                    }
                }
            } else {
                // Stok Alımı (Madde 6.A): Gider henüz yazılmaz; depoya/stoğa alınır
                if (department == BusinessUnit.QUARRY) {
                    for (InvoiceItem item : saved.getItems()) {
                        String pName = item.getProductName() != null ? item.getProductName().toLowerCase() : "";
                        String unit = item.getUnit() != null ? item.getUnit().toLowerCase() : "";
                        boolean isFuel = pName.contains("mazot") || pName.contains("dizel") || pName.contains("diesel") || unit.contains("lt") || unit.contains("litre");
                        if (isFuel) {
                            quarryInventoryService.addFuelStock(item.getQuantity(), item.getUnitPrice(), saved, saved.getNotes());
                        } else {
                            quarryInventoryService.addConsumableStock(null, item.getProductName(), item.getQuantity(), item.getUnit(), item.getUnitPrice(), saved, saved.getNotes());
                        }
                    }
                }
            }
        }

        // 2: Şantiye / Proje Maliyet Takibi (KDV Hariç Net Tutar)
        if (project != null && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            CostCenter cc = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.SITE)
                    .or(() -> costCenterRepository.findByCode("CC-005"))
                    .or(() -> costCenterRepository.findAll().stream().findFirst())
                    .orElse(null);
            if (cc != null) {
                CostTransaction tx = CostTransaction.builder()
                        .costCenter(cc)
                        .project(project)
                        .constructionSite(project)
                        .expenseType(ExpenseType.MATERIAL)
                        .expenseCategory(ExpenseCategory.MATERIAL)
                        .amount(subtotal)
                        .businessUnit(department != null ? department : BusinessUnit.SITE)
                        .documentNo(saved.getInvoiceNo())
                        .invoiceDate(saved.getInvoiceDate())
                        .entryDate(LocalDate.now())
                        .expensePeriod(YearMonth.from(saved.getInvoiceDate()).toString())
                        .description("Fatura Malzeme Çıkışı: " + saved.getInvoiceNo() + (saved.getNotes() != null && !saved.getNotes().isBlank() ? " — " + saved.getNotes() : ""))
                        .build();
                costTransactionRepository.save(tx);
            }
        }

        return saved;
    }

    @Transactional
    public Invoice createInvoice(String invoiceNo, LocalDate invoiceDate, LocalDate dueDate,
                                 InvoiceType invoiceType, BusinessUnit department, Long customerId,
                                 Long supplierId, BusinessUnit targetDepartment, Long projectId,
                                 BigDecimal taxRate, String partyName, String notes,
                                 List<InvoiceItemForm> itemForms) {
        return createInvoice(invoiceNo, invoiceDate, dueDate, invoiceType, department, customerId, supplierId, targetDepartment, projectId, taxRate, false, partyName, notes, itemForms);
    }

    @Transactional
    public Invoice createInvoice(String invoiceNo, LocalDate invoiceDate, LocalDate dueDate,
                                 InvoiceType invoiceType, BusinessUnit department, Long customerId,
                                 Long supplierId, String partyName, String notes,
                                 List<InvoiceItemForm> itemForms) {
        return createInvoice(invoiceNo, invoiceDate, dueDate, invoiceType, department, customerId, supplierId, null, null, new BigDecimal("20.00"), partyName, notes, itemForms);
    }

    private void recordBlockMovement(Block block, StockLocation from, StockLocation to, String desc) {
        if (movementRepository != null && block != null && to != null) {
            movementRepository.save(BlockLocationMovement.builder()
                    .block(block)
                    .fromLocation(from)
                    .toLocation(to)
                    .description(desc)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public Page<Invoice> searchInvoices(InvoiceType type, BusinessUnit dept, InvoiceStatus status,
                                        Long customerId, LocalDate startDate, LocalDate endDate,
                                        String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return invoiceRepository.searchInvoices(type, dept, status, customerId, startDate, endDate, search, pageable);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fatura bulunamadı: " + id));
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlyTotal(InvoiceType type) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1);
        return invoiceRepository.sumAmountByTypeThisMonth(type, start, end);
    }
}
