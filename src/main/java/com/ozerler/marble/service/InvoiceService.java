package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
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
import java.time.LocalDate;
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

    public record InvoiceItemForm(String productName, String description, BigDecimal quantity, String unit, BigDecimal unitPrice, Long blockId, Long stockItemId) {}

    @Transactional
    public Invoice createInvoice(String invoiceNo, LocalDate invoiceDate, LocalDate dueDate,
                                 InvoiceType invoiceType, BusinessUnit department, Long customerId,
                                 Long supplierId, String partyName, String notes,
                                 List<InvoiceItemForm> itemForms) {

        Objects.requireNonNull(invoiceType, "Fatura tipi seçilmelidir.");
        Objects.requireNonNull(department, "Departman seçilmelidir.");

        String no = (invoiceNo != null && !invoiceNo.isBlank())
                ? invoiceNo.trim().toUpperCase()
                : UniqueCodes.yearly(invoiceType == InvoiceType.PURCHASE ? "ALF" : "STF", invoiceRepository::existsByInvoiceNo);

        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        Supplier supplier = supplierId != null ? supplierRepository.findById(supplierId).orElse(null) : null;

        Invoice invoice = Invoice.builder()
                .invoiceNo(no)
                .invoiceDate(invoiceDate != null ? invoiceDate : LocalDate.now())
                .dueDate(dueDate)
                .invoiceType(invoiceType)
                .department(department)
                .customer(customer)
                .supplier(supplier)
                .partyName(partyName != null && !partyName.isBlank() ? partyName : (customer != null ? customer.getCompanyName() : (supplier != null ? supplier.getCompanyName() : "")))
                .status(InvoiceStatus.ISSUED)
                .notes(notes)
                .items(new ArrayList<>())
                .build();

        BigDecimal grandTotal = BigDecimal.ZERO;

        if (itemForms != null && !itemForms.isEmpty()) {
            for (InvoiceItemForm f : itemForms) {
                if (f.productName() == null || f.productName().isBlank()) continue;
                BigDecimal qty = f.quantity() != null ? f.quantity() : BigDecimal.ONE;
                BigDecimal price = f.unitPrice() != null ? f.unitPrice() : BigDecimal.ZERO;
                BigDecimal lineTot = qty.multiply(price);

                Block block = f.blockId() != null ? blockRepository.findById(f.blockId()).orElse(null) : null;
                StockItem stockItem = f.stockItemId() != null ? stockItemRepository.findById(f.stockItemId()).orElse(null) : null;

                InvoiceItem item = InvoiceItem.builder()
                        .invoice(invoice)
                        .productName(f.productName().trim())
                        .description(f.description())
                        .quantity(qty)
                        .unit(f.unit() != null && !f.unit().isBlank() ? f.unit() : "m2")
                        .unitPrice(price)
                        .lineTotal(lineTot)
                        .block(block)
                        .stockItem(stockItem)
                        .build();

                invoice.getItems().add(item);
                grandTotal = grandTotal.add(lineTot);
            }
        }

        invoice.setSubtotalAmount(grandTotal);
        invoice.setTotalAmount(grandTotal);

        return invoiceRepository.save(invoice);
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
