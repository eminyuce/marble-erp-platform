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
import com.ozerler.marble.model.enums.QuarryCategory;
import org.springframework.http.ResponseEntity;
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

import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SlabRepository;

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
    private final SlabRepository slabRepository;
    private final ProjectRepository projectRepository;
    private final com.ozerler.marble.service.QuarryInventoryService quarryInventoryService;

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
        model.addAttribute("projects", projectRepository.findAll());
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
            String party = inv.getTargetDepartment() != null
                    ? "Dahili: " + inv.getTargetDepartment().getDisplayName()
                    : (inv.getCustomer() != null ? inv.getCustomer().getCompanyName()
                    : (inv.getSupplier() != null ? inv.getSupplier().getCompanyName() : inv.getPartyName()));
            map.put("partyName", party != null ? party : "—");
            map.put("department", inv.getDepartment() != null ? inv.getDepartment().name() : "");
            map.put("departmentLabel", inv.getDepartment() != null ? inv.getDepartment().getDisplayName() : "Genel");
            map.put("targetDepartment", inv.getTargetDepartment() != null ? inv.getTargetDepartment().name() : "");
            map.put("targetDepartmentLabel", inv.getTargetDepartment() != null ? inv.getTargetDepartment().getDisplayName() : "");
            map.put("isInternalTransfer", inv.isInternalTransfer());
            map.put("projectName", inv.getProject() != null ? inv.getProject().getName() : "");
            map.put("subtotalAmount", inv.getSubtotalAmount() != null ? inv.getSubtotalAmount() : BigDecimal.ZERO);
            map.put("taxRate", inv.getTaxRate() != null ? inv.getTaxRate() : BigDecimal.ZERO);
            map.put("taxAmount", inv.getTaxAmount() != null ? inv.getTaxAmount() : BigDecimal.ZERO);
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
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("departments", BusinessUnit.values());
        model.addAttribute("invoiceTypes", InvoiceType.values());
        model.addAttribute("blocks", blockRepository.findAllWithQuarry());
        model.addAttribute("stockItems", stockItemRepository.findAll());
        model.addAttribute("slabs", slabRepository.findByStatus(SlabStatus.AVAILABLE));
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

                    itemForms.add(new InvoiceService.InvoiceItemForm(pName, desc, qty, unit, price, blkId, slbId, stkId, width, calcM2));
                }
            }

            BigDecimal effectiveTaxRate = taxRate != null ? taxRate : new BigDecimal("20.00");
            InvoiceStatus effectiveStatus = status != null ? status : InvoiceStatus.ISSUED;

            Invoice invoice = invoiceService.createInvoice(
                    invoiceNo, invoiceDate, dueDate, invoiceType, department,
                    customerId, supplierId, targetDepartment, projectId, effectiveTaxRate,
                    directExpense, effectiveStatus, partyName, notes, itemForms, quarryCategory
            );

            redirectAttributes.addFlashAttribute("successMessage", "Fatura (" + invoice.getInvoiceNo() + ") başarıyla oluşturuldu. Toplam: " + invoice.getTotalAmount() + " TL");
            return "redirect:/invoices/" + invoice.getId();
        } catch (Exception e) {
            log.error("Fatura oluşturulamadı", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fatura oluşturulurken hata: " + e.getMessage());
            return "redirect:/invoices/new?type=" + invoiceType.name();
        }
    }

    @PostMapping("/{id}/finalize")
    public String finalizeInvoice(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.finalizeInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Fatura (" + invoice.getInvoiceNo() + ") başarıyla kesinleştirildi ve stok hareketleri işlendi.");
        } catch (Exception e) {
            log.error("Fatura kesinleştirme hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fatura kesinleştirilemedi: " + e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelInvoice(@PathVariable("id") Long id,
                                @RequestParam(value = "reason", required = false, defaultValue = "Kullanıcı talebiyle iptal edildi") String reason,
                                RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.cancelInvoice(id, reason);
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
            @RequestParam(value = "includeZero", required = false, defaultValue = "false") Boolean includeZero) {

        List<Map<String, Object>> result = new ArrayList<>();
        String s = search != null ? search.trim().toLowerCase() : "";

        // 1. Bloklar (Ocak & Fabrika filtrelenebilir)
        if (stockType == null || stockType.isBlank() || "BLOCK".equalsIgnoreCase(stockType)) {
            List<Block> blocks = blockRepository.findAllWithQuarry().stream()
                    .filter(b -> b.getStatus() != com.ozerler.marble.model.enums.BlockStatus.SOLD && b.getStatus() != com.ozerler.marble.model.enums.BlockStatus.IN_TRANSIT)
                    .filter(b -> {
                        if (department == BusinessUnit.QUARRY) {
                            return b.isAtQuarry();
                        } else if (department == BusinessUnit.FACTORY) {
                            return b.isAtFactory();
                        }
                        return true;
                    })
                    .filter(b -> s.isEmpty() || b.getBlockCode().toLowerCase().contains(s) || (b.getStoneType() != null && b.getStoneType().toLowerCase().contains(s)))
                    .limit(50)
                    .toList();
            for (Block b : blocks) {
                Map<String, Object> m = new HashMap<>();
                m.put("category", "BLOCK");
                m.put("categoryLabel", "Blok");
                m.put("id", b.getId());
                m.put("code", b.getBlockCode());
                m.put("name", "Blok: " + b.getBlockCode() + (b.getStoneType() != null ? " (" + b.getStoneType() + ")" : ""));
                m.put("stoneType", b.getStoneType() != null ? b.getStoneType() : "—");
                m.put("dimensions", (b.getWidthCm() != null ? b.getWidthCm() : 0) + "x" + (b.getLengthCm() != null ? b.getLengthCm() : 0) + "x" + (b.getHeightCm() != null ? b.getHeightCm() : 0) + " cm");
                BigDecimal ton = b.getActualTonnage() != null && b.getActualTonnage().compareTo(BigDecimal.ZERO) > 0
                        ? b.getActualTonnage()
                        : (b.getEstimatedTonnage() != null ? b.getEstimatedTonnage() : BigDecimal.ONE);
                m.put("quantity", ton);
                m.put("unit", "ton");
                m.put("widthCm", b.getWidthCm());
                result.add(m);
            }
        }

        // 2. Mazot Deposu (Ocak Mazot Stoğu - Madde 3 & 8)
        if (department == null || department == BusinessUnit.QUARRY) {
            if (stockType == null || stockType.isBlank() || "FUEL".equalsIgnoreCase(stockType) || "MAZOT".equalsIgnoreCase(stockType)) {
                StockItem fuelTank = quarryInventoryService.getOrCreateFuelTankStockItem();
                if (fuelTank != null && fuelTank.getQuantity() != null && fuelTank.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    if (s.isEmpty() || "mazot".contains(s) || "dizel".contains(s) || fuelTank.getItemCode().toLowerCase().contains(s)) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("category", "FUEL");
                        m.put("categoryLabel", "Mazot");
                        m.put("id", fuelTank.getId());
                        m.put("code", fuelTank.getItemCode());
                        m.put("name", "Ocak Mazot Stoğu (Dizel)");
                        m.put("stoneType", "Akaryakıt");
                        m.put("dimensions", "Ana Depo Tankı");
                        m.put("quantity", fuelTank.getQuantity());
                        m.put("unit", "litre");
                        m.put("unitPrice", fuelTank.getUnitPrice());
                        result.add(m);
                    }
                }
            }
        }

        // 3. Sarf Malzeme & Diğer Ocak Stok Kartları (Madde 1 & 2 & 9)
        if (department == null || department == BusinessUnit.QUARRY) {
            List<StockItem> allQuarryCards = quarryInventoryService.getAllQuarryStockCards(null);
            for (StockItem ci : allQuarryCards) {
                if (ci.getItemCode().equals("O-MZ-TANK")) {
                    continue;
                }
                if (Boolean.TRUE.equals(includeZero) || (ci.getQuantity() != null && ci.getQuantity().compareTo(BigDecimal.ZERO) > 0)) {
                    String catCode = ci.getQuarryCategory() != null ? ci.getQuarryCategory().name() : "CONSUMABLE";
                    String catLabel = ci.getQuarryCategory() != null ? ci.getQuarryCategory().getDisplayName() : "Sarf Malzeme";

                    if (stockType != null && !stockType.isBlank()) {
                        boolean match = stockType.equalsIgnoreCase(catCode)
                                || ("CONSUMABLE".equalsIgnoreCase(stockType) && (ci.getQuarryCategory() == null || ci.getQuarryCategory() == com.ozerler.marble.model.enums.QuarryCategory.SARF_MALZEME));
                        if (!match) {
                            continue;
                        }
                    }

                    if (s.isEmpty() || (ci.getDescription() != null && ci.getDescription().toLowerCase().contains(s)) || (ci.getItemCode() != null && ci.getItemCode().toLowerCase().contains(s))) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("category", catCode);
                        m.put("categoryLabel", catLabel);
                        m.put("id", ci.getId());
                        m.put("code", ci.getItemCode());
                        m.put("name", ci.getDescription() != null ? ci.getDescription() : ci.getItemCode());
                        m.put("stoneType", catLabel);
                        m.put("dimensions", "—");
                        m.put("quantity", ci.getQuantity());
                        m.put("unit", ci.getUnit() != null ? ci.getUnit() : "adet");
                        m.put("unitPrice", ci.getUnitPrice());
                        m.put("directExpense", Boolean.TRUE.equals(ci.getDirectExpense()));
                        result.add(m);
                    }
                }
            }
        }

        // 4. Plakalar (Fabrika & Atölye)
        if (department == null || department == BusinessUnit.FACTORY || department == BusinessUnit.WORKSHOP) {
            if (stockType == null || stockType.isBlank() || "SLAB".equalsIgnoreCase(stockType)) {
                List<Slab> slabs = slabRepository.findByStatus(SlabStatus.AVAILABLE).stream()
                        .filter(sl -> s.isEmpty() || sl.getSlabCode().toLowerCase().contains(s))
                        .limit(50)
                        .toList();
                for (Slab sl : slabs) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("category", "SLAB");
                    m.put("categoryLabel", "Plaka");
                    m.put("id", sl.getId());
                    m.put("code", sl.getSlabCode());
                    m.put("name", "Plaka: " + sl.getSlabCode());
                    m.put("stoneType", "Plaka");
                    m.put("dimensions", sl.getWidthCm() + "x" + sl.getLengthCm() + "x" + sl.getThicknessCm() + " cm");
                    m.put("quantity", sl.getSurfaceAreaM2() != null ? sl.getSurfaceAreaM2() : BigDecimal.ONE);
                    m.put("unit", "m2");
                    m.put("widthCm", sl.getWidthCm());
                    result.add(m);
                }
            }
        }

        // 5. Ebatlı Ürün Stoğu
        if (department == null || department == BusinessUnit.FACTORY || department == BusinessUnit.WORKSHOP) {
            if (stockType == null || stockType.isBlank() || "SIZED".equalsIgnoreCase(stockType)) {
                List<StockItem> stockItems = stockItemRepository.findAll().stream()
                        .filter(st -> st.getQuantity() != null && st.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                        .filter(st -> {
                            if (department != null && st.getStockLocation() != null) {
                                return department.equals(st.getStockLocation().getBusinessUnit());
                            }
                            return true;
                        })
                        .filter(st -> s.isEmpty() || (st.getProductCode() != null && st.getProductCode().toLowerCase().contains(s)) || (st.getStoneType() != null && st.getStoneType().toLowerCase().contains(s)))
                        .limit(50)
                        .toList();
                for (StockItem st : stockItems) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("category", "SIZED");
                    m.put("categoryLabel", "Ebatlı Stok");
                    m.put("id", st.getId());
                    m.put("code", st.getProductCode() != null ? st.getProductCode() : st.getItemCode());
                    m.put("name", (st.getProductCode() != null ? st.getProductCode() : st.getItemCode()) + " — " + (st.getStoneType() != null ? st.getStoneType() : "Ebatlı Mermer"));
                    m.put("stoneType", st.getStoneType() != null ? st.getStoneType() : "—");
                    m.put("dimensions", (st.getWidthCm() != null ? st.getWidthCm() : "") + "x" + (st.getLengthCm() != null ? st.getLengthCm() : "") + "x" + (st.getThicknessCm() != null ? st.getThicknessCm() : "") + " cm");
                    m.put("quantity", st.getQuantity());
                    m.put("unit", st.getUnit() != null ? st.getUnit() : "m2");
                    m.put("widthCm", st.getWidthCm());
                    result.add(m);
                }
            }
        }

        return result;
    }

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
