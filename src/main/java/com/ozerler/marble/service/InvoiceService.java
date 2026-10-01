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
                                 BigDecimal taxRate, String partyName, String notes,
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

        // 1 & 4 & 6: Stok hareketleri ve transferleri
        if (invoiceType == InvoiceType.SALES) {
            for (InvoiceItem item : saved.getItems()) {
                if (targetDepartment != null) {
                    // Dahili departman transferi
                    if (item.getBlock() != null) {
                        Block b = item.getBlock();
                        if (targetDepartment == BusinessUnit.FACTORY) {
                            b.setStatus(BlockStatus.AT_FACTORY);
                            StockLocation factoryYard = StockLocations.require(stockLocationRepository, StockLocationType.FACTORY_BLOCK_YARD);
                            recordBlockMovement(b, b.getCurrentLocation(), factoryYard, "Dahili Transfer Faturası (" + saved.getInvoiceNo() + ") ile Fabrikaya sevk edildi");
                            b.setCurrentLocation(factoryYard);
                            b.setArrivalDate(invoiceDate != null ? invoiceDate : LocalDate.now());
                        } else if (targetDepartment == BusinessUnit.WORKSHOP) {
                            b.setStatus(BlockStatus.AT_FACTORY);
                            StockLocation wsYard = StockLocations.require(stockLocationRepository, StockLocationType.WORKSHOP_STOCK);
                            recordBlockMovement(b, b.getCurrentLocation(), wsYard, "Dahili Transfer Faturası (" + saved.getInvoiceNo() + ") ile Atölyeye sevk edildi");
                            b.setCurrentLocation(wsYard);
                        }
                        blockRepository.save(b);
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
                        b.setStatus(BlockStatus.SOLD);
                        if (customer != null) b.setSoldCustomer(customer);
                        blockRepository.save(b);
                    }
                    if (item.getSlab() != null) {
                        Slab s = item.getSlab();
                        s.setStatus(SlabStatus.SOLD);
                        if (customer != null) s.setCustomer(customer);
                        slabRepository.save(s);
                    }
                    if (item.getStockItem() != null) {
                        StockItem st = item.getStockItem();
                        BigDecimal deduction = item.getCalculatedM2() != null && item.getCalculatedM2().compareTo(BigDecimal.ZERO) > 0
                                ? item.getCalculatedM2()
                                : item.getQuantity();
                        if (st.getQuantity() != null) {
                            BigDecimal newQty = st.getQuantity().subtract(deduction);
                            if (newQty.compareTo(BigDecimal.ZERO) <= 0) {
                                st.setQuantity(BigDecimal.ZERO);
                                st.setStatus("SOLD");
                            } else {
                                st.setQuantity(newQty);
                            }
                            stockItemRepository.save(st);
                        }
                    }
                }
            }
        }

        // 2: Şantiye / Proje Maliyet Takibi
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
