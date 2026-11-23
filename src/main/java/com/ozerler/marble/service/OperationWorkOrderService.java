package com.ozerler.marble.service;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.OperationWorkOrderItem;
import java.util.ArrayList;
import com.ozerler.marble.dto.view.OrderRef;
import com.ozerler.marble.dto.form.WorkOrderRequest;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.security.SecurityUtils;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationWorkOrderService {

    private final OperationWorkOrderRepository workOrderRepository;
    private final com.ozerler.marble.mapper.WorkOrderMapper workOrderMapper;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final StockItemRepository stockItemRepository;
    private final SlabRepository slabRepository;
    private final BlockRepository blockRepository;
    private final StockMovementService stockMovementService;
    private final OperationDefinitionRepository definitionRepository;

    public record WorkOrderCreateForm(
            String orderNo,
            Long customerId,
            LocalDate orderDate,
            LocalDate dueDate,
            BusinessUnit department,
            String responsiblePerson,
            String stoneType,
            String colorQuality,
            BigDecimal thicknessCm,
            BigDecimal widthCm,
            BigDecimal lengthCm,
            BigDecimal quantity,
            String quantityUnit,
            String surfaceOperation,
            String edgeOperation,
            Long sourceStockItemId,
            Long sourceSlabId,
            Long sourceBlockId,
            BigDecimal usedQuantity,
            String notes,
            List<OperationWorkOrderItem> items
    ) {
        public WorkOrderCreateForm(
                String orderNo, Long customerId, LocalDate orderDate, LocalDate dueDate,
                BusinessUnit department, String responsiblePerson, String stoneType,
                String colorQuality, BigDecimal thicknessCm, BigDecimal widthCm,
                BigDecimal lengthCm, BigDecimal quantity, String quantityUnit,
                String surfaceOperation, String edgeOperation, Long sourceStockItemId,
                Long sourceSlabId, Long sourceBlockId, BigDecimal usedQuantity, String notes
        ) {
            this(orderNo, customerId, orderDate, dueDate, department, responsiblePerson,
                    stoneType, colorQuality, thicknessCm, widthCm, lengthCm, quantity,
                    quantityUnit, surfaceOperation, edgeOperation, sourceStockItemId,
                    sourceSlabId, sourceBlockId, usedQuantity, notes, List.of());
        }
    }

    @Transactional
    public OperationWorkOrder createWorkOrder(WorkOrderCreateForm form) {
        Objects.requireNonNull(form.department(), "Departman seçilmelidir.");

        String prefix = form.department() == BusinessUnit.FACTORY ? "FWO"
                : form.department() == BusinessUnit.WORKSHOP ? "AWO" : "SWO";
        String code = (form.orderNo() != null && !form.orderNo().isBlank())
                ? form.orderNo().trim().toUpperCase()
                : UniqueCodes.yearly(prefix, workOrderRepository::existsByOrderNo);

        Customer customer = form.customerId() != null ? customerRepository.findById(form.customerId()).orElse(null) : null;
        StockItem stockItem = form.sourceStockItemId() != null ? stockItemRepository.findById(form.sourceStockItemId()).orElse(null) : null;
        Slab slab = form.sourceSlabId() != null ? slabRepository.findById(form.sourceSlabId()).orElse(null) : null;
        Block block = form.sourceBlockId() != null ? blockRepository.findById(form.sourceBlockId()).orElse(null) : null;

        OperationWorkOrder order = OperationWorkOrder.builder()
                .orderNo(code)
                .customer(customer)
                .orderDate(form.orderDate() != null ? form.orderDate() : LocalDate.now())
                .dueDate(form.dueDate())
                .department(form.department())
                .responsiblePerson(form.responsiblePerson())
                .stoneType(form.stoneType())
                .colorQuality(form.colorQuality())
                .thicknessCm(form.thicknessCm())
                .widthCm(form.widthCm())
                .lengthCm(form.lengthCm())
                .quantity(form.quantity())
                .quantityUnit(form.quantityUnit() != null ? form.quantityUnit() : "m2")
                .surfaceOperation(form.surfaceOperation())
                .edgeOperation(form.edgeOperation())
                .status(OperationWorkOrderStatus.NEW)
                .sourceStockItem(stockItem)
                .sourceSlab(slab)
                .sourceBlock(block)
                .usedQuantity(form.usedQuantity())
                .notes(form.notes())
                .build();

        if (form.items() != null && !form.items().isEmpty()) {
            for (OperationWorkOrderItem it : form.items()) {
                order.addItem(it);
            }
        }

        OperationWorkOrder saved = workOrderRepository.save(order);

        // Record status history
        recordHistory(saved, null, OperationWorkOrderStatus.NEW, "İş emri oluşturuldu");

        // If stock is allocated and used right away, deduct from stock and record movement
        if (block != null) {
            block.setStatus(BlockStatus.IN_PROCESS);
            if (customer != null && block.getAssignedCustomer() == null) {
                block.setAssignedCustomer(customer);
            }
            blockRepository.save(block);
            BigDecimal ton = block.getEffectiveTonnage() != null ? block.getEffectiveTonnage() : BigDecimal.ONE;
            stockMovementService.recordWorkOrderConsumption(saved, null, null, block,
                    form.usedQuantity() != null ? form.usedQuantity() : ton, "ton");
        } else if (slab != null) {
            slab.setStatus(SlabStatus.RESERVED);
            if (customer != null) slab.setCustomer(customer);
            slabRepository.save(slab);
            stockMovementService.recordWorkOrderConsumption(saved, null, slab, null,
                    form.usedQuantity() != null ? form.usedQuantity() : slab.getSurfaceAreaM2(), "m2");
        } else if (stockItem != null && form.usedQuantity() != null) {
            BigDecimal remaining = stockItem.getQuantity().subtract(form.usedQuantity());
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
            stockItem.setQuantity(remaining);
            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                stockItem.setStatus("USED_IN_PRODUCTION");
            }
            stockItemRepository.save(stockItem);
            stockMovementService.recordWorkOrderConsumption(saved, stockItem, null, null,
                    form.usedQuantity(), stockItem.getUnit());
        }

        return saved;
    }

    @Transactional
    public OperationWorkOrder updateStatus(Long orderId, OperationWorkOrderStatus newStatus, String notes) {
        OperationWorkOrder order = workOrderRepository.findByIdWithDetails(orderId)
                .or(() -> workOrderRepository.findById(orderId))
                .orElseThrow(() -> new IllegalArgumentException("İş emri bulunamadı: " + orderId));

        OperationWorkOrderStatus prevStatus = order.getStatus();
        order.setStatus(newStatus);

        if (newStatus == OperationWorkOrderStatus.IN_PRODUCTION && order.getSourceBlock() != null) {
            Block b = order.getSourceBlock();
            if (b.getStatus() != BlockStatus.IN_PROCESS) {
                b.setStatus(BlockStatus.IN_PROCESS);
                blockRepository.save(b);
            }
        }

        OperationWorkOrder saved = workOrderRepository.save(order);

        recordHistory(saved, prevStatus, newStatus, notes);

        initializeDetails(saved);

        return saved;
    }

    private void recordHistory(OperationWorkOrder order, OperationWorkOrderStatus prev, OperationWorkOrderStatus next, String notes) {
        String user = SecurityUtils.getCurrentUserLogin().orElse("system");
        WorkOrderStatusHistory history = WorkOrderStatusHistory.builder()
                .workOrder(order)
                .previousStatus(prev)
                .newStatus(next)
                .changedBy(user)
                .changedAt(LocalDateTime.now())
                .notes(notes)
                .build();
        WorkOrderStatusHistory savedHistory = historyRepository.save(history);
        if (order.getStatusHistory() != null) {
            order.getStatusHistory().add(0, savedHistory);
        }
    }

    @Transactional(readOnly = true)
    public Page<OperationWorkOrder> searchOrders(BusinessUnit dept, OperationWorkOrderStatus status,
                                                Long customerId, LocalDate startDate, LocalDate endDate,
                                                String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<OperationWorkOrder> pageResult = workOrderRepository.searchOrders(
                dept, status, customerId, startDate, endDate, search, pageable);
        pageResult.getContent().forEach(this::initializeDetails);
        return pageResult;
    }

    @Transactional(readOnly = true)
    public OperationWorkOrder getOrderById(Long id) {
        OperationWorkOrder order = workOrderRepository.findByIdWithDetails(id)
                .or(() -> workOrderRepository.findById(id))
                .orElseThrow(() -> new IllegalArgumentException("İş emri bulunamadı: " + id));
        initializeDetails(order);
        return order;
    }

    private void initializeDetails(OperationWorkOrder order) {
        if (order == null) return;
        if (order.getCustomer() != null) {
            order.rememberCustomerName(order.getCustomer().getCompanyName());
        } else {
            order.rememberCustomerName(null);
        }
        if (order.getSourceBlock() != null) {
            order.getSourceBlock().getBlockCode();
        }
        if (order.getSourceSlab() != null) {
            order.getSourceSlab().getSlabCode();
        }
        if (order.getSourceStockItem() != null) {
            order.getSourceStockItem().getItemCode();
        }
        if (order.getItems() != null) {
            log.trace("Initialized {} work order items", order.getItems().size());
        }
        if (order.getStatusHistory() != null) {
            log.trace("Initialized {} work order status rows", order.getStatusHistory().size());
        }
    }

    @Transactional(readOnly = true)
    public List<OperationDefinition> getSurfaceOperations() {
        return definitionRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAsc("SURFACE_OPERATION");
    }

    @Transactional(readOnly = true)
    public List<OperationDefinition> getEdgeOperations() {
        return definitionRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAsc("EDGE_OPERATION");
    }

    @Transactional(readOnly = true)
    public List<OperationDefinition> getUnits() {
        return definitionRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAsc("UNIT");
    }

    /**
     * Turns the posted form (header plus optional dimension rows) into a work order. Row areas, running
     * metres, the breakdown note and the effective header quantity are calculated here.
     *
     * @param request the posted form
     * @return id and number of the new order
     */
    @Transactional
    public OrderRef createWorkOrder(WorkOrderRequest request) {
        String orderNo = request.orderNo();
        Long customerId = request.customerId();
        LocalDate orderDate = request.orderDate();
        LocalDate dueDate = request.dueDate();
        BusinessUnit department = request.department();
        String responsiblePerson = request.responsiblePerson();
        String stoneType = request.stoneType();
        String colorQuality = request.colorQuality();
        BigDecimal thicknessCm = request.thicknessCm();
        BigDecimal widthCm = request.widthCm();
        BigDecimal lengthCm = request.lengthCm();
        BigDecimal quantity = request.quantity();
        String quantityUnit = request.quantityUnit();
        String surfaceOperation = request.surfaceOperation();
        String edgeOperation = request.edgeOperation();
        Long sourceStockItemId = request.sourceStockItemId();
        Long sourceSlabId = request.sourceSlabId();
        Long sourceBlockId = request.sourceBlockId();
        BigDecimal usedQuantity = request.usedQuantity();
        String notes = request.notes();
        WorkOrderRequest.Dimensions dims = request.dimensions();
        List<BigDecimal> dimWidths = dims.widths();
        List<BigDecimal> dimLengths = dims.lengths();
        List<BigDecimal> dimThicknesses = dims.thicknesses();
        List<Integer> dimPieceCounts = dims.pieceCounts();
        List<String> dimSurfaceOps = dims.surfaceOps();
        List<String> dimSurfaceUnits = dims.surfaceUnits();
        List<String> dimEdgeOps = dims.edgeOps();
        List<String> dimEdgeUnits = dims.edgeUnits();
        List<String> dimNotes = dims.notes();
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
                        ? dimSurfaceOps.get(i).trim() : surfaceOperation != null ? surfaceOperation : "Cilalı";
                String sUnit = (dimSurfaceUnits != null && i < dimSurfaceUnits.size() && dimSurfaceUnits.get(i) != null && !dimSurfaceUnits.get(i).isBlank())
                        ? dimSurfaceUnits.get(i).trim() : "m2";
                String eOp = (dimEdgeOps != null && i < dimEdgeOps.size() && dimEdgeOps.get(i) != null && !dimEdgeOps.get(i).isBlank())
                        ? dimEdgeOps.get(i).trim() : edgeOperation != null ? edgeOperation : "Pah";
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
                        i + 1, w, l, t, pcs, rowM2, rowMt, sOp, sUnit, eOp, eUnit, dNote.isEmpty() ? "" : " {" + dNote + "}"));
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
                : !items.isEmpty() && items.get(0).getSurfaceOperation() != null ? items.get(0).getSurfaceOperation() : "Cilalı";
        String topEdge = (edgeOperation != null && !edgeOperation.isBlank())
                ? edgeOperation
                : !items.isEmpty() && items.get(0).getEdgeOperation() != null ? items.get(0).getEdgeOperation() : "Pah";

        String finalNotes = notes != null ? notes.trim() : "";
        if (breakdownBuilder.length() > 0) {
            finalNotes = breakdownBuilder.toString() + (finalNotes.isEmpty() ? "" : "\n\nEk Notlar: " + finalNotes);
        }

        WorkOrderCreateForm form = new WorkOrderCreateForm(
                orderNo, customerId, orderDate, dueDate, department, responsiblePerson,
                stoneType, colorQuality, effectiveThickness, effectiveWidth, effectiveLength, effectiveQuantity, quantityUnit,
                topSurface, topEdge, sourceStockItemId, sourceSlabId, sourceBlockId,
                usedQuantity, finalNotes, items
        );

        return workOrderMapper.toRef(createWorkOrder(form));
    }

    /**
     * Changes the status and returns a reference for the confirmation message.
     *
     * @param orderId   order id
     * @param newStatus new status
     * @param notes     optional notes
     * @return id and number of the order
     */
    @Transactional
    public OrderRef updateStatusRef(Long orderId, OperationWorkOrderStatus newStatus, String notes) {
        return workOrderMapper.toRef(updateStatus(orderId, newStatus, notes));
    }

    /**
     * Builds grid rows inside the read transaction (moved from the controller unchanged).
     *
     * @return grid rows
     */
    @Transactional(readOnly = true)
    public com.ozerler.marble.dto.TabulatorResponse<java.util.Map<String, Object>> orderRows(String search, BusinessUnit department, OperationWorkOrderStatus status, Long customerId, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        Page<OperationWorkOrder> ordersPage = searchOrders(department, status, customerId, null, null, search, pageIndex, size);

        List<java.util.Map<String, Object>> data = ordersPage.getContent().stream().map(ord -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", ord.getId());
            map.put("orderNo", ord.getOrderNo());
            map.put("orderDate", ord.getOrderDate() != null ? ord.getOrderDate().toString() : "");
            map.put("targetDeliveryDate", ord.getDueDate() != null ? ord.getDueDate().toString() : "");
            map.put("department", ord.getDepartment() != null ? ord.getDepartment().name() : "");
            map.put("departmentLabel", ord.getDepartment() != null ? ord.getDepartment().getDisplayName() : "");
            map.put("customerName", ord.getCustomerDisplayName());
            map.put("stoneType", ord.getStoneType() != null ? ord.getStoneType() : "—");
            map.put("quantity", ord.getQuantity() != null ? ord.getQuantity() : BigDecimal.ZERO);
            map.put("unit", ord.getUnit() != null ? ord.getUnit() : "m²");
            map.put("responsiblePerson", ord.getResponsiblePerson() != null ? ord.getResponsiblePerson() : "—");
            map.put("status", ord.getStatus() != null ? ord.getStatus().name() : "");
            map.put("statusLabel", ord.getStatus() != null ? ord.getStatus().getDisplayName() : "");
            return map;
        }).toList();

        return com.ozerler.marble.dto.TabulatorResponse.of(data, ordersPage.getTotalPages(), ordersPage.getTotalElements());
    }

    /**
     * Loads a work order with items, history and sources and copies it into a view.
     *
     * @param id work order id
     * @return the work order view
     */
    @Transactional(readOnly = true)
    public com.ozerler.marble.dto.view.WorkOrderView getOrderView(Long id) {
        OperationWorkOrder order = getOrderById(id);
        com.ozerler.marble.dto.view.WorkOrderView view = workOrderMapper.toDto(order);
        view.setCustomerNameForView(order.getCustomerNameForView());
        return view;
    }

    /**
     * Same as {@code searchOrders}, copied into a view inside the transaction.
     *
     * @param dept see {@code searchOrders}
     * @param status see {@code searchOrders}
     * @param customerId see {@code searchOrders}
     * @param startDate see {@code searchOrders}
     * @param endDate see {@code searchOrders}
     * @param search see {@code searchOrders}
     * @param page see {@code searchOrders}
     * @param size see {@code searchOrders}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.view.WorkOrderView> searchOrderRows(BusinessUnit dept, OperationWorkOrderStatus status,
                                                Long customerId, LocalDate startDate, LocalDate endDate,
                                                String search, int page, int size) {
        return searchOrders(dept, status, customerId, startDate, endDate, search, page, size).map(workOrderMapper::toRow);
    }
}
