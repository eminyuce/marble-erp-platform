package com.ozerler.marble.service;

import com.ozerler.marble.domain.OperationYield;
import com.ozerler.marble.domain.WorkshopOrderCost;
import com.ozerler.marble.dto.WorkshopReceiptDto;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.PurchaseOrderItem;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.WorkshopMaterialReceipt;
import com.ozerler.marble.model.WorkshopOperation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ChamferStatus;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.ProductForm;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.WorkshopProcessType;
import com.ozerler.marble.model.enums.WorkshopReceiptSource;
import com.ozerler.marble.model.enums.WorkshopWorkPurpose;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.PurchaseOrderItemRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.repository.SupplierRepository;
import com.ozerler.marble.repository.WorkshopMaterialReceiptRepository;
import com.ozerler.marble.repository.WorkshopOperationRepository;
import com.ozerler.marble.util.MessageUtils;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorkshopOperationService {

    private final WorkshopMaterialReceiptRepository receiptRepository;
    private final WorkshopOperationRepository operationRepository;
    private final CutOrderRepository cutOrderRepository;
    private final MachineRepository machineRepository;
    private final MaterialLotRepository materialLotRepository;
    private final StockLocationRepository stockLocationRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final CutItemRepository cutItemRepository;
    private final CostCenterRepository costCenterRepository;
    private final ExpenseService expenseService;

    @Transactional
    public WorkshopMaterialReceipt receiveMaterial(WorkshopReceiptSource source, Long supplierId,
                                                   Long purchaseOrderItemId, String stoneType,
                                                   BigDecimal quantity, BigDecimal areaM2, BigDecimal purchaseCost,
                                                   LocalDate receivedAt, String notes) {
        Objects.requireNonNull(source, MessageUtils.getMessage("error.workshop.receipt.source.required"));
        if (source == WorkshopReceiptSource.EXTERNAL_FACTORY && supplierId == null) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.workshop.receipt.supplier.required"));
        }
        Supplier supplier = supplierId != null ? supplierRepository.findById(supplierId).orElse(null) : null;
        PurchaseOrderItem purchaseItem = purchaseOrderItemId != null
                ? purchaseOrderItemRepository.findById(purchaseOrderItemId).orElse(null)
                : null;
        MaterialLot lot = materialLotRepository.save(MaterialLot.builder()
                .lotCode(UniqueCodes.yearly("WML", materialLotRepository::existsByLotCode))
                .productForm(ProductForm.SLAB)
                .stoneType(stoneType)
                .quantity(quantity != null ? quantity.intValue() : 1)
                .totalAreaM2(areaM2 != null ? areaM2 : BigDecimal.ZERO)
                .chamferStatus(ChamferStatus.NOT_APPLICABLE)
                .stockLocation(stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.WORKSHOP_STOCK).orElse(null))
                .status(MaterialLotStatus.AVAILABLE)
                .unitCost(areaM2 != null && areaM2.compareTo(BigDecimal.ZERO) > 0 && purchaseCost != null
                        ? purchaseCost.divide(areaM2, 2, java.math.RoundingMode.HALF_UP)
                        : (purchaseCost != null ? purchaseCost : BigDecimal.ZERO))
                .totalCost(purchaseCost != null ? purchaseCost : BigDecimal.ZERO)
                .build());
        WorkshopMaterialReceipt receipt = receiptRepository.save(WorkshopMaterialReceipt.builder()
                .receiptNo(UniqueCodes.yearly("WMR", receiptRepository::existsByReceiptNo))
                .source(source)
                .supplier(supplier)
                .purchaseOrderItem(purchaseItem)
                .materialLot(lot)
                .quantity(quantity != null ? quantity : BigDecimal.ONE)
                .areaM2(areaM2 != null ? areaM2 : BigDecimal.ZERO)
                .purchaseCost(purchaseCost != null ? purchaseCost : BigDecimal.ZERO)
                .receivedAt(receivedAt != null ? receivedAt : LocalDate.now())
                .notes(notes)
                .build());
        if (purchaseCost != null && purchaseCost.compareTo(BigDecimal.ZERO) > 0) {
            costCenterRepository.findByCode("CC-004").ifPresent(center -> expenseService.recordExpense(
                    new ExpenseService.ExpenseDraft(center.getId(), ExpenseType.MATERIAL, null, BusinessUnit.WORKSHOP,
                            purchaseCost, "TRY", receipt.getReceiptNo(), receipt.getReceivedAt(), LocalDate.now(),
                            YearMonth.from(receipt.getReceivedAt()).toString(), YearMonth.now().toString(),
                            null, null, null, null, null, null, null, source.name(), "Atölye malzeme kabulü")));
        }
        return receipt;
    }

    @Transactional
    public WorkshopOperation recordOperation(Long cutOrderId, WorkshopProcessType processType, Long machineId,
                                             String operatorName, BigDecimal laborHours,
                                             BigDecimal inputAreaM2, BigDecimal outputAreaM2, BigDecimal wasteAreaM2,
                                             BigDecimal extraExpense, String notes) {
        CutOrder order = cutOrderRepository.findById(cutOrderId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.cut_order.not_found", cutOrderId)));
        OperationYield.validateSameUnitBalance(inputAreaM2, outputAreaM2, wasteAreaM2);
        if (processType.requiresMachine() && machineId == null) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.workshop.machine.required"));
        }
        if (processType.requiresLaborHours() && (laborHours == null || laborHours.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.workshop.labor.required"));
        }
        Machine machine = machineId != null ? machineRepository.findById(machineId).orElse(null) : null;
        WorkshopOperation operation = operationRepository.save(WorkshopOperation.builder()
                .cutOrder(order)
                .processType(processType)
                .machine(machine)
                .operatorName(operatorName)
                .laborHours(laborHours)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .inputAreaM2(inputAreaM2)
                .outputAreaM2(outputAreaM2)
                .wasteAreaM2(wasteAreaM2)
                .extraExpense(extraExpense != null ? extraExpense : BigDecimal.ZERO)
                .status(OperationStatus.COMPLETED)
                .notes(notes)
                .build());
        if (order.getPurpose() == null) {
            order.setPurpose(WorkshopWorkPurpose.AFTER_PROCESSING_SALE);
        }
        if (machine != null) {
            order.setMachine(machine);
            order.setMachineName(machine.getName());
        }
        if (extraExpense != null && extraExpense.compareTo(BigDecimal.ZERO) > 0) {
            costCenterRepository.findByCode("CC-004").ifPresent(center -> expenseService.recordExpense(
                    new ExpenseService.ExpenseDraft(center.getId(), ExpenseType.CONSUMABLES, null, BusinessUnit.WORKSHOP,
                            extraExpense, "TRY", order.getCutOrderNo(), LocalDate.now(), LocalDate.now(),
                            YearMonth.now().toString(), YearMonth.now().toString(),
                            null, null, null, order.getProject(), machine, null, operation, processType.name(),
                            processType.getLabel())));
        }
        return operation;
    }

    @Transactional(readOnly = true)
    public BigDecimal orderCost(Long cutOrderId) {
        cutOrderRepository.findById(cutOrderId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.cut_order.not_found", cutOrderId)));
        BigDecimal material = cutItemRepository.findByCutOrderId(cutOrderId).stream()
                .map(item -> item.getUnitCost() != null && item.getAreaM2() != null
                        ? item.getUnitCost().multiply(item.getAreaM2()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        BigDecimal extra = operationRepository.findByCutOrderIdOrderByIdAsc(cutOrderId).stream()
                .map(op -> op.getExtraExpense() != null ? op.getExtraExpense() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        return WorkshopOrderCost.total(material, extra, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<WorkshopReceiptDto> listReceipts() {
        return receiptRepository.findAllWithLotAndSupplier().stream()
                .map(WorkshopReceiptDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Supplier> suppliers() {
        return supplierRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Machine> workshopMachines() {
        return machineRepository.findByBusinessUnitAndActiveTrueOrderByNameAsc(BusinessUnit.WORKSHOP);
    }

    @Transactional(readOnly = true)
    public List<WorkshopOperation> operationsFor(Long cutOrderId) {
        return operationRepository.findByCutOrderIdOrderByIdAsc(cutOrderId);
    }

    @Transactional
    public CutOrder assignPurposeAndMachine(Long cutOrderId, WorkshopWorkPurpose purpose, Long machineId) {
        CutOrder order = cutOrderRepository.findById(cutOrderId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.cut_order.not_found", cutOrderId)));
        if (purpose != null) {
            order.setPurpose(purpose);
        }
        if (machineId != null) {
            Machine machine = machineRepository.findById(machineId)
                    .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.machine.not_found", machineId)));
            order.setMachine(machine);
            order.setMachineName(machine.getName());
        }
        return cutOrderRepository.save(order);
    }
}
