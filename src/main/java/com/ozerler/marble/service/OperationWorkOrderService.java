package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.form.WorkOrderRequest;
import com.ozerler.marble.dto.view.OrderRef;
import com.ozerler.marble.dto.view.WorkOrderView;
import com.ozerler.marble.mapper.WorkOrderMapper;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.OperationDefinition;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.OperationWorkOrderItem;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.WorkOrderStatusHistory;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.OperationDefinitionRepository;
import com.ozerler.marble.repository.OperationWorkOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.WorkOrderStatusHistoryRepository;
import com.ozerler.marble.security.SecurityUtils;
import com.ozerler.marble.util.UniqueCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationWorkOrderService {

    private static final int CM2_PER_M2 = 10000;

    private final OperationWorkOrderRepository workOrderRepository;
    private final WorkOrderMapper workOrderMapper;
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

        String prefix;
        if (form.department() == BusinessUnit.FACTORY) {
            prefix = "FWO";
        } else {
            if (form.department() == BusinessUnit.WORKSHOP) {
                prefix = "AWO";
            } else {
                prefix = "SWO";
            }
        }
        String code;
        if (form.orderNo() != null && !form.orderNo().isBlank()) {
            code = form.orderNo().trim().toUpperCase();
        } else {
            code = UniqueCodes.yearly(prefix, workOrderRepository::existsByOrderNo);
        }

        Customer customer;
        if (form.customerId() != null) {
            customer = customerRepository.findById(form.customerId()).orElse(null);
        } else {
            customer = null;
        }
        StockItem stockItem;
        if (form.sourceStockItemId() != null) {
            stockItem = stockItemRepository.findById(form.sourceStockItemId()).orElse(null);
        } else {
            stockItem = null;
        }
        Slab slab;
        if (form.sourceSlabId() != null) {
            slab = slabRepository.findById(form.sourceSlabId()).orElse(null);
        } else {
            slab = null;
        }
        Block block;
        if (form.sourceBlockId() != null) {
            block = blockRepository.findById(form.sourceBlockId()).orElse(null);
        } else {
            block = null;
        }

        LocalDate orderDate;
        if (form.orderDate() != null) {
            orderDate = form.orderDate();
        } else {
            orderDate = LocalDate.now();
        }
        String quantityUnit;
        if (form.quantityUnit() != null) {
            quantityUnit = form.quantityUnit();
        } else {
            quantityUnit = "m2";
        }
        OperationWorkOrder order = OperationWorkOrder.builder()
                .orderNo(code)
                .customer(customer)
                .orderDate(orderDate)
                .dueDate(form.dueDate())
                .department(form.department())
                .responsiblePerson(form.responsiblePerson())
                .stoneType(form.stoneType())
                .colorQuality(form.colorQuality())
                .thicknessCm(form.thicknessCm())
                .widthCm(form.widthCm())
                .lengthCm(form.lengthCm())
                .quantity(form.quantity())
                .quantityUnit(quantityUnit)
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
            BigDecimal ton;
            if (block.getEffectiveTonnage() != null) {
                ton = block.getEffectiveTonnage();
            } else {
                ton = BigDecimal.ONE;
            }
            BigDecimal recordWorkOrderConsumption;
            if (form.usedQuantity() != null) {
                recordWorkOrderConsumption = form.usedQuantity();
            } else {
                recordWorkOrderConsumption = ton;
            }
            stockMovementService.recordWorkOrderConsumption(saved, null, null, block,
                    recordWorkOrderConsumption, "ton");
        } else if (slab != null) {
            slab.setStatus(SlabStatus.RESERVED);
            if (customer != null) {
                slab.setCustomer(customer);
            }
            slabRepository.save(slab);
            BigDecimal recordWorkOrderConsumptionValue;
            if (form.usedQuantity() != null) {
                recordWorkOrderConsumptionValue = form.usedQuantity();
            } else {
                recordWorkOrderConsumptionValue = slab.getSurfaceAreaM2();
            }
            stockMovementService.recordWorkOrderConsumption(saved, null, slab, null,
                    recordWorkOrderConsumptionValue, "m2");
        } else if (stockItem != null && form.usedQuantity() != null) {
            BigDecimal remaining = stockItem.getQuantity().subtract(form.usedQuantity());
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO;
            }
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
        if (order == null) {
            return;
        }
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
                if (w == null || w.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal l;
                if (dimLengths != null && i < dimLengths.size()) {
                    l = dimLengths.get(i);
                } else {
                    l = BigDecimal.ZERO;
                }
                BigDecimal t;
                if (dimThicknesses != null && i < dimThicknesses.size()) {
                    t = dimThicknesses.get(i);
                } else {
                    t = new BigDecimal("2.0");
                }
                int pcs;
                if (dimPieceCounts != null && i < dimPieceCounts.size() && dimPieceCounts.get(i) != null) {
                    pcs = dimPieceCounts.get(i);
                } else {
                    pcs = 1;
                }
                String sOp;
                if (dimSurfaceOps != null && i < dimSurfaceOps.size() && dimSurfaceOps.get(i) != null && !dimSurfaceOps.get(i).isBlank()) {
                    sOp = dimSurfaceOps.get(i).trim();
                } else {
                    if (surfaceOperation != null) {
                        sOp = surfaceOperation;
                    } else {
                        sOp = "Cilalı";
                    }
                }
                String sUnit;
                if (dimSurfaceUnits != null && i < dimSurfaceUnits.size() && dimSurfaceUnits.get(i) != null && !dimSurfaceUnits.get(i).isBlank()) {
                    sUnit = dimSurfaceUnits.get(i).trim();
                } else {
                    sUnit = "m2";
                }
                String eOp;
                if (dimEdgeOps != null && i < dimEdgeOps.size() && dimEdgeOps.get(i) != null && !dimEdgeOps.get(i).isBlank()) {
                    eOp = dimEdgeOps.get(i).trim();
                } else {
                    if (edgeOperation != null) {
                        eOp = edgeOperation;
                    } else {
                        eOp = "Pah";
                    }
                }
                String eUnit;
                if (dimEdgeUnits != null && i < dimEdgeUnits.size() && dimEdgeUnits.get(i) != null && !dimEdgeUnits.get(i).isBlank()) {
                    eUnit = dimEdgeUnits.get(i).trim();
                } else {
                    eUnit = "m.t.";
                }
                String dNote;
                if (dimNotes != null && i < dimNotes.size() && dimNotes.get(i) != null) {
                    dNote = dimNotes.get(i).trim();
                } else {
                    dNote = "";
                }

                BigDecimal rowM2 = w.multiply(l).multiply(BigDecimal.valueOf(pcs)).divide(BigDecimal.valueOf(CM2_PER_M2), 2, RoundingMode.HALF_UP);
                BigDecimal rowMt = l.multiply(BigDecimal.valueOf(pcs)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                sumM2 = sumM2.add(rowM2);
                sumMt = sumMt.add(rowMt);
                totalPieces += pcs;

                if (effectiveWidth == null) {
                    effectiveWidth = w;
                }
                if (effectiveLength == null) {
                    effectiveLength = l;
                }
                if (effectiveThickness == null) {
                    effectiveThickness = t;
                }

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

                Object value;
                if (dNote.isEmpty()) {
                    value = "";
                } else {
                    value = " {" + dNote + "}";
                }
                breakdownBuilder.append(String.format("%d. %s x %s x %s cm — %d Adet (%s m² | %s m.t.) [Yüzey: %s (%s), Kenar: %s (%s)]%s\n",
                        i + 1, w, l, t, pcs, rowM2, rowMt, sOp, sUnit, eOp, eUnit, value));
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

        String topSurface;
        if (surfaceOperation != null && !surfaceOperation.isBlank()) {
            topSurface = surfaceOperation;
        } else {
            if (!items.isEmpty() && items.get(0).getSurfaceOperation() != null) {
                topSurface = items.get(0).getSurfaceOperation();
            } else {
                topSurface = "Cilalı";
            }
        }
        String topEdge;
        if (edgeOperation != null && !edgeOperation.isBlank()) {
            topEdge = edgeOperation;
        } else {
            if (!items.isEmpty() && items.get(0).getEdgeOperation() != null) {
                topEdge = items.get(0).getEdgeOperation();
            } else {
                topEdge = "Pah";
            }
        }

        String finalNotes;
        if (notes != null) {
            finalNotes = notes.trim();
        } else {
            finalNotes = "";
        }
        if (breakdownBuilder.length() > 0) {
            String valueValue;
            if (finalNotes.isEmpty()) {
                valueValue = "";
            } else {
                valueValue = "\n\nEk Notlar: " + finalNotes;
            }
            finalNotes = breakdownBuilder.toString() + valueValue;
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
    public TabulatorResponse<Map<String, Object>> orderRows(String search, BusinessUnit department, OperationWorkOrderStatus status, Long customerId, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        Page<OperationWorkOrder> ordersPage = searchOrders(department, status, customerId, null, null, search, pageIndex, size);

        List<Map<String, Object>> data = ordersPage.getContent().stream().map(ord -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ord.getId());
            map.put("orderNo", ord.getOrderNo());
            Object value;
            if (ord.getOrderDate() != null) {
                value = ord.getOrderDate().toString();
            } else {
                value = "";
            }
            map.put("orderDate", value);
            Object valueValue;
            if (ord.getDueDate() != null) {
                valueValue = ord.getDueDate().toString();
            } else {
                valueValue = "";
            }
            map.put("targetDeliveryDate", valueValue);
            Object valueValue2;
            if (ord.getDepartment() != null) {
                valueValue2 = ord.getDepartment().name();
            } else {
                valueValue2 = "";
            }
            map.put("department", valueValue2);
            Object displayName;
            if (ord.getDepartment() != null) {
                displayName = ord.getDepartment().getDisplayName();
            } else {
                displayName = "";
            }
            map.put("departmentLabel", displayName);
            map.put("customerName", ord.getCustomerDisplayName());
            map.put("stoneType", Objects.requireNonNullElse(ord.getStoneType(), "—"));
            map.put("quantity", Objects.requireNonNullElse(ord.getQuantity(), BigDecimal.ZERO));
            map.put("unit", Objects.requireNonNullElse(ord.getUnit(), "m²"));
            map.put("responsiblePerson", Objects.requireNonNullElse(ord.getResponsiblePerson(), "—"));
            Object valueValue3;
            if (ord.getStatus() != null) {
                valueValue3 = ord.getStatus().name();
            } else {
                valueValue3 = "";
            }
            map.put("status", valueValue3);
            Object displayNameValue;
            if (ord.getStatus() != null) {
                displayNameValue = ord.getStatus().getDisplayName();
            } else {
                displayNameValue = "";
            }
            map.put("statusLabel", displayNameValue);
            return map;
        }).toList();

        return TabulatorResponse.of(data, ordersPage.getTotalPages(), ordersPage.getTotalElements());
    }

    /**
     * Loads a work order with items, history and sources and copies it into a view.
     *
     * @param id work order id
     * @return the work order view
     */
    @Transactional(readOnly = true)
    public WorkOrderView getOrderView(Long id) {
        OperationWorkOrder order = getOrderById(id);
        WorkOrderView view = workOrderMapper.toDto(order);
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
    public Page<WorkOrderView> searchOrderRows(BusinessUnit dept, OperationWorkOrderStatus status,
                                                Long customerId, LocalDate startDate, LocalDate endDate,
                                                String search, int page, int size) {
        return searchOrders(dept, status, customerId, startDate, endDate, search, page, size).map(workOrderMapper::toRow);
    }
}
