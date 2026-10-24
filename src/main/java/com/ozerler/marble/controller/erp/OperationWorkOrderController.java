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

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final SalesOrderItemRepository salesOrderItemRepository;

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
                               @RequestParam(value = "blockId", required = false) Long blockId,
                               @RequestParam(value = "orderItemId", required = false) Long orderItemId,
                               @RequestParam(value = "customerId", required = false) Long customerId,
                               Model model) {

        SalesOrderItem orderItem = orderItemId != null
                ? salesOrderItemRepository.findByIdWithDetails(orderItemId).orElse(null)
                : null;

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
            if (customerId == null && orderItem.getSalesOrder() != null && orderItem.getSalesOrder().getCustomer() != null) {
                customerId = orderItem.getSalesOrder().getCustomer().getId();
            }
            model.addAttribute("prefillOrderItem", orderItem);
            prefillNotes = "Sipariş: " + (orderItem.getSalesOrder() != null ? orderItem.getSalesOrder().getOrderNo() : "")
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

        Slab selectedSlab = slabId != null ? slabRepository.findById(slabId).orElse(null) : null;
        StockItem selectedStockItem = stockItemId != null ? stockItemRepository.findById(stockItemId).orElse(null) : null;
        Block selectedBlock = blockId != null ? blockRepository.findById(blockId).orElse(null) : null;

        List<Block> availableBlocks = blockRepository.findByStatusInOrderByBlockCodeAsc(
                List.of(BlockStatus.AT_FACTORY, BlockStatus.FACTORY_STOCK, BlockStatus.PRODUCED, BlockStatus.QUARRY, BlockStatus.EXTRACTED, BlockStatus.MARKED)
        );
        if (selectedBlock != null && !availableBlocks.contains(selectedBlock)) {
            availableBlocks = new java.util.ArrayList<>(availableBlocks);
            availableBlocks.add(0, selectedBlock);
        }

        model.addAttribute("defaultDepartment", department);
        model.addAttribute("selectedSlab", selectedSlab);
        model.addAttribute("selectedStockItem", selectedStockItem);
        model.addAttribute("selectedBlock", selectedBlock);
        model.addAttribute("prefillCustomerId", customerId);
        model.addAttribute("prefillStoneType", prefillStoneType);
        model.addAttribute("prefillNotes", prefillNotes);
        model.addAttribute("prefillCount", prefillCount != null ? prefillCount.intValue() : 1);
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("departments", List.of(BusinessUnit.FACTORY, BusinessUnit.WORKSHOP, BusinessUnit.SITE));
        model.addAttribute("availableBlocks", availableBlocks);
        model.addAttribute("availableSlabs", slabRepository.findByStatus(SlabStatus.AVAILABLE));
        model.addAttribute("availableStockItems", stockItemRepository.findByProductTypeAndStatus(StockProductType.SIZED, "AVAILABLE"));
        model.addAttribute("surfaceOperations", workOrderService.getSurfaceOperations());
        model.addAttribute("edgeOperations", workOrderService.getEdgeOperations());
        model.addAttribute("units", workOrderService.getUnits());
        model.addAttribute("activeNav", "work-orders");

        return "workorders/form";
    }

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
            LocalDate dueDate = parseDateSafely(dueDateStr, LocalDate.now().plusDays(14));

            List<OperationWorkOrderItem> items = new ArrayList<>();
            BigDecimal effectiveWidth = widthCm;
            BigDecimal effectiveLength = lengthCm;
            BigDecimal effectiveThickness = thicknessCm;
            BigDecimal effectiveQuantity = quantity;
            StringBuilder breakdownBuilder = new StringBuilder();

            if (dimWidths != null && !dimWidths.isEmpty()) {
                breakdownBuilder.append("=== SİPARİŞ ÖLÇÜ & İŞLEM LİSTESİ ===\n");
                BigDecimal sumM2 = BigDecimal.ZERO;
                BigDecimal sumMt = BigDecimal.ZERO;
                int totalPieces = 0;
                for (int i = 0; i < dimWidths.size(); i++) {
                    BigDecimal w = dimWidths.get(i);
                    if (w == null || w.compareTo(BigDecimal.ZERO) <= 0) continue;
                    BigDecimal l = (dimLengths != null && i < dimLengths.size()) ? dimLengths.get(i) : BigDecimal.ZERO;
                    BigDecimal t = (dimThicknesses != null && i < dimThicknesses.size()) ? dimThicknesses.get(i) : new BigDecimal("2.0");
                    int pcs = (dimPieceCounts != null && i < dimPieceCounts.size() && dimPieceCounts.get(i) != null) ? dimPieceCounts.get(i) : 1;
                    String sOp = (dimSurfaceOps != null && i < dimSurfaceOps.size() && dimSurfaceOps.get(i) != null && !dimSurfaceOps.get(i).isBlank())
                            ? dimSurfaceOps.get(i).trim() : (surfaceOperation != null ? surfaceOperation : "Cilalı");
                    String sUnit = (dimSurfaceUnits != null && i < dimSurfaceUnits.size() && dimSurfaceUnits.get(i) != null && !dimSurfaceUnits.get(i).isBlank())
                            ? dimSurfaceUnits.get(i).trim() : "m2";
                    String eOp = (dimEdgeOps != null && i < dimEdgeOps.size() && dimEdgeOps.get(i) != null && !dimEdgeOps.get(i).isBlank())
                            ? dimEdgeOps.get(i).trim() : (edgeOperation != null ? edgeOperation : "Pah");
                    String eUnit = (dimEdgeUnits != null && i < dimEdgeUnits.size() && dimEdgeUnits.get(i) != null && !dimEdgeUnits.get(i).isBlank())
                            ? dimEdgeUnits.get(i).trim() : "m.t.";
                    String dNote = (dimNotes != null && i < dimNotes.size() && dimNotes.get(i) != null) ? dimNotes.get(i).trim() : "";

                    BigDecimal rowM2 = w.multiply(l).multiply(BigDecimal.valueOf(pcs)).divide(BigDecimal.valueOf(10000), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal rowMt = l.multiply(BigDecimal.valueOf(pcs)).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                    sumM2 = sumM2.add(rowM2);
                    sumMt = sumMt.add(rowMt);
                    totalPieces += pcs;

                    if (effectiveWidth == null) effectiveWidth = w;
                    if (effectiveLength == null) effectiveLength = l;
                    if (effectiveThickness == null) effectiveThickness = t;

                    OperationWorkOrderItem item = OperationWorkOrderItem.builder()
                            .thicknessCm(t)
                            .widthCm(w)
                            .lengthCm(l)
                            .pieceCount(pcs)
                            .calculatedM2(rowM2)
                            .calculatedMt(rowMt)
                            .surfaceOperation(sOp)
                            .surfaceUnit(sUnit)
                            .edgeOperation(eOp)
                            .edgeUnit(eUnit)
                            .notes(dNote)
                            .itemOrder(i)
                            .build();
                    items.add(item);

                    breakdownBuilder.append(String.format("%d. %s x %s x %s cm — %d Adet (%s m² | %s m.t.) [Yüzey: %s (%s), Kenar: %s (%s)]%s\n",
                            (i + 1), w, l, t, pcs, rowM2, rowMt, sOp, sUnit, eOp, eUnit, dNote.isEmpty() ? "" : " {" + dNote + "}"));
                }
                breakdownBuilder.append(String.format("-------------------------------------------\nTOPLAM: %d Adet | %s m² | %s m.t.\n", totalPieces, sumM2, sumMt));

                if (effectiveQuantity == null || effectiveQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    if ("adet".equalsIgnoreCase(quantityUnit)) {
                        effectiveQuantity = BigDecimal.valueOf(totalPieces);
                    } else if ("m.t.".equalsIgnoreCase(quantityUnit) || "mt".equalsIgnoreCase(quantityUnit)) {
                        effectiveQuantity = sumMt;
                    } else {
                        effectiveQuantity = sumM2;
                    }
                }
            }

            String topSurface = (surfaceOperation != null && !surfaceOperation.isBlank())
                    ? surfaceOperation
                    : (!items.isEmpty() && items.get(0).getSurfaceOperation() != null ? items.get(0).getSurfaceOperation() : "Cilalı");
            String topEdge = (edgeOperation != null && !edgeOperation.isBlank())
                    ? edgeOperation
                    : (!items.isEmpty() && items.get(0).getEdgeOperation() != null ? items.get(0).getEdgeOperation() : "Pah");

            String finalNotes = notes != null ? notes.trim() : "";
            if (breakdownBuilder.length() > 0) {
                finalNotes = breakdownBuilder.toString() + (finalNotes.isEmpty() ? "" : "\n\nEk Notlar: " + finalNotes);
            }

            OperationWorkOrderService.WorkOrderCreateForm form = new OperationWorkOrderService.WorkOrderCreateForm(
                    orderNo, customerId, orderDate, dueDate, department, responsiblePerson,
                    stoneType, colorQuality, effectiveThickness, effectiveWidth, effectiveLength, effectiveQuantity, quantityUnit,
                    topSurface, topEdge, sourceStockItemId, sourceSlabId, sourceBlockId,
                    usedQuantity, finalNotes, items
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
            log.error("İş emri durumu güncellenemedi: id={}, status={}", id, status, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Durum güncellenirken hata: " + e.getMessage());
        }
        return "redirect:/work-orders/" + id;
    }

    private LocalDate parseDateSafely(String val, LocalDate fallback) {
        if (val == null || val.isBlank()) return fallback;
        try {
            return LocalDate.parse(val.trim());
        } catch (Exception e) {
            try {
                return LocalDate.parse(val.trim(), java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (Exception ex) {
                return fallback;
            }
        }
    }
}
