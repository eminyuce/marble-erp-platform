package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.service.InvoiceService;
import com.ozerler.marble.web.ListQuery;
import com.ozerler.marble.web.InvoiceGrid;
import com.ozerler.marble.service.QuarryInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ozerler.marble.model.enums.QuarryCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.ozerler.marble.model.enums.SlabStatus;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController extends AbstractController {

    private final com.ozerler.marble.service.FormLookupService formLookup;

    private final InvoiceService invoiceService;
    private final com.ozerler.marble.service.InvoiceScreenService invoiceScreenService;
    private final QuarryInventoryService quarryInventoryService;
    private final com.ozerler.marble.service.ProcurementService procurementService;

    @GetMapping
    public String index(@RequestParam(value = "type", required = false) InvoiceType invoiceType,
                        @RequestParam(value = "department", required = false) BusinessUnit department,
                        @RequestParam(value = "status", required = false) InvoiceStatus status,
                        @RequestParam(value = "customerId", required = false) Long customerId,
                        @ModelAttribute ListQuery query, Model model) {

        BigDecimal monthlyPurchase = invoiceService.getMonthlyTotal(InvoiceType.PURCHASE);
        BigDecimal monthlySales = invoiceService.getMonthlyTotal(InvoiceType.SALES);

        model.addAttribute("grid", InvoiceGrid.view(
                invoiceService.searchInvoices(invoiceType, department, status, customerId, null, null, query.getSearch(), query.pageNumber() - 1, query.pageSize()),
                query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("selectedType", invoiceType != null ? invoiceType.name() : "");
        model.addAttribute("selectedDepartment", department != null ? department.name() : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("departments", BusinessUnit.values());
        model.addAttribute("monthlyPurchase", monthlyPurchase);
        model.addAttribute("monthlySales", monthlySales);
        model.addAttribute("projects", formLookup.projects());
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
        return invoiceScreenService.invoicesData(page, size, search, invoiceType, department, status, customerId);
    }


    @GetMapping({"/new", "/create"})
    public String newInvoiceForm(@RequestParam(value = "type", defaultValue = "SALES") InvoiceType type,
                                 @RequestParam(value = "department", defaultValue = "FACTORY") BusinessUnit department,
                                 @RequestParam(value = "blockId", required = false) Long blockId,
                                 @RequestParam(value = "purchaseOrderId", required = false) Long purchaseOrderId,
                                 Model model) {

        if (purchaseOrderId != null) {
            try {
                PurchaseOrder po = procurementService.getOrderById(purchaseOrderId);
                if (po != null) {
                    type = InvoiceType.PURCHASE;
                    if (po.getBusinessUnit() != null) {
                        department = po.getBusinessUnit();
                    }
                    model.addAttribute("prefillPurchaseOrder", po);
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<Map<String, Object>> itemMaps = po.getItems().stream().map(it -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("itemName", it.getItemName());
                        m.put("description", it.getItemType() != null ? it.getItemType().getLabel() : "");
                        m.put("quantity", it.getQuantity() != null ? it.getQuantity() : BigDecimal.ONE);
                        m.put("unit", it.getUnit() != null ? it.getUnit() : "ADET");
                        m.put("unitPrice", it.getUnitPrice() != null ? it.getUnitPrice() : BigDecimal.ZERO);
                        return m;
                    }).toList();
                    model.addAttribute("prefillPoItemsJson", mapper.writeValueAsString(itemMaps));
                }
            } catch (Exception e) {
                log.warn("Could not load PO for invoice prefill: {}", purchaseOrderId, e);
            }
        }

        model.addAttribute("defaultType", type);
        model.addAttribute("defaultDepartment", department);
        if (blockId != null) {
            formLookup.block(blockId).ifPresent(b -> model.addAttribute("prefillBlock", b));
        }
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("suppliers", formLookup.suppliers());
        model.addAttribute("projects", formLookup.projects());
        model.addAttribute("departments", BusinessUnit.values());
        model.addAttribute("invoiceTypes", InvoiceType.values());
        model.addAttribute("blocks", formLookup.blocksWithQuarry());
        model.addAttribute("stockItems", formLookup.stockItems());
        model.addAttribute("slabs", formLookup.slabsByStatus(SlabStatus.AVAILABLE));
        model.addAttribute("activeNav", "invoices");

        return "invoices/form";
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping({"/new", "/create"})
    public String createInvoice(@RequestParam(value = "invoiceNo", required = false) String invoiceNo,
                                @RequestParam("invoiceDate") LocalDate invoiceDate,
                                @RequestParam(value = "dueDate", required = false) LocalDate dueDate,
                                @RequestParam("invoiceType") InvoiceType invoiceType,
                                @RequestParam("department") BusinessUnit department,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "supplierId", required = false) Long supplierId,
                                @RequestParam(value = "targetDepartment", required = false) BusinessUnit targetDepartment,
                                @RequestParam(value = "projectId", required = false) Long projectId,
                                @RequestParam(value = "taxRate", required = false) BigDecimal taxRate,
                                @RequestParam(value = "directExpense", required = false, defaultValue = "false") Boolean directExpense,
                                @RequestParam(value = "quarryCategory", required = false) QuarryCategory quarryCategory,
                                @RequestParam(value = "status", required = false) InvoiceStatus status,
                                @RequestParam(value = "partyName", required = false) String partyName,
                                @RequestParam(value = "notes", required = false) String notes,
                                @RequestParam(value = "itemProductName", required = false) List<String> productNames,
                                @RequestParam(value = "itemDescription", required = false) List<String> descriptions,
                                @RequestParam(value = "itemQuantity", required = false) List<BigDecimal> quantities,
                                @RequestParam(value = "itemUnit", required = false) List<String> units,
                                @RequestParam(value = "itemUnitPrice", required = false) List<BigDecimal> unitPrices,
                                @RequestParam(value = "itemBlockId", required = false) List<Long> blockIds,
                                @RequestParam(value = "itemSlabId", required = false) List<Long> slabIds,
                                @RequestParam(value = "itemStockItemId", required = false) List<Long> stockItemIds,
                                @RequestParam(value = "itemWidthCm", required = false) List<BigDecimal> widthCms,
                                @RequestParam(value = "itemCalculatedM2", required = false) List<BigDecimal> calculatedM2s,
                                @RequestParam(value = "itemWasteQuantity", required = false) List<BigDecimal> wasteQuantities,
                                @RequestParam(value = "purchaseOrderId", required = false) Long purchaseOrderId,
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
                    Long slbId = (slabIds != null && i < slabIds.size()) ? slabIds.get(i) : null;
                    Long stkId = (stockItemIds != null && i < stockItemIds.size()) ? stockItemIds.get(i) : null;
                    BigDecimal width = (widthCms != null && i < widthCms.size()) ? widthCms.get(i) : null;
                    BigDecimal calcM2 = (calculatedM2s != null && i < calculatedM2s.size()) ? calculatedM2s.get(i) : null;
                    BigDecimal waste = (wasteQuantities != null && i < wasteQuantities.size()) ? wasteQuantities.get(i) : BigDecimal.ZERO;

                    itemForms.add(new InvoiceService.InvoiceItemForm(pName, desc, qty, unit, price, blkId, slbId, stkId, width, calcM2, waste));
                }
            }

            BigDecimal effectiveTaxRate = taxRate != null ? taxRate : new BigDecimal("20.00");
            InvoiceStatus effectiveStatus = status != null ? status : InvoiceStatus.ISSUED;

            com.ozerler.marble.dto.view.InvoiceRef invoice = invoiceService.createInvoiceRef(
                    invoiceNo, invoiceDate, dueDate, invoiceType, department,
                    customerId, supplierId, targetDepartment, projectId, effectiveTaxRate,
                    directExpense, effectiveStatus, partyName, notes, itemForms, quarryCategory
            );

            if (purchaseOrderId != null) {
                try {
                    procurementService.updateStatus(purchaseOrderId, com.ozerler.marble.model.enums.PurchaseOrderStatus.DELIVERED);
                } catch (Exception ex) {
                    log.warn("Could not update purchase order status to DELIVERED: {}", purchaseOrderId, ex);
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Fatura (" + invoice.getInvoiceNo() + ") başarıyla oluşturuldu. Toplam: " + invoice.getTotalAmount() + " TL");
            return "redirect:/invoices/" + invoice.getId();
        } catch (Exception e) {
            log.error("Fatura oluşturulamadı", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fatura oluşturulurken hata: " + e.getMessage());
            return "redirect:/invoices/new?type=" + invoiceType.name();
        }
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping("/{id}/finalize")
    public String finalizeInvoice(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            com.ozerler.marble.dto.view.InvoiceRef invoice = invoiceService.finalizeInvoiceRef(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Fatura (" + invoice.getInvoiceNo() + ") başarıyla kesinleştirildi ve stok hareketleri işlendi.");
        } catch (Exception e) {
            log.error("Fatura kesinleştirme hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fatura kesinleştirilemedi: " + e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping("/{id}/cancel")
    public String cancelInvoice(@PathVariable("id") Long id,
                                @RequestParam(value = "reason", required = false, defaultValue = "Kullanıcı talebiyle iptal edildi") String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            com.ozerler.marble.dto.view.InvoiceRef invoice = invoiceService.cancelInvoiceRef(id, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Fatura (" + invoice.getInvoiceNo() + ") iptal edildi ve stoklar depoya iade edildi.");
        } catch (Exception e) {
            log.error("Fatura iptal hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fatura iptal edilemedi: " + e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @GetMapping("/api/available-stock")
    @ResponseBody
    public List<Map<String, Object>> getAvailableStock(
            @RequestParam(value = "department", required = false) BusinessUnit department,
            @RequestParam(value = "type", required = false) String stockType,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "includeZero", required = false, defaultValue = "false") Boolean includeZero) {
        return invoiceScreenService.availableStock(department, stockType, search, customerId, includeZero);
    }


    @PreAuthorize(Constants.PRE_AUTH_FINANCE_WRITE)
    @PostMapping("/api/create-stock-card")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createStockCardApi(
            @RequestParam("quarryCategory") QuarryCategory quarryCategory,
            @RequestParam("productName") String productName,
            @RequestParam(value = "itemCode", required = false) String itemCode,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "unitPrice", required = false) BigDecimal unitPrice,
            @RequestParam(value = "isExpense", required = false, defaultValue = "false") Boolean isExpense,
            @RequestParam(value = "notes", required = false) String notes) {
        try {
            StockItem item = quarryInventoryService.createStockCard(
                    quarryCategory, productName, unit, BigDecimal.ZERO, unitPrice, itemCode, isExpense, notes);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("id", item.getId());
            resp.put("name", item.getDescription());
            resp.put("code", item.getItemCode());
            resp.put("category", item.getProductType() != null ? item.getProductType().name() : "CONSUMABLE");
            resp.put("categoryLabel", item.getQuarryCategory() != null ? item.getQuarryCategory().getDisplayName() : "Sarf Malzeme");
            resp.put("quarryCategory", item.getQuarryCategory() != null ? item.getQuarryCategory().name() : quarryCategory.name());
            resp.put("unit", item.getUnit());
            resp.put("unitPrice", item.getUnitPrice());
            resp.put("quantity", item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
            resp.put("isExpense", Boolean.TRUE.equals(item.getDirectExpense()));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Stok kartı oluşturulamadı: {}", e.getMessage(), e);
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
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
