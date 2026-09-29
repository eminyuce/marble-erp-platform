package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.service.OperationWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.ozerler.marble.dto.TabulatorResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/work-orders")
@RequiredArgsConstructor
@Slf4j
public class OperationWorkOrderController extends AbstractController {

    private final OperationWorkOrderService workOrderService;
    private final CustomerRepository customerRepository;
    private final SlabRepository slabRepository;
    private final StockItemRepository stockItemRepository;
    private final BlockRepository blockRepository;

    @GetMapping
    public String index(@RequestParam(value = "department", required = false) BusinessUnit department,
                        @RequestParam(value = "status", required = false) OperationWorkOrderStatus status,
                        @RequestParam(value = "customerId", required = false) Long customerId,
                        Model model) {

        model.addAttribute("selectedDepartment", department != null ? department.name() : "");
        model.addAttribute("selectedStatus", status != null ? status.name() : "");
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("departments", List.of(BusinessUnit.FACTORY, BusinessUnit.WORKSHOP, BusinessUnit.SITE));
        model.addAttribute("statuses", OperationWorkOrderStatus.values());
        model.addAttribute("activeNav", "work-orders");

        return "workorders/list";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<Map<String, Object>> getOrdersData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "department", required = false) BusinessUnit department,
            @RequestParam(value = "status", required = false) OperationWorkOrderStatus status,
            @RequestParam(value = "customerId", required = false) Long customerId) {

        int pageIndex = Math.max(0, page - 1);
        Page<OperationWorkOrder> ordersPage = workOrderService.searchOrders(department, status, customerId, null, null, search, pageIndex, size);

        List<Map<String, Object>> data = ordersPage.getContent().stream().map(ord -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ord.getId());
            map.put("orderNo", ord.getOrderNo());
            map.put("orderDate", ord.getOrderDate() != null ? ord.getOrderDate().toString() : "");
            map.put("targetDeliveryDate", ord.getDueDate() != null ? ord.getDueDate().toString() : "");
            map.put("department", ord.getDepartment() != null ? ord.getDepartment().name() : "");
            map.put("departmentLabel", ord.getDepartment() != null ? ord.getDepartment().getDisplayName() : "");
            map.put("customerName", ord.getCustomer() != null ? ord.getCustomer().getCompanyName() : "Genel Sipariş");
            map.put("stoneType", ord.getStoneType() != null ? ord.getStoneType() : "—");
            map.put("quantity", ord.getQuantity() != null ? ord.getQuantity() : BigDecimal.ZERO);
            map.put("unit", ord.getUnit() != null ? ord.getUnit() : "m²");
            map.put("responsiblePerson", ord.getResponsiblePerson() != null ? ord.getResponsiblePerson() : "—");
            map.put("status", ord.getStatus() != null ? ord.getStatus().name() : "");
            map.put("statusLabel", ord.getStatus() != null ? ord.getStatus().getDisplayName() : "");
            return map;
        }).toList();

        return TabulatorResponse.of(data, ordersPage.getTotalPages(), ordersPage.getTotalElements());
    }

    @GetMapping({"/new", "/create"})
    public String newOrderForm(@RequestParam(value = "department", defaultValue = "FACTORY") BusinessUnit department,
                               @RequestParam(value = "slabId", required = false) Long slabId,
                               @RequestParam(value = "stockItemId", required = false) Long stockItemId,
                               Model model) {

        Slab selectedSlab = slabId != null ? slabRepository.findById(slabId).orElse(null) : null;
        StockItem selectedStockItem = stockItemId != null ? stockItemRepository.findById(stockItemId).orElse(null) : null;

        model.addAttribute("defaultDepartment", department);
        model.addAttribute("selectedSlab", selectedSlab);
        model.addAttribute("selectedStockItem", selectedStockItem);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("departments", List.of(BusinessUnit.FACTORY, BusinessUnit.WORKSHOP, BusinessUnit.SITE));
        model.addAttribute("availableSlabs", slabRepository.findByStatus(SlabStatus.AVAILABLE));
        model.addAttribute("availableStockItems", stockItemRepository.findByProductTypeAndStatus(StockProductType.SIZED, "AVAILABLE"));
        model.addAttribute("surfaceOperations", workOrderService.getSurfaceOperations());
        model.addAttribute("edgeOperations", workOrderService.getEdgeOperations());
        model.addAttribute("units", workOrderService.getUnits());
        model.addAttribute("activeNav", "work-orders");

        return "workorders/form";
    }

    @PostMapping({"/new", "/create"})
    public String createOrder(@RequestParam(value = "orderNo", required = false) String orderNo,
                              @RequestParam(value = "customerId", required = false) Long customerId,
                              @RequestParam(value = "orderDate", required = false) LocalDate orderDate,
                              @RequestParam(value = "dueDate", required = false) LocalDate dueDate,
                              @RequestParam("department") BusinessUnit department,
                              @RequestParam(value = "responsiblePerson", required = false) String responsiblePerson,
                              @RequestParam(value = "stoneType", required = false) String stoneType,
                              @RequestParam(value = "colorQuality", required = false) String colorQuality,
                              @RequestParam(value = "thicknessCm", required = false) BigDecimal thicknessCm,
                              @RequestParam(value = "widthCm", required = false) BigDecimal widthCm,
                              @RequestParam(value = "lengthCm", required = false) BigDecimal lengthCm,
                              @RequestParam(value = "quantity", required = false) BigDecimal quantity,
                              @RequestParam(value = "quantityUnit", defaultValue = "m2") String quantityUnit,
                              @RequestParam(value = "surfaceOperation", required = false) String surfaceOperation,
                              @RequestParam(value = "edgeOperation", required = false) String edgeOperation,
                              @RequestParam(value = "sourceStockItemId", required = false) Long sourceStockItemId,
                              @RequestParam(value = "sourceSlabId", required = false) Long sourceSlabId,
                              @RequestParam(value = "sourceBlockId", required = false) Long sourceBlockId,
                              @RequestParam(value = "usedQuantity", required = false) BigDecimal usedQuantity,
                              @RequestParam(value = "notes", required = false) String notes,
                              RedirectAttributes redirectAttributes) {

        try {
            OperationWorkOrderService.WorkOrderCreateForm form = new OperationWorkOrderService.WorkOrderCreateForm(
                    orderNo, customerId, orderDate, dueDate, department, responsiblePerson,
                    stoneType, colorQuality, thicknessCm, widthCm, lengthCm, quantity, quantityUnit,
                    surfaceOperation, edgeOperation, sourceStockItemId, sourceSlabId, sourceBlockId,
                    usedQuantity, notes
            );

            OperationWorkOrder order = workOrderService.createWorkOrder(form);
            redirectAttributes.addFlashAttribute("successMessage", "İş Emri (" + order.getOrderNo() + ") başarıyla oluşturuldu. Stok bağlantısı ve tüketim hareketi kaydedildi.");
            return "redirect:/work-orders/" + order.getId();
        } catch (Exception e) {
            log.error("İş emri oluşturulamadı", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Hata: " + e.getMessage());
            return "redirect:/work-orders/new?department=" + department.name();
        }
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        OperationWorkOrder order = workOrderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("statuses", OperationWorkOrderStatus.values());
        model.addAttribute("activeNav", "work-orders");
        return "workorders/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") OperationWorkOrderStatus status,
                               @RequestParam(value = "notes", required = false) String notes,
                               RedirectAttributes redirectAttributes) {
        try {
            OperationWorkOrder order = workOrderService.updateStatus(id, status, notes);
            redirectAttributes.addFlashAttribute("successMessage", "İş Emri (" + order.getOrderNo() + ") durumu güncellendi: " + status.getLabel());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Durum güncellenirken hata: " + e.getMessage());
        }
        return "redirect:/work-orders/" + id;
    }
}
