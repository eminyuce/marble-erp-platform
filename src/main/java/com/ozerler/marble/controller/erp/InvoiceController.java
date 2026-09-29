package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController extends AbstractController {

    private final InvoiceService invoiceService;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BlockRepository blockRepository;
    private final StockItemRepository stockItemRepository;

    @GetMapping
    public String index(@RequestParam(value = "type", required = false) InvoiceType invoiceType,
                        @RequestParam(value = "department", required = false) BusinessUnit department,
                        @RequestParam(value = "status", required = false) InvoiceStatus status,
                        @RequestParam(value = "customerId", required = false) Long customerId,
                        @RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size,
                        Model model) {

        Page<Invoice> invoicePage = invoiceService.searchInvoices(invoiceType, department, status, customerId, null, null, search, page, size);
        BigDecimal monthlyPurchase = invoiceService.getMonthlyTotal(InvoiceType.PURCHASE);
        BigDecimal monthlySales = invoiceService.getMonthlyTotal(InvoiceType.SALES);

        model.addAttribute("invoices", invoicePage.getContent());
        model.addAttribute("page", invoicePage);
        model.addAttribute("selectedType", invoiceType);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("departments", BusinessUnit.values());
        model.addAttribute("monthlyPurchase", monthlyPurchase);
        model.addAttribute("monthlySales", monthlySales);
        model.addAttribute("activeNav", "invoices");

        return "invoices/list";
    }

    @GetMapping({"/new", "/create"})
    public String newInvoiceForm(@RequestParam(value = "type", defaultValue = "SALES") InvoiceType type,
                                 @RequestParam(value = "department", defaultValue = "FACTORY") BusinessUnit department,
                                 Model model) {

        model.addAttribute("defaultType", type);
        model.addAttribute("defaultDepartment", department);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("departments", BusinessUnit.values());
        model.addAttribute("invoiceTypes", InvoiceType.values());
        model.addAttribute("blocks", blockRepository.findAllWithQuarry());
        model.addAttribute("stockItems", stockItemRepository.findAll());
        model.addAttribute("activeNav", "invoices");

        return "invoices/form";
    }

    @PostMapping({"/new", "/create"})
    public String createInvoice(@RequestParam(value = "invoiceNo", required = false) String invoiceNo,
                                @RequestParam("invoiceDate") LocalDate invoiceDate,
                                @RequestParam(value = "dueDate", required = false) LocalDate dueDate,
                                @RequestParam("invoiceType") InvoiceType invoiceType,
                                @RequestParam("department") BusinessUnit department,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "supplierId", required = false) Long supplierId,
                                @RequestParam(value = "partyName", required = false) String partyName,
                                @RequestParam(value = "notes", required = false) String notes,
                                @RequestParam(value = "itemProductName", required = false) List<String> productNames,
                                @RequestParam(value = "itemDescription", required = false) List<String> descriptions,
                                @RequestParam(value = "itemQuantity", required = false) List<BigDecimal> quantities,
                                @RequestParam(value = "itemUnit", required = false) List<String> units,
                                @RequestParam(value = "itemUnitPrice", required = false) List<BigDecimal> unitPrices,
                                @RequestParam(value = "itemBlockId", required = false) List<Long> blockIds,
                                @RequestParam(value = "itemStockItemId", required = false) List<Long> stockItemIds,
                                RedirectAttributes redirectAttributes) {

        try {
            List<InvoiceService.InvoiceItemForm> itemForms = new ArrayList<>();
            if (productNames != null) {
                for (int i = 0; i < productNames.size(); i++) {
                    String pName = productNames.get(i);
                    if (pName == null || pName.isBlank()) continue;
                    String desc = (descriptions != null && i < descriptions.size()) ? descriptions.get(i) : null;
                    BigDecimal qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : BigDecimal.ONE;
                    String unit = (units != null && i < units.size()) ? units.get(i) : "m2";
                    BigDecimal price = (unitPrices != null && i < unitPrices.size()) ? unitPrices.get(i) : BigDecimal.ZERO;
                    Long blkId = (blockIds != null && i < blockIds.size()) ? blockIds.get(i) : null;
                    Long stkId = (stockItemIds != null && i < stockItemIds.size()) ? stockItemIds.get(i) : null;

                    itemForms.add(new InvoiceService.InvoiceItemForm(pName, desc, qty, unit, price, blkId, stkId));
                }
            }

            Invoice invoice = invoiceService.createInvoice(
                    invoiceNo, invoiceDate, dueDate, invoiceType, department,
                    customerId, supplierId, partyName, notes, itemForms
            );

            redirectAttributes.addFlashAttribute("successMessage", "Fatura (" + invoice.getInvoiceNo() + ") başarıyla oluşturuldu. Toplam: " + invoice.getTotalAmount() + " TL");
            return "redirect:/invoices/" + invoice.getId();
        } catch (Exception e) {
            log.error("Fatura oluşturulamadı", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fatura oluşturulurken hata: " + e.getMessage());
            return "redirect:/invoices/new?type=" + invoiceType.name();
        }
    }

    @GetMapping("/{id}")
    public String invoiceDetail(@PathVariable("id") Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("activeNav", "invoices");
        return "invoices/detail";
    }
}
