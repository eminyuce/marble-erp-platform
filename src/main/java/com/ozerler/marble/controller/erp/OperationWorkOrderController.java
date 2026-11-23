package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.form.WorkOrderRequest;
import com.ozerler.marble.dto.view.OrderRef;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.service.FormLookupService;
import com.ozerler.marble.service.OperationWorkOrderService;
import com.ozerler.marble.web.ListQuery;
import com.ozerler.marble.web.WorkOrderGrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/work-orders")
@RequiredArgsConstructor
@Slf4j
public class OperationWorkOrderController extends AbstractController {

    private static final int DEFAULT_DUE_DAYS = 14;

    private final FormLookupService formLookup;

    private final OperationWorkOrderService workOrderService;

    @GetMapping
    public String index(@RequestParam(value = "department", required = false) BusinessUnit department,
                        @RequestParam(value = "status", required = false) OperationWorkOrderStatus status,
                        @RequestParam(value = "customerId", required = false) Long customerId,
                        @ModelAttribute ListQuery query, Model model) {

        model.addAttribute("grid", WorkOrderGrid.view(
                workOrderService.searchOrderRows(department, status, customerId, null, null, query.getSearch(), query.pageNumber() - 1, query.pageSize()),
                query.pageNumber(), query.pageSize(), query.sortKey(), query.sortDirection()));
        Object valueValue;
        if (department != null) {
            valueValue = department.name();
        } else {
            valueValue = "";
        }
        model.addAttribute("selectedDepartment", valueValue);
        Object valueValue2;
        if (status != null) {
            valueValue2 = status.name();
        } else {
            valueValue2 = "";
        }
        model.addAttribute("selectedStatus", valueValue2);
        model.addAttribute("selectedCustomerId", customerId);
        model.addAttribute("customers", formLookup.customers());
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

        return workOrderService.orderRows(search, department, status, customerId, page, size);
    }

    @GetMapping({"/new", "/create"})
    public String newOrderForm(@RequestParam(value = "department", defaultValue = "FACTORY") BusinessUnit department,
                               @RequestParam(value = "slabId", required = false) Long slabId,
                               @RequestParam(value = "stockItemId", required = false) Long stockItemId,
                               @RequestParam(value = "blockId", required = false) Long blockId,
                               @RequestParam(value = "orderItemId", required = false) Long orderItemId,
                               @RequestParam(value = "customerId", required = false) Long customerId,
                               Model model) {

        var orderItem = formLookup.findSalesOrderItem(orderItemId);

        BigDecimal prefillCount = null;
        String prefillNotes = "";
        String prefillStoneType = "";

        if (orderItem != null) {
            if (orderItem.getBlock() != null && blockId == null) {
                blockId = orderItem.getBlock().getId();
            }
            if (orderItem.getSlab() != null && slabId == null) {
                slabId = orderItem.getSlab().getId();
            }
            if (customerId == null) {
                customerId = orderItem.getCustomerId();
            }
            model.addAttribute("prefillOrderItem", orderItem);
            prefillNotes = "Sipariş: " + Objects.requireNonNullElse(orderItem.getOrderNo(), "")
                    + " | Kalem: " + orderItem.getDescription();
            if (orderItem.getBlock() != null && orderItem.getBlock().getStoneType() != null) {
                prefillStoneType = orderItem.getBlock().getStoneType();
            } else {
                prefillStoneType = orderItem.getDescription();
            }
            if ("adet".equalsIgnoreCase(orderItem.getUnit())) {
                prefillCount = orderItem.getQuantity();
            }
        }

        var selectedSlab = formLookup.findSlab(slabId);
        var selectedStockItem = formLookup.findStockItem(stockItemId);
        var selectedBlock = formLookup.findBlock(blockId);

        var availableBlocks = new ArrayList<>(formLookup.blocksInStatus(
                List.of(BlockStatus.AT_FACTORY, BlockStatus.FACTORY_STOCK, BlockStatus.PRODUCED, BlockStatus.QUARRY, BlockStatus.EXTRACTED, BlockStatus.MARKED)));
        if (selectedBlock != null && availableBlocks.stream().noneMatch(block -> block.getId().equals(selectedBlock.getId()))) {
            availableBlocks.add(0, selectedBlock);
        }

        model.addAttribute("defaultDepartment", department);
        model.addAttribute("selectedSlab", selectedSlab);
        model.addAttribute("selectedStockItem", selectedStockItem);
        model.addAttribute("selectedBlock", selectedBlock);
        model.addAttribute("prefillCustomerId", customerId);
        model.addAttribute("prefillStoneType", prefillStoneType);
        model.addAttribute("prefillNotes", prefillNotes);
        int valueValue;
        if (prefillCount != null) {
            valueValue = prefillCount.intValue();
        } else {
            valueValue = 1;
        }
        model.addAttribute("prefillCount", valueValue);
        model.addAttribute("customers", formLookup.customers());
        model.addAttribute("departments", List.of(BusinessUnit.FACTORY, BusinessUnit.WORKSHOP, BusinessUnit.SITE));
        model.addAttribute("availableBlocks", availableBlocks);
        model.addAttribute("availableSlabs", formLookup.slabsByStatus(SlabStatus.AVAILABLE));
        model.addAttribute("availableStockItems", formLookup.stockByTypeAndStatus(StockProductType.SIZED, "AVAILABLE"));
        model.addAttribute("activeNav", "work-orders");

        return "workorders/form";
    }

    @PreAuthorize(Constants.PRE_AUTH_OPERATOR_WRITE)
    @PostMapping({"", "/", "/new", "/create"})
    public String createOrder(@RequestParam(value = "orderNo", required = false) String orderNo,
                              @RequestParam(value = "customerId", required = false) Long customerId,
                              @RequestParam(value = "orderDate", required = false) String orderDateStr,
                              @RequestParam(value = "dueDate", required = false) String dueDateStr,
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
                              @RequestParam(value = "dimWidthCm", required = false) List<BigDecimal> dimWidths,
                              @RequestParam(value = "dimLengthCm", required = false) List<BigDecimal> dimLengths,
                              @RequestParam(value = "dimThicknessCm", required = false) List<BigDecimal> dimThicknesses,
                              @RequestParam(value = "dimPieceCount", required = false) List<Integer> dimPieceCounts,
                              @RequestParam(value = "dimSurfaceOp", required = false) List<String> dimSurfaceOps,
                              @RequestParam(value = "dimSurfaceUnit", required = false) List<String> dimSurfaceUnits,
                              @RequestParam(value = "dimEdgeOp", required = false) List<String> dimEdgeOps,
                              @RequestParam(value = "dimEdgeUnit", required = false) List<String> dimEdgeUnits,
                              @RequestParam(value = "dimNote", required = false) List<String> dimNotes,
                              RedirectAttributes redirectAttributes) {

        try {
            LocalDate orderDate = parseDateSafely(orderDateStr, LocalDate.now());
            LocalDate dueDate = parseDateSafely(dueDateStr, LocalDate.now().plusDays(DEFAULT_DUE_DAYS));

            WorkOrderRequest request = new WorkOrderRequest(orderNo, customerId, orderDate, dueDate, department,
                    responsiblePerson, stoneType, colorQuality, thicknessCm, widthCm, lengthCm, quantity, quantityUnit,
                    surfaceOperation, edgeOperation, sourceStockItemId, sourceSlabId, sourceBlockId, usedQuantity, notes,
                    new WorkOrderRequest.Dimensions(dimWidths, dimLengths, dimThicknesses, dimPieceCounts, dimSurfaceOps,
                            dimSurfaceUnits, dimEdgeOps, dimEdgeUnits, dimNotes));
            OrderRef order = workOrderService.createWorkOrder(request);
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
        model.addAttribute("order", workOrderService.getOrderView(id));
        model.addAttribute("statuses", OperationWorkOrderStatus.values());
        model.addAttribute("activeNav", "work-orders");
        return "workorders/detail";
    }

    @PreAuthorize(Constants.PRE_AUTH_OPERATOR_WRITE)
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") OperationWorkOrderStatus status,
                               @RequestParam(value = "notes", required = false) String notes,
                               RedirectAttributes redirectAttributes) {
        try {
            OrderRef order = workOrderService.updateStatusRef(id, status, notes);
            redirectAttributes.addFlashAttribute("successMessage", "İş Emri (" + order.getOrderNo() + ") durumu güncellendi: " + status.getLabel());
        } catch (Exception e) {
            log.error("İş emri durumu güncellenemedi: id={}, status={}", id, status, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Durum güncellenirken hata: " + e.getMessage());
        }
        return "redirect:/work-orders/" + id;
    }

    private LocalDate parseDateSafely(String val, LocalDate fallback) {
        if (val == null || val.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(val.trim());
        } catch (Exception e) {
            try {
                return LocalDate.parse(val.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (Exception ex) {
                return fallback;
            }
        }
    }
}
