package com.ozerler.marble.service;

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
            String notes
    ) {}

    @Transactional
    public OperationWorkOrder createWorkOrder(WorkOrderCreateForm form) {
        Objects.requireNonNull(form.department(), "Departman seçilmelidir.");

        String prefix = form.department() == BusinessUnit.FACTORY ? "FWO"
                : (form.department() == BusinessUnit.WORKSHOP ? "AWO" : "SWO");
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

        OperationWorkOrder saved = workOrderRepository.save(order);

        // Record status history
        recordHistory(saved, null, OperationWorkOrderStatus.NEW, "İş emri oluşturuldu");

        // If stock is allocated and used right away, deduct from stock and record movement
        if (slab != null) {
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
        OperationWorkOrder order = workOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("İş emri bulunamadı: " + orderId));

        OperationWorkOrderStatus prevStatus = order.getStatus();
        order.setStatus(newStatus);
        OperationWorkOrder saved = workOrderRepository.save(order);

        recordHistory(saved, prevStatus, newStatus, notes);

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
        historyRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<OperationWorkOrder> searchOrders(BusinessUnit dept, OperationWorkOrderStatus status,
                                                Long customerId, LocalDate startDate, LocalDate endDate,
                                                String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return workOrderRepository.searchOrders(dept, status, customerId, startDate, endDate, search, pageable);
    }

    @Transactional(readOnly = true)
    public OperationWorkOrder getOrderById(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("İş emri bulunamadı: " + id));
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
}
