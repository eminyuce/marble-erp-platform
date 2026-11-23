package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.InvoiceRef;
import com.ozerler.marble.dto.view.PurchaseOrderView;
import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.service.FormLookupService;
import com.ozerler.marble.service.InvoiceScreenService;
import com.ozerler.marble.service.InvoiceService;
import com.ozerler.marble.service.ProcurementService;
import com.ozerler.marble.service.QuarryInventoryService;
import com.ozerler.marble.web.InvoiceGrid;
import com.ozerler.marble.web.ListQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController extends AbstractController {

    private final FormLookupService formLookup;

    private final InvoiceService invoiceService;
    private final InvoiceScreenService invoiceScreenService;
    private final QuarryInventoryService quarryInventoryService;
    private final ProcurementService procurementService;

    @GetMapping
    public String index(@RequestParam(value = "type", required = false) InvoiceType invoiceType,
                        @RequestParam(value = "department", required = false) BusinessUnit department,
                        @RequestParam(value = "status", required = false) InvoiceStatus status,
                        @RequestParam(value = "customerId", required = false) Long customerId,
                        @ModelAttribute ListQuery query, Model model) {

        BigDecimal monthlyPurchase = invoiceService.getMonthlyTotal(InvoiceType.PURCHASE);
        BigDecimal monthlySales = invoiceService.getMonthlyTotal(InvoiceType.SALES);

        model.addAttribute(
                "grid",
                InvoiceGrid.view(
                        invoiceService.searchInvoiceRows(
                                invoiceType,
                                department,
                                status,
                                customerId,
                                null,
                                null,
                                query.getSearch(),
                                query.pageNumber() - 1,
                                query.pageSize()),
                        query.pageNumber(),
                        query.pageSize(),
                        query.sortKey(),
                        query.sortDirection()));
        Object valueValue;
        if (invoiceType != null) {
            valueValue = invoiceType.name();
        } else {
            valueValue = "";
        }
        model.addAttribute("selectedType", valueValue);
        Object valueValue2;
        if (department != null) {
            valueValue2 = department.name();
        } else {
            valueValue2 = "";
        }
        model.addAttribute("selectedDepartment", valueValue2);
        Object valueValue3;
        if (status != null) {
            valueValue3 = status.name();
        } else {
            valueValue3 = "";
        }
        model.addAttribute("selectedStatus", valueValue3);
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
                PurchaseOrderView po = procurementService.getOrder(purchaseOrderId);
                if (po != null) {
                    type = InvoiceType.PURCHASE;
                    if (po.getBusinessUnit() != null) {
                        department = po.getBusinessUnit();
                    }
                    model.addAttribute("prefillPurchaseOrder", po);
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> itemMaps = po.getItems().stream().map(it -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("itemName", it.getItemName());
                        Object label;
                        if (it.getItemType() != null) {
                            label = it.getItemType().getLabel();
                        } else {
                            label = "";
                        }
                        m.put("description", label);
                        m.put("quantity", Objects.requireNonNullElse(it.getQuantity(), BigDecimal.ONE));
                        m.put("unit", Objects.requireNonNullElse(it.getUnit(), "ADET"));
                        m.put("unitPrice", Objects.requireNonNullElse(it.getUnitPrice(), BigDecimal.ZERO));
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
            model.addAttribute("prefillBlock", formLookup.findBlock(blockId));
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
    public String createInvoice(
            @RequestParam(value = "invoiceNo", required = false) String invoiceNo,
            @RequestParam("invoiceDate") LocalDate invoiceDate,
            @RequestParam(value = "dueDate", required = false) LocalDate dueDate,
            @RequestParam("invoiceType") InvoiceType invoiceType,
            @RequestParam("department") BusinessUnit department,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "supplierId", required = false) Long supplierId,
            @RequestParam(value = "targetDepartment", required = false)
                    BusinessUnit targetDepartment,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(value = "taxRate", required = false) BigDecimal taxRate,
            @RequestParam(value = "directExpense", required = false, defaultValue = "false")
                    Boolean directExpense,
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
            @RequestParam(value = "itemCalculatedM2", required = false)
                    List<BigDecimal> calculatedM2s,
            @RequestParam(value = "itemWasteQuantity", required = false)
                    List<BigDecimal> wasteQuantities,
            @RequestParam(value = "purchaseOrderId", required = false) Long purchaseOrderId,
            RedirectAttributes redirectAttributes) {

        try {
            List<InvoiceService.InvoiceItemForm> itemForms = new ArrayList<>();
            if (productNames != null) {
                for (int i = 0; i < productNames.size(); i++) {
                    String pName = productNames.get(i);
                    if (pName == null || pName.isBlank()) {
                        continue;
                    }
                    String desc;
                    if (descriptions != null && i < descriptions.size()) {
                        desc = descriptions.get(i);
                    } else {
                        desc = null;
                    }
                    BigDecimal qty;
                    if (quantities != null && i < quantities.size()) {
                        qty = quantities.get(i);
                    } else {
                        qty = BigDecimal.ONE;
                    }
                    String unit;
                    if (units != null && i < units.size()) {
                        unit = units.get(i);
                    } else {
                        unit = "m2";
                    }
                    BigDecimal price;
                    if (unitPrices != null && i < unitPrices.size()) {
                        price = unitPrices.get(i);
                    } else {
                        price = BigDecimal.ZERO;
                    }
                    Long blkId;
                    if (blockIds != null && i < blockIds.size()) {
                        blkId = blockIds.get(i);
                    } else {
                        blkId = null;
                    }
                    Long slbId;
                    if (slabIds != null && i < slabIds.size()) {
                        slbId = slabIds.get(i);
                    } else {
                        slbId = null;
                    }
                    Long stkId;
                    if (stockItemIds != null && i < stockItemIds.size()) {
                        stkId = stockItemIds.get(i);
                    } else {
                        stkId = null;
                    }
                    BigDecimal width;
                    if (widthCms != null && i < widthCms.size()) {
                        width = widthCms.get(i);
                    } else {
                        width = null;
                    }
                    BigDecimal calcM2;
                    if (calculatedM2s != null && i < calculatedM2s.size()) {
                        calcM2 = calculatedM2s.get(i);
                    } else {
                        calcM2 = null;
                    }
                    BigDecimal waste;
                    if (wasteQuantities != null && i < wasteQuantities.size()) {
                        waste = wasteQuantities.get(i);
                    } else {
                        waste = BigDecimal.ZERO;
                    }

                    itemForms.add(
                            new InvoiceService.InvoiceItemForm(
                                    pName, desc, qty, unit, price, blkId, slbId, stkId, width,
                                    calcM2, waste));
                }
            }

            BigDecimal effectiveTaxRate;
            if (taxRate != null) {
                effectiveTaxRate = taxRate;
            } else {
                effectiveTaxRate = new BigDecimal("20.00");
            }
            InvoiceStatus effectiveStatus;
            if (status != null) {
                effectiveStatus = status;
            } else {
                effectiveStatus = InvoiceStatus.ISSUED;
            }

            InvoiceRef invoice = invoiceService.createInvoiceRef(
                    invoiceNo, invoiceDate, dueDate, invoiceType, department,
                    customerId, supplierId, targetDepartment, projectId, effectiveTaxRate,
                    directExpense, effectiveStatus, partyName, notes, itemForms, quarryCategory
            );

            if (purchaseOrderId != null) {
                try {
                    procurementService.updateStatus(purchaseOrderId, PurchaseOrderStatus.DELIVERED);
                } catch (Exception ex) {
                    log.warn("Could not update purchase order status to DELIVERED: {}", purchaseOrderId, ex);
                }
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Fatura ("
                            + invoice.getInvoiceNo()
                            + ") başarıyla oluşturuldu. Toplam: "
                            + invoice.getTotalAmount()
                            + " TL");
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
            InvoiceRef invoice = invoiceService.finalizeInvoiceRef(id);
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
    public String cancelInvoice(
            @PathVariable("id") Long id,
            @RequestParam(
                            value = "reason",
                            required = false,
                            defaultValue = "Kullanıcı talebiyle iptal edildi")
                    String reason,
            RedirectAttributes redirectAttributes) {
        try {
            InvoiceRef invoice = invoiceService.cancelInvoiceRef(id, reason);
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
            StockItemView item = quarryInventoryService.createStockCardView(
                    quarryCategory, productName, unit, BigDecimal.ZERO, unitPrice, itemCode, isExpense, notes);

            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("id", item.getId());
            resp.put("name", item.getDescription());
            resp.put("code", item.getItemCode());
            Object valueValue;
            if (item.getProductType() != null) {
                valueValue = item.getProductType().name();
            } else {
                valueValue = "CONSUMABLE";
            }
            resp.put("category", valueValue);
            Object displayName;
            if (item.getQuarryCategory() != null) {
                displayName = item.getQuarryCategory().getDisplayName();
            } else {
                displayName = "Sarf Malzeme";
            }
            resp.put("categoryLabel", displayName);
            Object valueValue2;
            if (item.getQuarryCategory() != null) {
                valueValue2 = item.getQuarryCategory().name();
            } else {
                valueValue2 = quarryCategory.name();
            }
            resp.put("quarryCategory", valueValue2);
            resp.put("unit", item.getUnit());
            resp.put("unitPrice", item.getUnitPrice());
            resp.put("quantity", Objects.requireNonNullElse(item.getQuantity(), BigDecimal.ZERO));
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
        model.addAttribute("invoice", invoiceService.getInvoice(id));
        model.addAttribute("activeNav", "invoices");
        return "invoices/detail";
    }
}
