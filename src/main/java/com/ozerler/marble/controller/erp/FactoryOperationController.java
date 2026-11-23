package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.view.WorkOrderView;
import com.ozerler.marble.dto.view.SlabView;
import com.ozerler.marble.dto.view.MachineFuelEntryView;
import com.ozerler.marble.dto.view.MachineRef;
import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.web.StockYardResponses;

import com.ozerler.marble.common.Constants;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.service.FactoryInventoryService;
import com.ozerler.marble.service.FactoryStockService;
import com.ozerler.marble.service.OperationWorkOrderService;
import com.ozerler.marble.web.FactoryOrderGrid;
import com.ozerler.marble.web.FactorySizedGrid;
import com.ozerler.marble.web.FactorySlabGrid;
import com.ozerler.marble.web.ListQuery;
import com.ozerler.marble.web.OperationFactoryBlockGrid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.dto.TabulatorResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/operations/factory")
@RequiredArgsConstructor
@Slf4j
public class FactoryOperationController extends AbstractController {

    private final com.ozerler.marble.service.FormLookupService formLookup;

    private final FactoryStockService factoryStockService;
    private final OperationWorkOrderService workOrderService;
    private final FactoryInventoryService factoryInventoryService;

    @GetMapping
    public String index() {
        return "redirect:/operations/factory/blocks";
    }

    @GetMapping("/customers")
    public String factoryCustomers() {
        return "redirect:/admin/definitions/customers?unit=FACTORY";
    }

    @GetMapping("/checks")
    public String factoryChecks() {
        return "redirect:/collections/checks?unit=FACTORY";
    }

    // --- 1. BLOK STOK SAHASI ---
    @GetMapping("/blocks")
    public String blockYard(@RequestParam(value = "customerId", required = false) Long customerId,
                            @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                            @ModelAttribute ListQuery query, Model model) {

        var figures = factoryStockService.factoryBlockFigures(customerId, generalStockOnly);
        var customers = formLookup.customers();

        model.addAttribute("blocks", factoryStockService.getFactoryUncutBlockViews(customerId, generalStockOnly));
        model.addAttribute("incomingBlocks", factoryStockService.getIncomingBlockViewsInTransit());
        model.addAttribute("customers", customers);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("totalBlockCount", figures.count());
        model.addAttribute("totalWeightTon", figures.weightTon());
        model.addAttribute("totalCostValue", figures.costValue());
        model.addAttribute("totalSaleValue", figures.saleValue());
        model.addAttribute("grid", OperationFactoryBlockGrid.view(
                factoryStockService.getFactoryUncutBlockViewsPaged(customerId, generalStockOnly, query.getSearch(), query.pageNumber() - 1, query.pageSize()),
                query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
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

        return factoryStockService.factoryBlockRows(page, size, search, customerId, generalStockOnly);
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/blocks/{id}/assign-customer")
    public String assignCustomerToBlock(@PathVariable("id") Long id,
                                        @RequestParam(value = "customerId", required = false) Long customerId,
                                        RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("successMessage", factoryStockService.assignCustomerMessage(id, customerId));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/factory/blocks";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/blocks/{id}/dispatch-cutting")
    public String dispatchBlockToCutting(@PathVariable("id") Long id,
                                         @RequestParam(value = "notes", required = false) String notes,
                                         RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("successMessage", factoryStockService.dispatchCuttingMessage(id, notes));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
        }
        return "redirect:/operations/factory/blocks";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/blocks/{id}/receive")
    public String receiveBlock(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("successMessage", factoryStockService.receiveAtFactoryMessage(id));
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
                           @ModelAttribute ListQuery query, Model model) {

        Page<SlabView> slabsPage = StockYardResponses.slabPage(
                factoryStockService, status, customerId, generalStockOnly, query);
        var customers = formLookup.customers();
        BigDecimal customerSlabArea = customerId != null ? factoryStockService.getCustomerSlabAreaTotal(customerId) : BigDecimal.ZERO;

        model.addAttribute("slabs", slabsPage.getContent());
        model.addAttribute("page", slabsPage);
        model.addAttribute("customers", customers);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("search", query.getSearch());
        model.addAttribute("customerSlabArea", customerSlabArea);
        model.addAttribute("grid", FactorySlabGrid.view(
                slabsPage, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
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
        return StockYardResponses.slabs(factoryStockService, page, size, search, status, customerId, generalStockOnly);
    }

    @GetMapping({"/slabs/new", "/slabs/create"})
    public String newSlabForm(Model model) {
        model.addAttribute("blocks", formLookup.blocksWithQuarry());
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "slabs");
        return "operations/factory/slab-form";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping({"/slabs/new", "/slabs/create"})
    public String createSlab(@RequestParam("blockId") Long blockId,
                             @RequestParam(value = "thicknessCm", required = false) BigDecimal thicknessCm,
                             @RequestParam(value = "widthCm", required = false) BigDecimal widthCm,
                             @RequestParam(value = "lengthCm", required = false) BigDecimal lengthCm,
                             @RequestParam(value = "rowThicknessCm", required = false) List<BigDecimal> rowThicknesses,
                             @RequestParam(value = "rowWidthCm", required = false) List<BigDecimal> rowWidths,
                             @RequestParam(value = "rowLengthCm", required = false) List<BigDecimal> rowLengths,
                             @RequestParam(value = "rowPieceCount", required = false) List<Integer> rowPieceCounts,
                             @RequestParam(value = "qualityGrade", required = false) QualityGrade qualityGrade,
                             @RequestParam(value = "surfaceFinish", required = false) SurfaceFinish surfaceFinish,
                             @RequestParam(value = "customerId", required = false) Long customerId,
                             @RequestParam(value = "notes", required = false) String notes,
                             RedirectAttributes redirectAttributes) {
        try {
            if (rowWidths != null && !rowWidths.isEmpty()) {
                List<SlabView> slabs = factoryStockService.createSlabsBatchViews(
                        blockId, thicknessCm, qualityGrade, surfaceFinish, customerId, notes,
                        rowThicknesses, rowWidths, rowLengths, rowPieceCounts
                );
                BigDecimal totalM2 = slabs.stream()
                        .map(s -> s.getSurfaceAreaM2() != null ? s.getSurfaceAreaM2() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                redirectAttributes.addFlashAttribute("successMessage", slabs.size() + " adet plaka (" + totalM2 + " m²) başarıyla Plaka Stok Sahasına eklendi.");
                return "redirect:/operations/factory/slabs";
            }

            SlabView slab = factoryStockService.createSlabDirectView(blockId, thicknessCm != null ? thicknessCm : new BigDecimal("2.00"), widthCm, lengthCm, qualityGrade, surfaceFinish, customerId, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Plaka (" + slab.getSlabCode() + ") başarıyla Plaka Stok Sahasına eklendi: " + slab.getSurfaceAreaM2() + " m²");
            return "redirect:/operations/factory/slabs";
        } catch (Exception e) {
            log.error("Plaka kaydedilemedi", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
            return "redirect:/operations/factory/slabs/new";
        }
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/slabs/{id}/assign-customer")
    public String assignCustomerToSlab(@PathVariable("id") Long id,
                                       @RequestParam(value = "customerId", required = false) Long customerId,
                                       RedirectAttributes redirectAttributes) {
        try {
            SlabView slab = factoryStockService.assignCustomerToSlabView(id, customerId);
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
                            @ModelAttribute ListQuery query, Model model) {

        Page<StockItemView> itemsPage = factoryStockService.getSizedStockItemViewsPaged(
                customerId, generalStockOnly, query.getSearch(), query.pageNumber() - 1, query.pageSize());
        var customers = formLookup.customers();

        model.addAttribute("items", itemsPage.getContent());
        model.addAttribute("page", itemsPage);
        model.addAttribute("customers", customers);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("search", query.getSearch());
        model.addAttribute("grid", FactorySizedGrid.view(
                itemsPage, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
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
        Page<StockItemView> itemsPage = factoryStockService.getSizedStockItemViewsPaged(customerId, generalStockOnly, search, pageIndex, size);
        return StockYardResponses.factorySized(itemsPage);
    }

    @GetMapping({"/sized/new", "/sized/create"})
    public String newSizedForm(Model model) {
        model.addAttribute("blocks", formLookup.blocksWithQuarry());
        model.addAttribute("slabs", formLookup.slabsInStatus(List.of(SlabStatus.AVAILABLE, SlabStatus.RESERVED)));
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "sized");
        return "operations/factory/sized-form";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping({"/sized/new", "/sized/create"})
    public String createSizedItem(@RequestParam(value = "blockId", required = false) Long blockId,
                                  @RequestParam(value = "slabId", required = false) Long slabId,
                                  @RequestParam(value = "description", required = false) String description,
                                  @RequestParam(value = "thicknessCm", required = false) BigDecimal thicknessCm,
                                  @RequestParam(value = "widthCm", required = false) BigDecimal widthCm,
                                  @RequestParam(value = "lengthCm", required = false) BigDecimal lengthCm,
                                  @RequestParam(value = "quantity", required = false) BigDecimal quantity,
                                  @RequestParam(value = "pieceCount", defaultValue = "1") int pieceCount,
                                  @RequestParam(value = "actualProducedQuantity", required = false) BigDecimal actualProducedQuantity,
                                  @RequestParam(value = "customerId", required = false) Long customerId,
                                  @RequestParam(value = "qualityGrade", required = false) String qualityGrade,
                                  @RequestParam(value = "surfaceFinish", required = false) String surfaceFinish,
                                  @RequestParam(value = "edgeFinish", required = false) String edgeFinish,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  @RequestParam(value = "rowDescription", required = false) List<String> rowDescriptions,
                                  @RequestParam(value = "rowThicknessCm", required = false) List<BigDecimal> rowThicknesses,
                                  @RequestParam(value = "rowWidthCm", required = false) List<BigDecimal> rowWidths,
                                  @RequestParam(value = "rowLengthCm", required = false) List<BigDecimal> rowLengths,
                                  @RequestParam(value = "rowPieceCount", required = false) List<Integer> rowPieceCounts,
                                  @RequestParam(value = "rowQuantity", required = false) List<BigDecimal> rowQuantities,
                                  @RequestParam(value = "rowSurfaceFinish", required = false) List<String> rowSurfaceFinishes,
                                  @RequestParam(value = "rowEdgeFinish", required = false) List<String> rowEdgeFinishes,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (rowWidths != null && !rowWidths.isEmpty()) {
                List<StockItemView> items = factoryStockService.createSizedItemsBatchViews(
                        blockId, slabId, customerId, description, qualityGrade, surfaceFinish, edgeFinish, notes,
                        rowDescriptions, rowThicknesses, rowWidths, rowLengths, rowPieceCounts, rowQuantities,
                        rowSurfaceFinishes, rowEdgeFinishes
                );
                BigDecimal totalM2 = items.stream().map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                redirectAttributes.addFlashAttribute("successMessage", items.size() + " grup ebatlı ürün (" + totalM2 + " m²) başarıyla Ebatlı Stok Sahasına eklendi.");
                return "redirect:/operations/factory/sized";
            }

            StockItemView item = factoryStockService.createSizedItemView(
                    blockId, slabId, description, thicknessCm, widthCm, lengthCm, quantity, pieceCount,
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

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/sized/{id}/assign-customer")
    public String assignCustomerToStockItem(@PathVariable("id") Long id,
                                            @RequestParam(value = "customerId", required = false) Long customerId,
                                            RedirectAttributes redirectAttributes) {
        try {
            StockItemView item = factoryStockService.assignCustomerToStockItemView(id, customerId);
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
                                @ModelAttribute ListQuery query, Model model) {

        Page<WorkOrderView> ordersPage = workOrderService.searchOrderRows(
                BusinessUnit.FACTORY, status, null, null, null, query.getSearch(), query.pageNumber() - 1, query.pageSize());
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("page", ordersPage);
        model.addAttribute("grid", FactoryOrderGrid.view(
                ordersPage, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "orders");

        return "operations/factory/orders";
    }

    // --- 5. FABRİKA MAZOT DEPOSU & ARAÇ TAKİBİ (MADDE 15) ---
    @GetMapping("/fuel")
    public String factoryFuelIndex(Model model) {
        StockItemView tank = factoryInventoryService.getOrCreateFactoryFuelTankStockItemView();
        BigDecimal currentLiters = tank.getQuantity() != null ? tank.getQuantity() : BigDecimal.ZERO;
        List<MachineRef> vehicles = factoryInventoryService.getFactoryVehiclesViews();
        List<MachineFuelEntryView> entries = factoryInventoryService.getRecentFuelEntryViews();

        model.addAttribute("tank", tank);
        model.addAttribute("currentLiters", currentLiters);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("entries", entries);
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "fuel");
        return "operations/factory/fuel";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/fuel/dispense")
    public String dispenseFactoryFuel(@RequestParam("machineId") Long machineId,
                                      @RequestParam("litres") BigDecimal litres,
                                      @RequestParam(value = "workingHoursOrKm", required = false) BigDecimal workingHoursOrKm,
                                      @RequestParam(value = "receiptNo", required = false) String receiptNo,
                                      @RequestParam(value = "issuedBy", required = false) String issuedBy,
                                      @RequestParam(value = "receivedBy", required = false) String receivedBy,
                                      @RequestParam(value = "notes", required = false) String notes,
                                      RedirectAttributes redirectAttributes) {
        try {
            MachineFuelEntryView entry = factoryInventoryService.dispenseFuelView(
                    machineId, litres, java.time.LocalDate.now(), workingHoursOrKm, receiptNo, issuedBy, receivedBy, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s aracına %s litre mazot verildi. Tutar (KDV hariç net): %s TL fabrika giderine yazıldı.",
                            entry.getMachine().getName(), litres, entry.getTotalAmount()));
        } catch (Exception e) {
            log.error("Fabrika mazot çıkış hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operations/factory/fuel";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/fuel/add")
    public String addFactoryFuelStock(@RequestParam("litres") BigDecimal litres,
                                      @RequestParam(value = "netUnitPrice", required = false) BigDecimal netUnitPrice,
                                      @RequestParam(value = "invoiceNo", required = false) String invoiceNo,
                                      @RequestParam(value = "notes", required = false) String notes,
                                      RedirectAttributes redirectAttributes) {
        try {
            StockItemView updated = factoryInventoryService.addFuelStockView(litres, netUnitPrice, null,
                    notes != null ? notes : invoiceNo != null ? "Fatura: " + invoiceNo : "Doğrudan Depo Girişi");
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s litre mazot fabrika deposuna eklendi. Güncel stok: %s litre.", litres, updated.getQuantity()));
        } catch (Exception e) {
            log.error("Fabrika mazot dolum hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operations/factory/fuel";
    }

    // --- 6. FABRİKA SARF MALZEME DEPOSU (MADDE 16) ---
    @GetMapping("/consumables")
    public String factoryConsumablesIndex(Model model) {
        List<StockItemView> consumables = factoryInventoryService.getAllFactoryConsumablesViews();
        List<MachineRef> vehicles = factoryInventoryService.getFactoryVehiclesViews();

        BigDecimal totalValue = consumables.stream()
                .filter(item -> item.getQuantity() != null && item.getUnitPrice() != null)
                .map(item -> item.getQuantity().multiply(item.getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("consumables", consumables);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("activeSection", "factory");
        model.addAttribute("activeSubSection", "consumables");
        return "operations/factory/consumables";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/consumables/consume")
    public String consumeFactoryConsumable(@RequestParam("stockItemId") Long stockItemId,
                                           @RequestParam("quantity") BigDecimal quantity,
                                           @RequestParam(value = "machineId", required = false) Long machineId,
                                           @RequestParam(value = "notes", required = false) String notes,
                                           RedirectAttributes redirectAttributes) {
        try {
            StockItemView item = factoryInventoryService.consumeConsumableView(stockItemId, quantity, machineId, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s malzemesinden %s %s kullanıldı ve fabrikanın giderine yazıldı. Kalan stok: %s %s.",
                            item.getDescription(), quantity, item.getUnit(), item.getQuantity(), item.getUnit()));
        } catch (Exception e) {
            log.error("Fabrika sarf malzeme tüketim hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operations/factory/consumables";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/consumables/add")
    public String addFactoryConsumableStock(@RequestParam(value = "itemCode", required = false) String itemCode,
                                            @RequestParam("description") String description,
                                            @RequestParam("quantity") BigDecimal quantity,
                                            @RequestParam(value = "unit", defaultValue = "adet") String unit,
                                            @RequestParam(value = "netUnitPrice", required = false) BigDecimal netUnitPrice,
                                            @RequestParam(value = "notes", required = false) String notes,
                                            RedirectAttributes redirectAttributes) {
        try {
            StockItemView item = factoryInventoryService.createStockCardView(
                    description, unit, quantity, netUnitPrice, itemCode, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("%s (%s %s) fabrika stok kartı ve depo girişi kaydedildi.",
                            item.getDescription(), quantity, item.getUnit()));
        } catch (Exception e) {
            log.error("Fabrika stok ekleme hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operations/factory/consumables";
    }

    @PreAuthorize(Constants.PRE_AUTH_FACTORY_WRITE)
    @PostMapping("/stock-cards/new")
    public String createFactoryStockCard(@RequestParam("description") String description,
                                         @RequestParam(value = "unit", defaultValue = "adet") String unit,
                                         @RequestParam(value = "initialQuantity", required = false) BigDecimal initialQuantity,
                                         @RequestParam(value = "netUnitPrice", required = false) BigDecimal netUnitPrice,
                                         @RequestParam(value = "itemCode", required = false) String itemCode,
                                         @RequestParam(value = "notes", required = false) String notes,
                                         RedirectAttributes redirectAttributes) {
        try {
            StockItemView item = factoryInventoryService.createStockCardView(
                    description, unit, initialQuantity, netUnitPrice, itemCode, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Yeni fabrika stok kartı (%s - %s) başarıyla tanımlandı.",
                            item.getItemCode(), item.getDescription()));
        } catch (Exception e) {
            log.error("Fabrika stok kartı oluşturma hatası: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/operations/factory/consumables";
    }
}
