package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.domain.OperationYield;
import com.ozerler.marble.domain.WorkshopOrderCost;
import com.ozerler.marble.dto.WorkshopReceiptDto;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.PurchaseOrderItem;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.WorkshopMaterialReceipt;
import com.ozerler.marble.model.WorkshopOperation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ChamferStatus;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.ProductForm;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.model.enums.WorkshopProcessType;
import com.ozerler.marble.model.enums.WorkshopReceiptSource;
import com.ozerler.marble.model.enums.WorkshopWorkPurpose;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.MachineRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.PurchaseOrderItemRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.repository.SupplierRepository;
import com.ozerler.marble.repository.WorkshopMaterialReceiptRepository;
import com.ozerler.marble.repository.WorkshopOperationRepository;
import com.ozerler.marble.util.MessageUtils;
import com.ozerler.marble.util.UniqueCodes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkshopOperationService {

    private final WorkshopMaterialReceiptRepository receiptRepository;
    private final com.ozerler.marble.mapper.SupplierMapper supplierMapper;
    private final com.ozerler.marble.mapper.MachineMapper machineMapper;
    private final com.ozerler.marble.mapper.WorkshopOperationMapper workshopOperationMapper;
    private final com.ozerler.marble.mapper.StockItemMapper stockItemMapper;
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
    private final StockItemRepository stockItemRepository;
    private final SlabRepository slabRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final StockMovementService stockMovementService;
    private final CostTransactionRepository costTransactionRepository;

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
                        ? purchaseCost.divide(areaM2, 2, RoundingMode.HALF_UP)
                        : purchaseCost != null ? purchaseCost : BigDecimal.ZERO)
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
        return BoundedReads.all(supplierRepository);
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

    @Transactional
    public List<StockItem> produceWorkshopSizedItemsBatch(
            String sourceType, // "SLAB", "SIZED", "NONE"
            Long slabId,
            Long sourceStockItemId,
            BigDecimal consumedQuantity,
            BigDecimal wasteQuantity,
            Long customerId,
            Long projectId,
            String defaultDescription,
            String qualityGradeStr,
            String surfaceFinishStr,
            String edgeFinish,
            String notes,
            List<String> descriptions,
            List<BigDecimal> thicknesses,
            List<BigDecimal> widths,
            List<BigDecimal> lengths,
            List<Integer> pieceCounts,
            List<BigDecimal> quantities,
            List<String> rowSurfaceFinishes,
            List<String> rowEdgeFinishes
    ) {
        List<StockItem> created = new ArrayList<>();
        if (widths == null || widths.isEmpty()) {
            return created;
        }

        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        Project project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;

        Slab sourceSlab = null;
        StockItem sourceStockItem = null;
        Block sourceBlock = null;
        String stoneType = "Mermer";

        if ("SLAB".equalsIgnoreCase(sourceType) && slabId != null) {
            sourceSlab = slabRepository.findById(slabId).orElse(null);
            if (sourceSlab != null) {
                sourceBlock = sourceSlab.getBlock();
                if (sourceSlab.getStoneType() != null && !sourceSlab.getStoneType().isBlank()) {
                    stoneType = sourceSlab.getStoneType();
                } else if (sourceBlock != null && sourceBlock.getStoneType() != null) {
                    stoneType = sourceBlock.getStoneType();
                }
            }
        } else if ("SIZED".equalsIgnoreCase(sourceType) && sourceStockItemId != null) {
            sourceStockItem = stockItemRepository.findById(sourceStockItemId).orElse(null);
            if (sourceStockItem != null) {
                sourceBlock = sourceStockItem.getSourceBlock();
                if (sourceStockItem.getStoneType() != null && !sourceStockItem.getStoneType().isBlank()) {
                    stoneType = sourceStockItem.getStoneType();
                } else if (sourceBlock != null && sourceBlock.getStoneType() != null) {
                    stoneType = sourceBlock.getStoneType();
                }
            }
        }

        StockLocation workshopLoc = stockLocationRepository
                .findByLocationTypeAndActiveTrue(StockLocationType.WORKSHOP_STOCK)
                .orElseGet(() -> stockLocationRepository.save(StockLocation.builder()
                        .code("ATLY-MAML-01")
                        .name("Atölye Mamul / Ebatlı Stok Sahası")
                        .businessUnit(BusinessUnit.WORKSHOP)
                        .locationType(StockLocationType.WORKSHOP_STOCK)
                        .active(true)
                        .build()));

        QualityGrade grade = QualityGrade.A;
        if (qualityGradeStr != null) {
            try { grade = QualityGrade.valueOf(qualityGradeStr); } catch (Exception ignored) {}
        }
        SurfaceFinish surface = SurfaceFinish.POLISHED;
        if (surfaceFinishStr != null) {
            try { surface = SurfaceFinish.valueOf(surfaceFinishStr); } catch (Exception ignored) {}
        }
        QualityGrade resolvedGrade = grade;
        SurfaceFinish resolvedSurface = surface;
        Slab resolvedSlab = sourceSlab;
        StockItem resolvedSourceItem = sourceStockItem;
        Block resolvedBlock = sourceBlock;
        String resolvedStoneType = stoneType;

        BigDecimal[] producedArea = { BigDecimal.ZERO };
        RowDimensions.eachPositiveSizedRow(widths, lengths, thicknesses, pieceCounts, quantities, new BigDecimal("2.0"),
                (index, width, length, thickness, count, quantity) -> {
            producedArea[0] = producedArea[0].add(quantity);

            String desc = (descriptions != null && index < descriptions.size() && descriptions.get(index) != null && !descriptions.get(index).isBlank())
                    ? descriptions.get(index).trim()
                    : defaultDescription != null && !defaultDescription.isBlank() ? defaultDescription : ("Atölye Ebatlı " + thickness + "x" + width + "x" + length);

            String rowSurfaceStr = (rowSurfaceFinishes != null && index < rowSurfaceFinishes.size() && rowSurfaceFinishes.get(index) != null && !rowSurfaceFinishes.get(index).isBlank())
                    ? rowSurfaceFinishes.get(index).trim() : surfaceFinishStr;
            SurfaceFinish rowSurface = resolvedSurface;
            if (rowSurfaceStr != null) {
                try { rowSurface = SurfaceFinish.valueOf(rowSurfaceStr); } catch (Exception ignored) {}
            }

            String rowEdge = (rowEdgeFinishes != null && index < rowEdgeFinishes.size() && rowEdgeFinishes.get(index) != null && !rowEdgeFinishes.get(index).isBlank())
                    ? rowEdgeFinishes.get(index).trim() : edgeFinish != null ? edgeFinish : "DUZ";

            String itemCode = UniqueCodes.yearly("ATL-EBT", stockItemRepository::existsByItemCode);

            String combinedNotes = notes;
            if (resolvedSlab != null) {
                String slabInfo = "[Kaynak Plaka: " + resolvedSlab.getSlabCode() + "]";
                combinedNotes = (combinedNotes != null && !combinedNotes.isBlank()) ? combinedNotes + " " + slabInfo : slabInfo;
            } else if (resolvedSourceItem != null) {
                String srcInfo = "[Yeniden Ebatlama - Kaynak Ürün: " + resolvedSourceItem.getItemCode() + "]";
                combinedNotes = (combinedNotes != null && !combinedNotes.isBlank()) ? combinedNotes + " " + srcInfo : srcInfo;
            }

            StockItem item = StockItem.builder()
                    .itemCode(itemCode)
                    .productType(StockProductType.SIZED)
                    .sourceBlock(resolvedBlock)
                    .stoneType(resolvedStoneType)
                    .description(desc)
                    .thicknessCm(thickness)
                    .widthCm(width)
                    .lengthCm(length)
                    .quantity(quantity.setScale(2, RoundingMode.HALF_UP))
                    .unit("m2")
                    .pieceCount(count)
                    .actualProducedQuantity(quantity.setScale(2, RoundingMode.HALF_UP))
                    .customer(customer)
                    .project(project)
                    .stockLocation(workshopLoc)
                    .qualityGrade(resolvedGrade)
                    .surfaceFinish(rowSurface)
                    .edgeFinish(rowEdge)
                    .status("AVAILABLE")
                    .productionDate(LocalDate.now())
                    .notes(combinedNotes)
                    .build();

            StockItem saved = stockItemRepository.save(item);
            stockMovementService.recordWorkshopSizedProduction(saved, resolvedSlab, resolvedSourceItem, customer);
            created.add(saved);
        });
        BigDecimal totalProducedM2 = producedArea[0];

        // Kaynak hammadde düşümü
        BigDecimal effectiveConsumed = (consumedQuantity != null && consumedQuantity.compareTo(BigDecimal.ZERO) > 0)
                ? consumedQuantity
                : totalProducedM2.add(wasteQuantity != null ? wasteQuantity : BigDecimal.ZERO);

        if (sourceSlab != null && effectiveConsumed.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentArea = sourceSlab.getSurfaceAreaM2() != null ? sourceSlab.getSurfaceAreaM2() : BigDecimal.ZERO;
            if (effectiveConsumed.compareTo(currentArea) >= 0) {
                sourceSlab.setStatus(SlabStatus.IN_CUTTING);
                sourceSlab.setSurfaceAreaM2(BigDecimal.ZERO);
            } else {
                sourceSlab.setSurfaceAreaM2(currentArea.subtract(effectiveConsumed));
            }
            slabRepository.save(sourceSlab);
        } else if (sourceStockItem != null && effectiveConsumed.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentQty = sourceStockItem.getQuantity() != null ? sourceStockItem.getQuantity() : BigDecimal.ZERO;
            if (effectiveConsumed.compareTo(currentQty) >= 0) {
                sourceStockItem.setQuantity(BigDecimal.ZERO);
                sourceStockItem.setStatus("CONSUMED");
            } else {
                sourceStockItem.setQuantity(currentQty.subtract(effectiveConsumed));
            }
            stockItemRepository.save(sourceStockItem);
        }

        // Fire miktarı ve maliyeti
        if (wasteQuantity != null && wasteQuantity.compareTo(BigDecimal.ZERO) > 0) {
            StockItem refItem = !created.isEmpty() ? created.get(0) : sourceStockItem;
            stockMovementService.recordWorkshopWaste(refItem, project, wasteQuantity, "m2", "Atölyede kesim firesi zayi olarak düşüldü");

            if (project != null) {
                CostCenter cc = costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.SITE)
                        .or(() -> costCenterRepository.findByCode("CC-005")).orElse(null);
                if (cc != null) {
                    BigDecimal unitPrice = (sourceStockItem != null && sourceStockItem.getUnitPrice() != null)
                            ? sourceStockItem.getUnitPrice() : new BigDecimal("400.00");
                    BigDecimal wasteCost = wasteQuantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    CostTransaction wasteTx = CostTransaction.builder()
                            .costCenter(cc)
                            .project(project)
                            .constructionSite(project)
                            .expenseType(ExpenseType.DIRECT_RAW)
                            .amount(wasteCost)
                            .businessUnit(BusinessUnit.SITE)
                            .documentNo("ATL-FIRE-" + LocalDate.now())
                            .invoiceDate(LocalDate.now())
                            .entryDate(LocalDate.now())
                            .expensePeriod(YearMonth.now().toString())
                            .description("Atölye Kesim/Ebatlama Fire Maliyeti (" + wasteQuantity + " m²): " + (refItem != null ? refItem.getItemCode() : "Hammadde"))
                            .build();
                    costTransactionRepository.save(wasteTx);
                    BigDecimal totalCosts = costTransactionRepository.sumByProjectId(project.getId());
                    project.setActualCost(totalCosts != null ? totalCosts : BigDecimal.ZERO);
                    projectRepository.save(project);
                }
            }
        }

        return created;
    }

    /**
     * Same as {@code produceWorkshopSizedItemsBatch}, copied into a view inside the transaction.
     *
     * @param sourceType see {@code produceWorkshopSizedItemsBatch}
     * @param "SLAB" see {@code produceWorkshopSizedItemsBatch}
     * @param "SIZED" see {@code produceWorkshopSizedItemsBatch}
     * @param slabId see {@code produceWorkshopSizedItemsBatch}
     * @param sourceStockItemId see {@code produceWorkshopSizedItemsBatch}
     * @param consumedQuantity see {@code produceWorkshopSizedItemsBatch}
     * @param wasteQuantity see {@code produceWorkshopSizedItemsBatch}
     * @param customerId see {@code produceWorkshopSizedItemsBatch}
     * @param projectId see {@code produceWorkshopSizedItemsBatch}
     * @param defaultDescription see {@code produceWorkshopSizedItemsBatch}
     * @param qualityGradeStr see {@code produceWorkshopSizedItemsBatch}
     * @param surfaceFinishStr see {@code produceWorkshopSizedItemsBatch}
     * @param edgeFinish see {@code produceWorkshopSizedItemsBatch}
     * @param notes see {@code produceWorkshopSizedItemsBatch}
     * @param descriptions see {@code produceWorkshopSizedItemsBatch}
     * @param thicknesses see {@code produceWorkshopSizedItemsBatch}
     * @param widths see {@code produceWorkshopSizedItemsBatch}
     * @param lengths see {@code produceWorkshopSizedItemsBatch}
     * @param pieceCounts see {@code produceWorkshopSizedItemsBatch}
     * @param quantities see {@code produceWorkshopSizedItemsBatch}
     * @param rowSurfaceFinishes see {@code produceWorkshopSizedItemsBatch}
     * @param rowEdgeFinishes see {@code produceWorkshopSizedItemsBatch}
     * @return the view
     */
    @Transactional
    public List<com.ozerler.marble.dto.view.StockItemView> produceWorkshopSizedItemsBatchViews(
            String sourceType, // "SLAB", "SIZED", "NONE"
            Long slabId,
            Long sourceStockItemId,
            BigDecimal consumedQuantity,
            BigDecimal wasteQuantity,
            Long customerId,
            Long projectId,
            String defaultDescription,
            String qualityGradeStr,
            String surfaceFinishStr,
            String edgeFinish,
            String notes,
            List<String> descriptions,
            List<BigDecimal> thicknesses,
            List<BigDecimal> widths,
            List<BigDecimal> lengths,
            List<Integer> pieceCounts,
            List<BigDecimal> quantities,
            List<String> rowSurfaceFinishes,
            List<String> rowEdgeFinishes
    ) {
        return produceWorkshopSizedItemsBatch(sourceType, slabId, sourceStockItemId, consumedQuantity, wasteQuantity, customerId, projectId, defaultDescription, qualityGradeStr, surfaceFinishStr, edgeFinish, notes, descriptions, thicknesses, widths, lengths, pieceCounts, quantities, rowSurfaceFinishes, rowEdgeFinishes).stream().map(stockItemMapper::toDto).toList();
    }

    /**
     * Same as {@code operationsFor}, copied into a view inside the transaction.
     *
     * @param cutOrderId see {@code operationsFor}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.WorkshopOperationView> operationViewsFor(Long cutOrderId) {
        return operationsFor(cutOrderId).stream().map(workshopOperationMapper::toDto).toList();
    }

    /**
     * Same as {@code workshopMachines}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.MachineRef> workshopMachineViews() {
        return workshopMachines().stream().map(machineMapper::toDto).toList();
    }

    /**
     * Same as {@code suppliers}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.PartyView> supplierViews() {
        return suppliers().stream().map(supplierMapper::toDto).toList();
    }
}
