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

import com.ozerler.marble.dto.TabulatorResponse;
import java.util.HashMap;
import java.util.Map;

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
                        Model model) {

        BigDecimal monthlyPurchase = invoiceService.getMonthlyTotal(InvoiceType.PURCHASE);
        BigDecimal monthlySales = invoiceService.getMonthlyTotal(InvoiceType.SALES);

        model.addAttribute("selectedType", invoiceType != null ? invoiceType.name() : "");
        model.addAttribute("selectedDepartment", department != null ? department.name() : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("departments", BusinessUnit.values());
        model.addAttribute("monthlyPurchase", monthlyPurchase);
        model.addAttribute("monthlySales", monthlySales);
        model.addAttribute("activeNav", "invoices");

        return "invoices/list";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getInvoicesData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type", required = false) InvoiceType invoiceType,
            @RequestParam(value = "department", required = false) BusinessUnit department,
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "customerId", required = false) Long customerId) {

        int pageIndex = Math.max(0, page - 1);
        Page<Invoice> invoicePage = invoiceService.searchInvoices(invoiceType, department, status, customerId, null, null, search, pageIndex, size);

        List<Map<String, Object>> data = invoicePage.getContent().stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("invoiceNumber", inv.getInvoiceNumber());
            map.put("invoiceDate", inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : "");
            map.put("invoiceType", inv.getInvoiceType() != null ? inv.getInvoiceType().name() : "");
            map.put("invoiceTypeLabel", inv.getInvoiceType() != null ? inv.getInvoiceType().getDisplayName() : "Satış");
            String party = inv.getCustomer() != null ? inv.getCustomer().getCompanyName()
                    : (inv.getSupplier() != null ? inv.getSupplier().getCompanyName() : inv.getPartyName());
            map.put("partyName", party != null ? party : "—");
            map.put("department", inv.getDepartment() != null ? inv.getDepartment().name() : "");
            map.put("departmentLabel", inv.getDepartment() != null ? inv.getDepartment().getDisplayName() : "Genel");
            map.put("totalAmount", inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
            map.put("status", inv.getStatus() != null ? inv.getStatus().name() : "");
            map.put("statusLabel", inv.getStatus() != null ? inv.getStatus().getDisplayName() : "Ödenmedi");
            return map;
        }).toList();

        return TabulatorResponse.of(data, invoicePage.getTotalPages(), invoicePage.getTotalElements());
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
