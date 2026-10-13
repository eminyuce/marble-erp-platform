package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.service.FactoryStockService;
import com.ozerler.marble.service.OperationWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/operations/workshop")
@RequiredArgsConstructor
@Slf4j
public class WorkshopOperationController extends AbstractController {

    private final OperationWorkOrderService workOrderService;
    private final FactoryStockService factoryStockService;
    private final CustomerRepository customerRepository;

    @GetMapping
    public String index(@RequestParam(value = "status", required = false) OperationWorkOrderStatus status,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size,
                        Model model) {

        Page<OperationWorkOrder> ordersPage = workOrderService.searchOrders(BusinessUnit.WORKSHOP, status, null, null, null, null, page, size);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("page", ordersPage);
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
        return "redirect:/production/pallets?department=WORKSHOP";
    }

    // --- 2. ATÖLYE PLAKA STOK SAHASI ---
    @GetMapping("/slabs")
    public String workshopSlabs(@RequestParam(value = "status", required = false) SlabStatus status,
                                @RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "20") int size,
                                Model model) {

        Page<Slab> slabsPage = factoryStockService.getFactorySlabsPaged(status, customerId, generalStockOnly, search, page, size);
        List<Customer> customers = customerRepository.findAll();

        BigDecimal totalAvailableM2 = slabsPage.getContent().stream()
                .map(Slab::getSurfaceAreaM2).filter(Objects::nonNull)
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
        model.addAttribute("activeSection", "workshop");
        model.addAttribute("activeSubSection", "slabs");

        return "operations/workshop/slabs";
    }

    @GetMapping("/slabs/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getWorkshopSlabsData(
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

    // --- 3. ATÖLYE EBATLI STOK SAHASI ---
    @GetMapping("/sized")
    public String workshopSized(@RequestParam(value = "customerId", required = false) Long customerId,
                                @RequestParam(value = "generalStockOnly", defaultValue = "false") boolean generalStockOnly,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "20") int size,
                                Model model) {

        Page<StockItem> itemsPage = factoryStockService.getSizedStockItemsPaged(customerId, generalStockOnly, search, page, size);
        List<Customer> customers = customerRepository.findAll();

        BigDecimal totalStockM2 = itemsPage.getContent().stream()
                .map(StockItem::getQuantity).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalItemsCount = itemsPage.getTotalElements();

        model.addAttribute("items", itemsPage.getContent());
        model.addAttribute("page", itemsPage);
        model.addAttribute("customers", customers);
        model.addAttribute("totalStockM2", totalStockM2);
        model.addAttribute("totalItemsCount", totalItemsCount);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("generalStockOnly", generalStockOnly);
        model.addAttribute("search", search);
        model.addAttribute("activeSection", "workshop");
        model.addAttribute("activeSubSection", "sized");

        return "operations/workshop/sized";
    }

    @GetMapping("/sized/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getWorkshopSizedData(
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
            String sourceBlockCode = "—";
            if (it.getSourceBlock() != null) {
                try {
                    sourceBlockCode = it.getSourceBlock().getBlockCode();
                } catch (Exception e) {
                    sourceBlockCode = "—";
                }
            }
            map.put("sourceBlockCode", sourceBlockCode != null ? sourceBlockCode : "—");
            map.put("stoneType", it.getStoneType() != null ? it.getStoneType() : "Mermer");
            map.put("description", it.getDescription() != null ? it.getDescription() : "Ebatlı Mermer");
            map.put("surfaceFinish", it.getSurfaceFinish() != null ? it.getSurfaceFinish().getDisplayName() : "Cilalı");
            map.put("thicknessCm", it.getThicknessCm() != null ? it.getThicknessCm() + " cm" : "2 cm");
            map.put("dimensions", (it.getWidthCm() != null && it.getLengthCm() != null) ? (it.getWidthCm() + "×" + it.getLengthCm() + " cm") : "—");
            map.put("quantity", it.getQuantity());
            map.put("unit", it.getUnit() != null ? it.getUnit() : "m²");
            map.put("pieceCount", it.getPieceCount() != null ? it.getPieceCount() : "—");
            String crateLoc = "—";
            if (it.getStockLocation() != null) {
                try {
                    crateLoc = it.getStockLocation().getName();
                } catch (Exception ignored) {
                }
            }
            map.put("crateNumber", crateLoc);
            map.put("customerName", it.getCustomer() != null ? it.getCustomer().getCompanyName() : null);
            map.put("customerId", it.getCustomer() != null ? it.getCustomer().getId() : null);
            map.put("isGeneralStock", it.getCustomer() == null);
            map.put("statusLabel", "Stokta");
            map.put("createdDate", it.getCreatedDate() != null ? it.getCreatedDate().format(dateFmt) : "—");
            map.put("notes", it.getNotes() != null ? it.getNotes() : "");
            return map;
        }).collect(Collectors.toList());

        return TabulatorResponse.of(data, itemsPage.getTotalPages(), itemsPage.getTotalElements());
    }
}
