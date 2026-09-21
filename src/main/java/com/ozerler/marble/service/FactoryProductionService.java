package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.BlockMeasurement;
import com.ozerler.marble.domain.FactoryProcessRouting;
import com.ozerler.marble.domain.OperationYield;
import com.ozerler.marble.domain.PalletCostAccumulator;
import com.ozerler.marble.util.UniqueCodes;
import com.ozerler.marble.dto.FactoryWorkOrderSummaryDto;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.FactoryWorkOrder;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.ScrapLog;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ChamferStatus;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.FactoryWorkOrderStatus;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.ProductForm;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.QuantityUnit;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.FactoryOperationRepository;
import com.ozerler.marble.repository.FactoryWorkOrderRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FactoryProductionService {

    private final BlockRepository blockRepository;
    private final FactoryWorkOrderRepository workOrderRepository;
    private final FactoryOperationRepository operationRepository;
    private final MachineRepository machineRepository;
    private final StockLocationRepository stockLocationRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final SlabRepository slabRepository;
    private final ScrapLogRepository scrapLogRepository;
    private final MaterialLotRepository materialLotRepository;
    private final CostCenterRepository costCenterRepository;
    private final ExpenseService expenseService;
    private final QuarryBlockService quarryBlockService;

    @Transactional
    public FactoryWorkOrder acceptBlock(Long blockId, Long machineId, String responsibleName) {
        Block block = quarryBlockService.getBlockById(blockId);
        if (!block.getCanonicalStatus().isAvailableForFactoryAccept()
                && block.getCanonicalStatus() != BlockStatus.AT_FACTORY) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.factory.accept.not_dispatched"));
        }
        StockLocation factoryYard = location(StockLocationType.FACTORY_BLOCK_YARD);
        block.setStatus(BlockStatus.AT_FACTORY);
        block.setCurrentLocation(factoryYard);
        blockRepository.save(block);

        FactoryWorkOrder existing = workOrderRepository.findFirstByBlockIdOrderByIdDesc(blockId).orElse(null);
        if (existing != null && existing.getStatus() != FactoryWorkOrderStatus.CANCELLED
                && existing.getStatus() != FactoryWorkOrderStatus.COMPLETED) {
            return existing;
        }
        Machine machine = machineId != null ? machine(machineId) : null;
        FactoryWorkOrder order = FactoryWorkOrder.builder()
                .orderNo(UniqueCodes.yearly("FWO", workOrderRepository::existsByOrderNo))
                .block(block)
                .acceptedAt(LocalDate.now())
                .stockLocation(factoryYard)
                .assignedMachine(machine)
                .status(machine != null ? FactoryWorkOrderStatus.ASSIGNED : FactoryWorkOrderStatus.ACCEPTED)
                .responsibleName(responsibleName)
                .build();
        return workOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder recordCutting(CuttingRequest request) {
        Objects.requireNonNull(request.blockId(), MessageUtils.getMessage("error.block.id.required"));
        FactoryProcessType processType = request.processType();
        FactoryProcessRouting.requireCutting(processType);
        Block block = quarryBlockService.getBlockById(request.blockId());
        if (!block.getCanonicalStatus().isAvailableForCutting()
                && block.getCanonicalStatus() != BlockStatus.DISPATCHED) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.factory.cut.block_not_ready"));
        }
        FactoryWorkOrder workOrder = workOrderRepository.findFirstByBlockIdOrderByIdDesc(block.getId())
                .orElseGet(() -> acceptBlock(block.getId(), request.machineId(), request.operatorName()));

        BigDecimal inputTons = request.inputQuantity() != null
                ? request.inputQuantity()
                : BlockMeasurement.productionTonnage(block.getActualWeightKg(), block.getTheoreticalWeightKg());
        BigDecimal outputM2 = request.outputQuantity() != null ? request.outputQuantity() : derivedOutputArea(request);
        BigDecimal waste = request.wasteQuantity() != null ? request.wasteQuantity() : BigDecimal.ZERO;
        QuantityUnit wasteUnit = request.wasteUnit() != null ? request.wasteUnit() : QuantityUnit.KG;
        OperationYield.requireNonNegative("input", inputTons);
        OperationYield.requireNonNegative("output", outputM2);
        OperationYield.requireNonNegative("waste", waste);

        Machine machine = request.machineId() != null ? machine(request.machineId()) : null;
        FactoryOperation previous = lastCompletedOperation(workOrder.getId());
        FactoryOperation operation = FactoryOperation.builder()
                .workOrder(workOrder)
                .previousOperation(previous)
                .processType(processType)
                .machine(machine)
                .operatorName(request.operatorName())
                .startedAt(LocalDateTime.now().minusHours(request.durationHours() != null ? request.durationHours().longValue() : 8))
                .finishedAt(LocalDateTime.now())
                .inputQuantity(inputTons)
                .inputUnit(QuantityUnit.TON)
                .outputQuantity(outputM2)
                .outputUnit(QuantityUnit.SQUARE_METER)
                .wasteQuantity(waste)
                .wasteUnit(wasteUnit)
                .status(OperationStatus.COMPLETED)
                .notes(request.notes())
                .build();
        operation = operationRepository.save(operation);

        ProductionOrder productionOrder = saveLegacyProductionOrder(block, workOrder, operation, request, machine);
        List<Slab> slabs = createSlabs(productionOrder, block, request);
        createLots(slabs, operation, block, processType == FactoryProcessType.ST_CUTTING ? ProductForm.STRIP : ProductForm.SLAB);
        recordScrap(productionOrder, block, request);
        recordCuttingExpense(request.directCuttingExpense(), operation, block);

        block.setStatus(BlockStatus.IN_PROCESS);
        blockRepository.save(block);
        workOrder.setStatus(FactoryWorkOrderStatus.WAITING_NEXT_STEP);
        workOrder.setAssignedMachine(machine);
        workOrderRepository.save(workOrder);
        return productionOrder;
    }

    @Transactional
    public FactoryOperation recordSurfaceOperation(Long workOrderId, FactoryProcessType processType,
                                                   Long machineId, String operatorName,
                                                   BigDecimal inputM2, BigDecimal outputM2, BigDecimal wasteM2,
                                                   ChamferStatus chamferStatus, String notes) {
        return recordSurfaceOperation(workOrderId, processType, machineId, operatorName,
                inputM2, outputM2, wasteM2, chamferStatus, notes, null, null, null);
    }

    @Transactional
    public FactoryOperation recordSurfaceOperation(Long workOrderId, FactoryProcessType processType,
                                                   Long machineId, String operatorName,
                                                   BigDecimal inputM2, BigDecimal outputM2, BigDecimal wasteM2,
                                                   ChamferStatus chamferStatus, String notes,
                                                   BigDecimal laborCost, BigDecimal electricityCost,
                                                   BigDecimal consumableCost) {
        FactoryProcessRouting.requireSurface(processType);
        FactoryWorkOrder workOrder = requireWorkOrder(workOrderId);
        FactoryProcessRouting.requireValidSurfaceRouting(lastCompletedCuttingType(workOrder.getId()), processType);
        OperationYield.validateSameUnitBalance(inputM2, outputM2, wasteM2);
        ChamferStatus chamfer = resolveSurfaceChamfer(processType, chamferStatus);
        BigDecimal labor = zero(laborCost);
        BigDecimal electricity = zero(electricityCost);
        BigDecimal consumable = zero(consumableCost);
        FactoryOperation operation = FactoryOperation.builder()
                .workOrder(workOrder)
                .previousOperation(lastCompletedOperation(workOrder.getId()))
                .processType(processType)
                .machine(machineId != null ? machine(machineId) : null)
                .operatorName(operatorName)
                .startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now())
                .inputQuantity(inputM2)
                .inputUnit(QuantityUnit.SQUARE_METER)
                .outputQuantity(outputM2)
                .outputUnit(QuantityUnit.SQUARE_METER)
                .wasteQuantity(wasteM2)
                .wasteUnit(QuantityUnit.SQUARE_METER)
                .chamferStatus(chamfer)
                .status(OperationStatus.COMPLETED)
                .laborCost(labor)
                .electricityCost(electricity)
                .consumableCost(consumable)
                .totalOperationCost(labor.add(electricity).add(consumable))
                .notes(notes)
                .build();
        FactoryOperation saved = operationRepository.save(operation);
        applyOperationCostToSlabs(workOrder, saved, inputM2, outputM2);
        workOrder.setStatus(FactoryWorkOrderStatus.WAITING_NEXT_STEP);
        if (processType == FactoryProcessType.BRIDGE_SAW_SIZING) {
            workOrder.setStatus(FactoryWorkOrderStatus.COMPLETED);
        }
        return saved;
    }

    @Transactional
    public FactoryOperation startAssignedOperation(Long operationId, String operatorName) {
        FactoryOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.factory.operation.not_found", operationId)));
        operation.setStatus(OperationStatus.IN_PROGRESS);
        operation.setStartedAt(LocalDateTime.now());
        if (operatorName != null && !operatorName.isBlank()) {
            operation.setOperatorName(operatorName);
        }
        operation.getWorkOrder().setStatus(FactoryWorkOrderStatus.IN_PROGRESS);
        operation.getWorkOrder().getBlock().setStatus(BlockStatus.IN_PROCESS);
        return operationRepository.save(operation);
    }

    @Transactional
    public FactoryOperation finishAssignedOperation(Long operationId, BigDecimal input, BigDecimal output,
                                                    BigDecimal waste, QuantityUnit wasteUnit, ChamferStatus chamferStatus) {
        FactoryOperation operation = operationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.factory.operation.not_found", operationId)));
        if (operation.getProcessType().usesSamePhysicalUnit()) {
            OperationYield.validateSameUnitBalance(input, output, waste);
        } else {
            OperationYield.requireNonNegative("input", input);
            OperationYield.requireNonNegative("output", output);
            OperationYield.requireNonNegative("waste", waste);
        }
        operation.setInputQuantity(input);
        operation.setOutputQuantity(output);
        operation.setWasteQuantity(waste);
        if (wasteUnit != null) {
            operation.setWasteUnit(wasteUnit);
        }
        if (operation.getProcessType() == FactoryProcessType.STRIP_POLISHING && chamferStatus != null) {
            operation.setChamferStatus(chamferStatus);
        }
        operation.setFinishedAt(LocalDateTime.now());
        operation.setStatus(OperationStatus.COMPLETED);
        operation.setTotalOperationCost(operation.operationCostTotal());
        FactoryOperation saved = operationRepository.save(operation);
        if (operation.getProcessType().usesSamePhysicalUnit()) {
            applyOperationCostToSlabs(operation.getWorkOrder(), saved, input, output);
        }
        saved.getWorkOrder().setStatus(FactoryWorkOrderStatus.WAITING_NEXT_STEP);
        return saved;
    }

    @Transactional
    public ProductionOrder updateProductionOrder(Long id, String machineName, String operatorName,
                                                 BigDecimal durationHours, BigDecimal electricityKwh,
                                                 BigDecimal bladeWearMm, String notes) {
        ProductionOrder order = productionOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.production.order.not_found", id)));
        if (machineName != null && !machineName.isBlank()) {
            order.setMachineName(machineName);
        }
        if (operatorName != null) {
            order.setOperatorName(operatorName);
        }
        if (durationHours != null) {
            order.setDurationHours(durationHours);
        }
        if (electricityKwh != null) {
            order.setElectricityKwh(electricityKwh);
        }
        if (bladeWearMm != null) {
            order.setBladeWearMm(bladeWearMm);
        }
        order.setNotes(notes);
        return productionOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<FactoryWorkOrderSummaryDto> listWorkOrderSummaries() {
        return workOrderRepository.findAllWithBlockOrderByIdDesc().stream()
                .map(FactoryWorkOrderSummaryDto::fromEntity)
                .toList();
    }

    @Transactional
    public FactoryOperation planOperation(Long workOrderId, FactoryProcessType processType,
                                          Long machineId, String operatorName) {
        Objects.requireNonNull(processType, MessageUtils.getMessage("error.factory.process.required"));
        FactoryWorkOrder workOrder = requireWorkOrder(workOrderId);
        FactoryProcessRouting.requireValidSurfaceRouting(lastCompletedCuttingType(workOrder.getId()), processType);
        FactoryOperation operation = FactoryOperation.builder()
                .workOrder(workOrder)
                .previousOperation(lastCompletedOperation(workOrder.getId()))
                .processType(processType)
                .machine(machineId != null ? machine(machineId) : null)
                .operatorName(operatorName)
                .inputUnit(processType.getDefaultInputUnit())
                .outputUnit(processType.getDefaultOutputUnit())
                .wasteUnit(processType.getDefaultOutputUnit())
                .status(OperationStatus.PLANNED)
                .build();
        workOrder.setStatus(FactoryWorkOrderStatus.ASSIGNED);
        return operationRepository.save(operation);
    }

    @Transactional(readOnly = true)
    public FactoryWorkOrder getWorkOrder(Long id) {
        return requireWorkOrder(id);
    }

    @Transactional(readOnly = true)
    public List<FactoryOperation> operationsFor(Long workOrderId) {
        return operationRepository.findByWorkOrderIdOrderByIdAsc(workOrderId);
    }

    @Transactional(readOnly = true)
    public List<FactoryOperation> tabletQueue(String operatorName) {
        List<FactoryOperation> queue = new ArrayList<>();
        if (operatorName == null || operatorName.isBlank()) {
            queue.addAll(operationRepository.findByStatusOrderByIdDesc(OperationStatus.IN_PROGRESS));
            queue.addAll(operationRepository.findByStatusOrderByIdDesc(OperationStatus.PLANNED));
            return queue;
        }
        queue.addAll(operationRepository.findByOperatorNameAndStatus(operatorName, OperationStatus.IN_PROGRESS));
        queue.addAll(operationRepository.findByOperatorNameAndStatus(operatorName, OperationStatus.PLANNED));
        return queue;
    }

    @Transactional(readOnly = true)
    public List<Machine> factoryMachines() {
        return machineRepository.findByBusinessUnitAndActiveTrueOrderByNameAsc(BusinessUnit.FACTORY);
    }

    @Transactional(readOnly = true)
    public List<FactoryProcessType> allowedSurfaceProcesses(Long workOrderId) {
        return FactoryProcessRouting.allowedSurfaceTypes(lastCompletedCuttingType(workOrderId));
    }

    private ProductionOrder saveLegacyProductionOrder(Block block, FactoryWorkOrder workOrder, FactoryOperation operation,
                                                      CuttingRequest request, Machine machine) {
        String orderNo = (request.orderNo() != null && !request.orderNo().isBlank())
                ? request.orderNo()
                : UniqueCodes.yearly("PRD", productionOrderRepository::existsByOrderNo);
        ProductionOrder order = ProductionOrder.builder()
                .orderNo(orderNo)
                .block(block)
                .machineName(machine != null ? machine.getName() : request.machineName())
                .machine(machine)
                .processType(request.processType().toLegacyProcessType())
                .startTime(operation.getStartedAt())
                .endTime(operation.getFinishedAt())
                .durationHours(request.durationHours() != null ? request.durationHours() : Constants.DEFAULT_DURATION_HOURS)
                .electricityKwh(request.electricityKwh() != null ? request.electricityKwh() : BigDecimal.ZERO)
                .bladeWearMm(request.bladeWearMm() != null ? request.bladeWearMm() : BigDecimal.ZERO)
                .operatorName(request.operatorName())
                .status(Constants.STATUS_COMPLETED)
                .notes(request.notes())
                .factoryWorkOrder(workOrder)
                .factoryOperation(operation)
                .build();
        return productionOrderRepository.save(order);
    }

    private List<Slab> createSlabs(ProductionOrder order, Block block, CuttingRequest request) {
        BigDecimal width = request.slabWidthCm() != null ? request.slabWidthCm() : BigDecimal.valueOf(block.getWidthCm());
        BigDecimal length = request.slabLengthCm() != null ? request.slabLengthCm() : BigDecimal.valueOf(block.getLengthCm());
        BigDecimal thickness = request.thicknessCm() != null ? request.thicknessCm() : Constants.DEFAULT_THICKNESS_CM;
        BigDecimal singleArea = width.multiply(length)
                .divide(Constants.SQUARE_CENTIMETERS_PER_SQUARE_METER, Constants.AREA_SCALE, RoundingMode.HALF_UP);

        int extra = Math.max(request.extraCount(), 0);
        int countA = Math.max(request.gradeACount(), 0);
        int countB = Math.max(request.gradeBCount(), 0);
        int countC = Math.max(request.gradeCCount(), 0);
        BigDecimal eqArea = singleArea.multiply(BigDecimal.valueOf(extra)).multiply(QualityGrade.EXTRA.getMultiplier())
                .add(singleArea.multiply(BigDecimal.valueOf(countA)).multiply(QualityGrade.A.getMultiplier()))
                .add(singleArea.multiply(BigDecimal.valueOf(countB)).multiply(QualityGrade.B.getMultiplier()))
                .add(singleArea.multiply(BigDecimal.valueOf(countC)).multiply(QualityGrade.C.getMultiplier()));
        BigDecimal direct = request.directCuttingExpense() != null ? request.directCuttingExpense() : BigDecimal.ZERO;
        BigDecimal base = eqArea.compareTo(BigDecimal.ZERO) > 0
                ? block.getTotalCost().add(direct).divide(eqArea, Constants.COST_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Slab> slabs = new ArrayList<>();
        long sequence = System.nanoTime() % 10000;
        sequence = addGrade(slabs, order, block, extra, QualityGrade.EXTRA, thickness, width, length, singleArea, base, sequence);
        sequence = addGrade(slabs, order, block, countA, QualityGrade.A, thickness, width, length, singleArea, base, sequence);
        sequence = addGrade(slabs, order, block, countB, QualityGrade.B, thickness, width, length, singleArea, base, sequence);
        addGrade(slabs, order, block, countC, QualityGrade.C, thickness, width, length, singleArea, base, sequence);
        if (!slabs.isEmpty()) {
            slabRepository.saveAll(slabs);
        }
        return slabs;
    }

    private long addGrade(List<Slab> slabs, ProductionOrder order, Block block, int count, QualityGrade grade,
                          BigDecimal thickness, BigDecimal width, BigDecimal length, BigDecimal area,
                          BigDecimal base, long sequence) {
        BigDecimal cost = base.multiply(grade.getMultiplier()).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
        for (int i = 0; i < count; i++) {
            slabs.add(Slab.builder()
                    .slabCode(nextSlabCode(sequence + i))
                    .productionOrder(order)
                    .block(block)
                    .thicknessCm(thickness)
                    .widthCm(width)
                    .lengthCm(length)
                    .surfaceAreaM2(area)
                    .surfaceFinish(SurfaceFinish.RAW)
                    .qualityGrade(grade)
                    .glossLevel(0)
                    .costPerM2(cost)
                    .status(SlabStatus.AVAILABLE)
                    .build());
        }
        return sequence + count;
    }

    private void createLots(List<Slab> slabs, FactoryOperation operation, Block block, ProductForm form) {
        StockLocation slabYard = location(StockLocationType.SLAB_STOCK_YARD);
        for (Slab slab : slabs) {
            materialLotRepository.save(MaterialLot.builder()
                    .lotCode(slab.getSlabCode())
                    .productForm(form)
                    .sourceFactoryOperation(operation)
                    .sourceBlock(block)
                    .slab(slab)
                    .stoneType(block.getStoneType())
                    .qualityGrade(slab.getQualityGrade())
                    .surfaceFinish(slab.getSurfaceFinish())
                    .thicknessCm(slab.getThicknessCm())
                    .widthCm(slab.getWidthCm())
                    .lengthCm(slab.getLengthCm())
                    .quantity(1)
                    .totalAreaM2(slab.getSurfaceAreaM2())
                    .stockLocation(slabYard)
                    .status(MaterialLotStatus.AVAILABLE)
                    .unitCost(slab.getCostPerM2())
                    .totalCost(slab.getCostPerM2().multiply(slab.getSurfaceAreaM2()).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP))
                    .build());
        }
    }

    private void recordScrap(ProductionOrder order, Block block, CuttingRequest request) {
        if (request.scrapReason() == null || request.scrapWeightKg() == null
                || request.scrapWeightKg().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal direct = request.directCuttingExpense() != null ? request.directCuttingExpense() : BigDecimal.ZERO;
        scrapLogRepository.save(ScrapLog.builder()
                .scrapCode(UniqueCodes.yearly("SCRAP", 10_000, scrapLogRepository::existsByScrapCode))
                .productionOrder(order)
                .block(block)
                .reasonCode(request.scrapReason())
                .scrapWeightKg(request.scrapWeightKg())
                .scrapAreaM2(BigDecimal.ZERO)
                .costImpact(direct.multiply(Constants.SCRAP_COST_IMPACT_RATE).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP))
                .description(request.scrapNotes() != null ? request.scrapNotes() : request.scrapReason().getDescription())
                .loggedBy(request.operatorName())
                .build());
    }

    private void recordCuttingExpense(BigDecimal amount, FactoryOperation operation, Block block) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        costCenterRepository.findByCode("CC-002").ifPresent(center -> expenseService.recordExpense(
                new ExpenseService.ExpenseDraft(center.getId(), ExpenseType.ELECTRICITY, null, BusinessUnit.FACTORY,
                        amount, "TRY", null, LocalDate.now(), LocalDate.now(),
                        YearMonth.now().toString(), YearMonth.now().toString(),
                        block, null, null, null, operation.getMachine(), operation, null, operation.getProcessType().name(),
                        operation.getProcessType().getLabel())));
    }

    private BigDecimal derivedOutputArea(CuttingRequest request) {
        int total = Math.max(request.extraCount(), 0) + Math.max(request.gradeACount(), 0)
                + Math.max(request.gradeBCount(), 0) + Math.max(request.gradeCCount(), 0);
        if (total == 0 || request.slabWidthCm() == null || request.slabLengthCm() == null) {
            return BigDecimal.ZERO;
        }
        return request.slabWidthCm().multiply(request.slabLengthCm())
                .divide(Constants.SQUARE_CENTIMETERS_PER_SQUARE_METER, Constants.AREA_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(total));
    }

    private FactoryOperation lastCompletedOperation(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        return operationRepository.findTopByWorkOrderIdAndStatusOrderByIdDesc(workOrderId, OperationStatus.COMPLETED)
                .orElse(null);
    }

    private FactoryProcessType lastCompletedCuttingType(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        return operationRepository.findTopByWorkOrderIdAndProcessTypeInAndStatusOrderByIdDesc(
                        workOrderId,
                        FactoryProcessRouting.CUTTING_TYPES,
                        OperationStatus.COMPLETED)
                .map(op -> op.getProcessType())
                .orElse(null);
    }

    private FactoryWorkOrder requireWorkOrder(Long workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        MessageUtils.getMessage("error.factory.work_order.not_found", workOrderId)));
    }

    private static ChamferStatus resolveSurfaceChamfer(FactoryProcessType processType, ChamferStatus chamferStatus) {
        if (processType != FactoryProcessType.STRIP_POLISHING) {
            return ChamferStatus.NOT_APPLICABLE;
        }
        return chamferStatus != null ? chamferStatus : ChamferStatus.UNCHAMFERED;
    }

    private void applyOperationCostToSlabs(FactoryWorkOrder workOrder, FactoryOperation operation,
                                           BigDecimal inputM2, BigDecimal outputM2) {
        if (workOrder == null || workOrder.getBlock() == null || workOrder.getBlock().getId() == null) {
            return;
        }
        List<Slab> slabs = slabRepository.findByBlockId(workOrder.getBlock().getId());
        if (slabs.isEmpty()) {
            return;
        }
        BigDecimal added = operation != null ? zero(operation.getTotalOperationCost()) : BigDecimal.ZERO;
        BigDecimal share = added.divide(BigDecimal.valueOf(slabs.size()), Constants.COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal areaRatio = remainingAreaRatio(inputM2, outputM2);
        for (Slab slab : slabs) {
            BigDecimal currentArea = zero(slab.getSurfaceAreaM2());
            BigDecimal remaining = currentArea.multiply(areaRatio).setScale(Constants.AREA_SCALE, RoundingMode.HALF_UP);
            PalletCostAccumulator.StepResult step = PalletCostAccumulator.applyOperation(
                    slab.getCostPerM2(), currentArea, remaining, share);
            slab.setCostPerM2(step.newCostPerM2());
            if (remaining.compareTo(BigDecimal.ZERO) > 0 && remaining.compareTo(currentArea) != 0) {
                slab.setSurfaceAreaM2(remaining);
            }
            syncMaterialLotCost(slab);
        }
        slabRepository.saveAll(slabs);
    }

    private static BigDecimal remainingAreaRatio(BigDecimal inputM2, BigDecimal outputM2) {
        BigDecimal input = inputM2 != null && inputM2.compareTo(BigDecimal.ZERO) > 0 ? inputM2 : BigDecimal.ZERO;
        if (input.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal output = outputM2 != null ? outputM2 : input;
        return output.divide(input, Constants.AREA_SCALE, RoundingMode.HALF_UP);
    }

    private void syncMaterialLotCost(Slab slab) {
        materialLotRepository.findBySlabId(slab.getId()).ifPresent(lot -> {
            lot.setUnitCost(slab.getCostPerM2());
            BigDecimal area = zero(slab.getSurfaceAreaM2());
            lot.setTotalAreaM2(area);
            lot.setTotalCost(slab.getCostPerM2().multiply(area).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP));
        });
    }

    private String nextSlabCode(long sequenceHint) {
        return UniqueCodes.allocate(
                () -> String.format("SLB-%d-%d", Year.now().getValue(), Math.abs(sequenceHint + System.nanoTime() % 1000)),
                slabRepository::existsBySlabCode);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private Machine machine(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.machine.not_found", id)));
    }

    private StockLocation location(StockLocationType type) {
        return StockLocations.require(stockLocationRepository, type);
    }

    public record CuttingRequest(
            Long blockId,
            String orderNo,
            Long machineId,
            String machineName,
            FactoryProcessType processType,
            BigDecimal durationHours,
            BigDecimal electricityKwh,
            BigDecimal bladeWearMm,
            BigDecimal directCuttingExpense,
            String operatorName,
            String notes,
            int extraCount,
            int gradeACount,
            int gradeBCount,
            int gradeCCount,
            BigDecimal slabWidthCm,
            BigDecimal slabLengthCm,
            BigDecimal thicknessCm,
            BigDecimal inputQuantity,
            BigDecimal outputQuantity,
            BigDecimal wasteQuantity,
            QuantityUnit wasteUnit,
            ScrapReasonCode scrapReason,
            BigDecimal scrapWeightKg,
            String scrapNotes
    ) {
    }
}
