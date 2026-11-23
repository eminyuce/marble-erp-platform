package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.SlabView;
import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.dto.view.WorkOrderView;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.service.FactoryStockService;
import com.ozerler.marble.service.OperationWorkOrderService;
import com.ozerler.marble.service.WorkshopOperationService;
import com.ozerler.marble.web.ListQuery;
import com.ozerler.marble.web.StockYardResponses;
import com.ozerler.marble.web.WorkshopOrderGrid;
import com.ozerler.marble.web.WorkshopSizedGrid;
import com.ozerler.marble.web.WorkshopSlabGrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/operations/workshop")
@RequiredArgsConstructor
@Slf4j
public class WorkshopOperationController extends AbstractController {

    private final com.ozerler.marble.service.FormLookupService formLookup;

    private final OperationWorkOrderService workOrderService;
    private final FactoryStockService factoryStockService;
    private final WorkshopOperationService workshopOperationService;

    @GetMapping("/customers")
    public String workshopCustomers() {
        return "redirect:/admin/definitions/customers?unit=WORKSHOP";
    }

    @GetMapping("/checks")
    public String workshopChecks() {
        return "redirect:/collections/checks?unit=WORKSHOP";
    }

    @GetMapping
    public String index(@RequestParam(value = "status", required = false) OperationWorkOrderStatus status,
                        @ModelAttribute ListQuery query, Model model) {

        Page<WorkOrderView> ordersPage = workOrderService.searchOrderRows(
                BusinessUnit.WORKSHOP, status, null, null, null, query.getSearch(), query.pageNumber() - 1, query.pageSize());
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("page", ordersPage);
        model.addAttribute("grid", WorkshopOrderGrid.view(
                ordersPage, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeSection", "workshop");
        model.addAttribute("activeSubSection", "orders");
        return "operations/workshop/index";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        return "redirect:/operations/workshop";
    }

    @GetMapping("/pallets")
    public String pallets() {
        return "redirect:/operations/workshop/sized";
    }

    // --- 2. ATÖLYE PLAKA STOK SAHASI ---
    @GetMapping("/slabs")
    public String workshopSlabs(@RequestParam(required = false, name = "status") SlabStatus status,
                                @RequestParam(required = false, name = "customerId") Long customerId,
                                @RequestParam(name = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                                @ModelAttribute ListQuery query, Model model) {

        Page<SlabView> slabsPage = StockYardResponses.slabPage(
                factoryStockService, status, customerId, generalStockOnly, query);
        var customers = formLookup.customers();

        BigDecimal totalAvailableM2 = slabsPage.getContent().stream()
                .map(SlabView::getSurfaceAreaM2).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalAvailableCount = slabsPage.getTotalElements();

        model.addAttribute("slabs", slabsPage.getContent());
        model.addAttribute("page", slabsPage);
        model.addAttribute("customers", customers);
        model.addAttribute("totalAvailableM2", totalAvailableM2);
        model.addAttribute("totalAvailableCount", totalAvailableCount);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("grid", WorkshopSlabGrid.view(
                slabsPage, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("activeSection", "workshop");
        model.addAttribute("activeSubSection", "slabs");

        return "operations/workshop/slabs";
    }

    @GetMapping("/slabs/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getWorkshopSlabsData(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "25") int size,
            @RequestParam(required = false, name = "search") String search,
            @RequestParam(required = false, name = "status") SlabStatus status,
            @RequestParam(required = false, name = "customerId") Long customerId,
            @RequestParam(name = "generalStockOnly", defaultValue = "false") boolean generalStockOnly) {
        return StockYardResponses.slabs(factoryStockService, page, size, search, status, customerId, generalStockOnly);
    }

    // --- 3. ATÖLYE EBATLI STOK SAHASI ---
    @GetMapping("/sized")
    public String workshopSized(@RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                                @RequestParam(value = "customerOnly", defaultValue = "false") boolean customerOnly,
                                @ModelAttribute ListQuery query, Model model) {

        Page<StockItemView> itemsPage = factoryStockService.getSizedStockItemViewsPaged(
                customerId, generalStockOnly, customerOnly, query.getSearch(), query.pageNumber() - 1, query.pageSize());
        var customers = formLookup.customers();

        BigDecimal totalStockM2 = itemsPage.getContent().stream()
                .map(StockItemView::getQuantity).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalItemsCount = itemsPage.getTotalElements();

        model.addAttribute("items", itemsPage.getContent());
        model.addAttribute("page", itemsPage);
        model.addAttribute("customers", customers);
        model.addAttribute("totalStockM2", totalStockM2);
        model.addAttribute("totalItemsCount", totalItemsCount);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("customerOnly", customerOnly);
        model.addAttribute("search", query.getSearch());
        model.addAttribute("grid", WorkshopSizedGrid.view(
                itemsPage, query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        model.addAttribute("activeSection", "workshop");
        model.addAttribute("activeSubSection", "sized");

        return "operations/workshop/sized";
    }

    @GetMapping("/sized/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getWorkshopSizedData(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "25") int size,
            @RequestParam(required = false, name = "search") String search,
            @RequestParam(required = false, name = "customerId") Long customerId,
            @RequestParam(name = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
            @RequestParam(name = "customerOnly", defaultValue = "false") boolean customerOnly) {

        int pageIndex = Math.max(0, page - 1);
        Page<StockItemView> itemsPage = factoryStockService.getSizedStockItemViewsPaged(
                customerId, generalStockOnly, customerOnly, search, pageIndex, size);
        return StockYardResponses.workshopSized(itemsPage);
    }

    // --- 4. ATÖLYE EBATLAMA / YENİDEN EBATLAMA ÜRETİM MODÜLÜ ---
    @GetMapping({"/sized/new", "/sized/create"})
    public String newWorkshopSizedForm(
            @RequestParam(value = "slabId", required = false) Long slabId,
            @RequestParam(value = "sourceStockItemId", required = false) Long sourceStockItemId,
            Model model) {
        var selectedSlab = formLookup.findSlabWithDetails(slabId);
        var selectedSourceItem = formLookup.findStockItemWithCustomer(sourceStockItemId);

        var slabsList = new java.util.ArrayList<>(formLookup.slabsInStatus(List.of(SlabStatus.AVAILABLE, SlabStatus.RESERVED)));
        if (selectedSlab != null && slabsList.stream().noneMatch(s -> s.getId().equals(selectedSlab.getId()))) {
            slabsList.add(0, selectedSlab);
        }

        var sourceSizedList = new java.util.ArrayList<>(formLookup.stockByTypeAndStatus(StockProductType.SIZED, "AVAILABLE"));
        if (selectedSourceItem != null && sourceSizedList.stream().noneMatch(it -> it.getId().equals(selectedSourceItem.getId()))) {
            sourceSizedList.add(0, selectedSourceItem);
        }

        model.addAttribute("slabs", slabsList);
        model.addAttribute("sourceSizedItems", sourceSizedList);
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("projects", formLookup.projects());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("surfaceFinishes", SurfaceFinish.values());
        model.addAttribute("prefillSlabId", slabId);
        model.addAttribute("prefillSourceStockItemId", sourceStockItemId);
        model.addAttribute("selectedSlab", selectedSlab);
        model.addAttribute("selectedSourceItem", selectedSourceItem);
        model.addAttribute("activeSection", "workshop");
        model.addAttribute("activeSubSection", "sized");
        return "operations/workshop/sized-form";
    }

    @PreAuthorize(Constants.PRE_AUTH_WORKSHOP_WRITE)
    @PostMapping({"/sized/new", "/sized/create"})
    public String createWorkshopSizedItem(
            @RequestParam(value = "sourceType", defaultValue = "SLAB") String sourceType,
            @RequestParam(value = "slabId", required = false) Long slabId,
            @RequestParam(value = "sourceStockItemId", required = false) Long sourceStockItemId,
            @RequestParam(value = "consumedQuantity", required = false) BigDecimal consumedQuantity,
            @RequestParam(value = "wasteQuantity", required = false) BigDecimal wasteQuantity,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam(required = false, name = "qualityGrade") String qualityGrade,
            @RequestParam(required = false, name = "surfaceFinish") String surfaceFinish,
            @RequestParam(required = false, name = "edgeFinish") String edgeFinish,
            @RequestParam(required = false, name = "notes") String notes,
            @RequestParam(required = false, name = "rowDescription") List<String> rowDescriptions,
            @RequestParam(required = false, name = "rowThicknessCm") List<BigDecimal> rowThicknesses,
            @RequestParam(required = false, name = "rowWidthCm") List<BigDecimal> rowWidths,
            @RequestParam(required = false, name = "rowLengthCm") List<BigDecimal> rowLengths,
            @RequestParam(required = false, name = "rowPieceCount") List<Integer> rowPieceCounts,
            @RequestParam(required = false, name = "rowQuantity") List<BigDecimal> rowQuantities,
            @RequestParam(required = false, name = "rowSurfaceFinish") List<String> rowSurfaceFinishes,
            @RequestParam(required = false, name = "rowEdgeFinish") List<String> rowEdgeFinishes,
            RedirectAttributes redirectAttributes) {
        try {
            List<StockItemView> created = workshopOperationService.produceWorkshopSizedItemsBatchViews(
                    sourceType, slabId, sourceStockItemId, consumedQuantity, wasteQuantity,
                    customerId, projectId, description, qualityGrade, surfaceFinish, edgeFinish, notes,
                    rowDescriptions, rowThicknesses, rowWidths, rowLengths, rowPieceCounts, rowQuantities,
                    rowSurfaceFinishes, rowEdgeFinishes
            );
            BigDecimal totalM2 = created.stream()
                    .map(i -> i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Atölye ebatlama işlemi başarıyla tamamlandı. " + created.size() + " grup ürün (" + totalM2 + " m²) Atölye Ebatlı Stok Sahasına eklendi.");
            return "redirect:/operations/workshop/sized";
        } catch (Exception e) {
            log.error("Atölye ebatlı ürün üretim hatası", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
            return "redirect:/operations/workshop/sized/new";
        }
    }
}
