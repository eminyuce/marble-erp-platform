package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.service.FactoryStockService;
import com.ozerler.marble.service.OperationWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.dto.TabulatorResponse;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/operations/factory")
@RequiredArgsConstructor
@Slf4j
public class FactoryOperationController extends AbstractController {

    private final FactoryStockService factoryStockService;
    private final OperationWorkOrderService workOrderService;
    private final CustomerRepository customerRepository;
    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final StockItemRepository stockItemRepository;

    @GetMapping
    public String index() {
        return "redirect:/operations/factory/blocks";
    }

    // --- 1. BLOK STOK SAHASI ---
    @GetMapping("/blocks")
    public String blockYard(@RequestParam(value = "customerId", required = false) Long customerId,
                            @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                            Model model) {

        List<Block> blocks = factoryStockService.getFactoryUncutBlocks(customerId, generalStockOnly);
        List<Customer> customers = customerRepository.findAll();

        BigDecimal totalWeightTon = blocks.stream()
                .map(b -> b.getActualTonnage() != null ? b.getActualTonnage() : b.getApproximateTonnage())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("blocks", blocks);
        model.addAttribute("customers", customers);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("totalBlockCount", blocks.size());
        model.addAttribute("totalWeightTon", totalWeightTon);
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "blocks");

        return "operations/factory/blocks";
    }

    @GetMapping("/blocks/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getFactoryBlocksData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly) {

        int pageIndex = Math.max(0, page - 1);
        Page<Block> blocksPage = factoryStockService.getFactoryUncutBlocksPaged(customerId, generalStockOnly, search, pageIndex, size);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        List<Map<String, Object>> data = blocksPage.getContent().stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("blockCode", b.getBlockCode());
            map.put("stoneType", b.getStoneType() != null ? b.getStoneType() : "—");
            map.put("quarryName", b.getQuarry() != null ? b.getQuarry().getName() : (b.getQuarryName() != null ? b.getQuarryName() : "Merkez Ocak"));
            map.put("arrivalDate", b.getArrivalDate() != null ? b.getArrivalDate().format(dateFmt) : (b.getCreatedDate() != null ? b.getCreatedDate().format(dateFmt) : "—"));
            map.put("estimatedTonnage", b.getEstimatedTonnage() != null ? b.getEstimatedTonnage() : (b.getApproximateTonnage() != null ? b.getApproximateTonnage() : null));
            map.put("actualTonnage", b.getActualTonnage());
            map.put("dimensions", b.getDimensions() != null ? b.getDimensions() : "—");
            map.put("customerName", b.getAssignedCustomer() != null ? b.getAssignedCustomer().getCompanyName() : null);
            map.put("customerId", b.getAssignedCustomer() != null ? b.getAssignedCustomer().getId() : null);
            map.put("isGeneralStock", b.getAssignedCustomer() == null);
            map.put("statusLabel", "Kesilmemiş Blok");
            map.put("notes", b.getNotes() != null ? b.getNotes() : "");
            return map;
        }).collect(Collectors.toList());

        return TabulatorResponse.of(data, blocksPage.getTotalPages(), blocksPage.getTotalElements());
    }

    @PostMapping("/blocks/{id}/assign-customer")
    public String assignCustomerToBlock(@PathVariable("id") Long id,
                                        @RequestParam(value = "customerId", required = false) Long customerId,
                                        RedirectAttributes redirectAttributes) {
        try {
            Block block = factoryStockService.assignCustomerToBlock(id, customerId);
            String custName = block.getAssignedCustomer() != null ? block.getAssignedCustomer().getCompanyName() : "Genel Stok";
            redirectAttributes.addFlashAttribute("successMessage", "Blok (" + block.getBlockCode() + ") müşterisi güncellendi: " + custName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/factory/blocks";
    }

    @PostMapping("/blocks/{id}/dispatch-cutting")
    public String dispatchBlockToCutting(@PathVariable("id") Long id,
                                         @RequestParam(value = "notes", required = false) String notes,
                                         RedirectAttributes redirectAttributes) {
        try {
            Block block = factoryStockService.dispatchBlockToCutting(id, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Blok (" + block.getBlockCode() + ") kesime alındı. Kesilmemiş blok stokundan düşüldü.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/factory/blocks";
    }

    // --- 2. PLAKA STOK SAHASI ---
    @GetMapping("/slabs")
    public String slabYard(@RequestParam(value = "status", required = false) SlabStatus status,
                           @RequestParam(value = "customerId", required = false) Long customerId,
                           @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                           @RequestParam(value = "search", required = false) String search,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "20") int size,
                           Model model) {

        Page<Slab> slabsPage = factoryStockService.getFactorySlabsPaged(status, customerId, generalStockOnly, search, page, size);
        List<Customer> customers = customerRepository.findAll();
        BigDecimal customerSlabArea = customerId != null ? factoryStockService.getCustomerSlabAreaTotal(customerId) : BigDecimal.ZERO;

        model.addAttribute("slabs", slabsPage.getContent());
        model.addAttribute("page", slabsPage);
        model.addAttribute("customers", customers);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("search", search);
        model.addAttribute("customerSlabArea", customerSlabArea);
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "slabs");

        return "operations/factory/slabs";
    }

    @GetMapping("/slabs/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getFactorySlabsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) SlabStatus status,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly) {

        int pageIndex = Math.max(0, page - 1);
        Page<Slab> slabsPage = factoryStockService.getFactorySlabsPaged(status, customerId, generalStockOnly, search, pageIndex, size);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        List<Map<String, Object>> data = slabsPage.getContent().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("slabCode", s.getSlabCode());
            map.put("sourceBlockCode", s.getBlock() != null ? s.getBlock().getBlockCode() : "—");
            map.put("stoneType", s.getBlock() != null && s.getBlock().getStoneType() != null ? s.getBlock().getStoneType() : "—");
            map.put("qualityGrade", s.getQualityGrade() != null ? s.getQualityGrade().getDisplayName() : "1. Sınıf");
            map.put("surfaceFinish", s.getSurfaceFinish() != null ? s.getSurfaceFinish().getDisplayName() : "Cilalı");
            map.put("thicknessCm", s.getThicknessCm() != null ? s.getThicknessCm() + " cm" : "2 cm");
            map.put("dimensions", (s.getWidthCm() != null && s.getLengthCm() != null) ? (s.getWidthCm() + "×" + s.getLengthCm() + " cm") : "—");
            map.put("surfaceAreaM2", s.getSurfaceAreaM2());
            map.put("pieceCount", 1);
            map.put("customerName", s.getCustomer() != null ? s.getCustomer().getCompanyName() : null);
            map.put("customerId", s.getCustomer() != null ? s.getCustomer().getId() : null);
            map.put("isGeneralStock", s.getCustomer() == null);
            map.put("status", s.getStatus() != null ? s.getStatus().name() : "AVAILABLE");
            map.put("statusLabel", s.getStatus() != null ? s.getStatus().getDisplayName() : "Hazır");
            map.put("createdDate", s.getCreatedDate() != null ? s.getCreatedDate().format(dateFmt) : "—");
            map.put("notes", "");
            return map;
        }).collect(Collectors.toList());

        return TabulatorResponse.of(data, slabsPage.getTotalPages(), slabsPage.getTotalElements());
    }

    @GetMapping({"/slabs/new", "/slabs/create"})
    public String newSlabForm(Model model) {
        model.addAttribute("blocks", blockRepository.findAllWithQuarry());
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "slabs");
        return "operations/factory/slab-form";
    }

    @PostMapping({"/slabs/new", "/slabs/create"})
    public String createSlab(@RequestParam("blockId") Long blockId,
                             @RequestParam("thicknessCm") BigDecimal thicknessCm,
                             @RequestParam("widthCm") BigDecimal widthCm,
                             @RequestParam("lengthCm") BigDecimal lengthCm,
                             @RequestParam(value = "qualityGrade", required = false) QualityGrade qualityGrade,
                             @RequestParam(value = "surfaceFinish", required = false) SurfaceFinish surfaceFinish,
                             @RequestParam(value = "customerId", required = false) Long customerId,
                             @RequestParam(value = "notes", required = false) String notes,
                             RedirectAttributes redirectAttributes) {
        try {
            Slab slab = factoryStockService.createSlabDirect(blockId, thicknessCm, widthCm, lengthCm, qualityGrade, surfaceFinish, customerId, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Plaka (" + slab.getSlabCode() + ") başarıyla Plaka Stok Sahasına eklendi: " + slab.getSurfaceAreaM2() + " m²");
            return "redirect:/operations/factory/slabs";
        } catch (Exception e) {
            log.error("Plaka kaydedilemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
            return "redirect:/operations/factory/slabs/new";
        }
    }

    @PostMapping("/slabs/{id}/assign-customer")
    public String assignCustomerToSlab(@PathVariable("id") Long id,
                                       @RequestParam(value = "customerId", required = false) Long customerId,
                                       RedirectAttributes redirectAttributes) {
        try {
            Slab slab = factoryStockService.assignCustomerToSlab(id, customerId);
            String custName = slab.getCustomer() != null ? slab.getCustomer().getCompanyName() : "Genel Stok";
            redirectAttributes.addFlashAttribute("successMessage", "Plaka (" + slab.getSlabCode() + ") müşterisi güncellendi: " + custName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/factory/slabs";
    }

    // --- 3. EBATLI STOK SAHASI ---
    @GetMapping("/sized")
    public String sizedYard(@RequestParam(value = "customerId", required = false) Long customerId,
                            @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                            @RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            Model model) {

        Page<StockItem> itemsPage = factoryStockService.getSizedStockItemsPaged(customerId, generalStockOnly, search, page, size);
        List<Customer> customers = customerRepository.findAll();

        model.addAttribute("items", itemsPage.getContent());
        model.addAttribute("page", itemsPage);
        model.addAttribute("customers", customers);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("search", search);
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "sized");

        return "operations/factory/sized";
    }

    @GetMapping("/sized/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getFactorySizedData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly) {

        int pageIndex = Math.max(0, page - 1);
        Page<StockItem> itemsPage = factoryStockService.getSizedStockItemsPaged(customerId, generalStockOnly, search, pageIndex, size);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        List<Map<String, Object>> data = itemsPage.getContent().stream().map(it -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", it.getId());
            map.put("itemCode", it.getItemCode());
            map.put("sourceBlockCode", it.getSourceBlock() != null ? it.getSourceBlock().getBlockCode() : "—");
            map.put("stoneType", it.getStoneType() != null ? it.getStoneType() : "Mermer");
            map.put("description", it.getDescription() != null ? it.getDescription() : "Ebatlı Mermer");
            map.put("surfaceFinish", it.getSurfaceFinish() != null ? it.getSurfaceFinish().getDisplayName() : "Cilalı");
            map.put("thicknessCm", it.getThicknessCm() != null ? it.getThicknessCm() + " cm" : "2 cm");
            map.put("dimensions", (it.getWidthCm() != null && it.getLengthCm() != null) ? (it.getWidthCm() + "×" + it.getLengthCm() + " cm") : "—");
            map.put("quantity", it.getQuantity());
            map.put("unit", it.getUnit() != null ? it.getUnit() : "m2");
            map.put("pieceCount", it.getPieceCount());
            map.put("actualProducedQuantity", it.getActualProducedQuantity());
            map.put("customerName", it.getCustomer() != null ? it.getCustomer().getCompanyName() : null);
            map.put("customerId", it.getCustomer() != null ? it.getCustomer().getId() : null);
            map.put("isGeneralStock", it.getCustomer() == null);
            map.put("productionDate", it.getProductionDate() != null ? it.getProductionDate().format(dateFmt) : (it.getCreatedDate() != null ? it.getCreatedDate().format(dateFmt) : "—"));
            map.put("status", it.getStatus() != null ? it.getStatus() : "AVAILABLE");
            map.put("statusLabel", "Hazır / Stokta");
            map.put("notes", it.getNotes() != null ? it.getNotes() : "");
            return map;
        }).collect(Collectors.toList());

        return TabulatorResponse.of(data, itemsPage.getTotalPages(), itemsPage.getTotalElements());
    }

    @GetMapping({"/sized/new", "/sized/create"})
    public String newSizedForm(Model model) {
        model.addAttribute("blocks", blockRepository.findAllWithQuarry());
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "sized");
        return "operations/factory/sized-form";
    }

    @PostMapping({"/sized/new", "/sized/create"})
    public String createSizedItem(@RequestParam(value = "blockId", required = false) Long blockId,
                                  @RequestParam(value = "description", required = false) String description,
                                  @RequestParam("thicknessCm") BigDecimal thicknessCm,
                                  @RequestParam("widthCm") BigDecimal widthCm,
                                  @RequestParam("lengthCm") BigDecimal lengthCm,
                                  @RequestParam("quantity") BigDecimal quantity,
                                  @RequestParam(value = "pieceCount", defaultValue = "1") int pieceCount,
                                  @RequestParam(value = "actualProducedQuantity", required = false) BigDecimal actualProducedQuantity,
                                  @RequestParam(value = "customerId", required = false) Long customerId,
                                  @RequestParam(value = "qualityGrade", required = false) String qualityGrade,
                                  @RequestParam(value = "surfaceFinish", required = false) String surfaceFinish,
                                  @RequestParam(value = "edgeFinish", required = false) String edgeFinish,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  RedirectAttributes redirectAttributes) {
        try {
            StockItem item = factoryStockService.createSizedItem(
                    blockId, description, thicknessCm, widthCm, lengthCm, quantity, pieceCount,
                    actualProducedQuantity, customerId, qualityGrade, surfaceFinish, edgeFinish, notes
            );
            redirectAttributes.addFlashAttribute("successMessage", "Ebatlı ürün (" + item.getItemCode() + ") Ebatlı Stok Sahasına eklendi.");
            return "redirect:/operations/factory/sized";
        } catch (Exception e) {
            log.error("Ebatlı ürün kaydedilemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
            return "redirect:/operations/factory/sized/new";
        }
    }

    @PostMapping("/sized/{id}/assign-customer")
    public String assignCustomerToStockItem(@PathVariable("id") Long id,
                                            @RequestParam(value = "customerId", required = false) Long customerId,
                                            RedirectAttributes redirectAttributes) {
        try {
            StockItem item = factoryStockService.assignCustomerToStockItem(id, customerId);
            String custName = item.getCustomer() != null ? item.getCustomer().getCompanyName() : "Genel Stok";
            redirectAttributes.addFlashAttribute("successMessage", "Ebatlı ürün (" + item.getItemCode() + ") müşterisi güncellendi: " + custName);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/factory/sized";
    }

    // --- 4. FABRİKA SİPARİŞLER / İŞ EMİRLERİ ---
    @GetMapping("/orders")
    public String factoryOrders(@RequestParam(value = "status", required = false) OperationWorkOrderStatus status,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "20") int size,
                                Model model) {

        Page<OperationWorkOrder> ordersPage = workOrderService.searchOrders(BusinessUnit.FACTORY, status, null, null, null, null, page, size);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("page", ordersPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "orders");

        return "operations/factory/orders";
    }
}
