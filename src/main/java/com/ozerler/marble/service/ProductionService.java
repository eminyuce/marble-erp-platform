package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.*;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.ScrapLog;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionOrderRepository productionOrderRepository;
    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final ScrapLogRepository scrapLogRepository;
    private final BarcodeService barcodeService;
    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ProductionOrderDto> getOrdersPaged(int page, int size, String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : Constants.DEFAULT_PAGE_SIZE, sort);

        Page<ProductionOrder> orderPage = productionOrderRepository.searchOrders(search, pageable);
        return TabulatorResponse.of(
                toOrderDtos(orderPage.getContent()),
                orderPage.getTotalPages(),
                orderPage.getTotalElements());
    }

    private List<ProductionOrderDto> toOrderDtos(List<ProductionOrder> orders) {
        Map<Long, OrderChildAggregate> metricsByOrderId = loadSlabMetrics(orders);
        return orders.stream()
                .map(order -> {
                    OrderChildAggregate metrics = metricsByOrderId.get(order.getId());
                    return ProductionOrderDto.fromEntity(
                            order,
                            OrderChildAggregate.itemCountOrZero(metrics),
                            OrderChildAggregate.totalAreaOrZero(metrics));
                })
                .toList();
    }

    private Map<Long, OrderChildAggregate> loadSlabMetrics(List<ProductionOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(ProductionOrder::getId).toList();
        return productionOrderRepository.aggregateSlabMetrics(orderIds).stream()
                .collect(Collectors.toMap(OrderChildAggregate::getParentId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public ProductionOrder getOrderById(Long id) {
        Objects.requireNonNull(id, getMessage("error.production.order.id.required"));
        return productionOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.production.order.not_found", id)));
    }

    /**
     * Executes Gangsaw block slicing and dynamically calculates slab costs via Grade Multiplier Algorithm (BRD Section 6.1)
     */
    @Transactional
    public ProductionOrder executeGangsawCut(Long blockId, String orderNo, String machineName,
                                             BigDecimal durationHours, BigDecimal electricityKwh,
                                             BigDecimal bladeWearMm, BigDecimal directCuttingExpense,
                                             String operatorName, String notes,
                                             int slabCountGradeA, int slabCountGradeB, int slabCountGradeC,
                                             BigDecimal slabWidthCm, BigDecimal slabLengthCm, BigDecimal thicknessCm,
                                             ScrapReasonCode scrapReason, BigDecimal scrapWeightKg, String scrapNotes) {

        Objects.requireNonNull(blockId, getMessage("error.block.id.required"));
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.block.not_found", blockId)));

        block.setStatus(BlockStatus.SAWING);
        blockRepository.save(block);

        ProductionOrder order = buildProductionOrder(block, orderNo, machineName, durationHours, electricityKwh, bladeWearMm, operatorName, notes);
        order = productionOrderRepository.save(order);

        BigDecimal effectiveWidth = slabWidthCm != null ? slabWidthCm : BigDecimal.valueOf(block.getWidthCm());
        BigDecimal effectiveLength = slabLengthCm != null ? slabLengthCm : BigDecimal.valueOf(block.getLengthCm());
        BigDecimal effectiveThickness = thicknessCm != null ? thicknessCm : Constants.DEFAULT_THICKNESS_CM;
        BigDecimal singleSlabAreaM2 = calculateSingleSlabAreaM2(effectiveWidth, effectiveLength);

        BigDecimal totalEquivalentArea = calculateTotalEquivalentArea(singleSlabAreaM2, slabCountGradeA, slabCountGradeB, slabCountGradeC);

        BigDecimal directExpenses = directCuttingExpense != null ? directCuttingExpense : BigDecimal.ZERO;
        BigDecimal grandTotalProductionCost = block.getTotalCost().add(directExpenses);
        BigDecimal baseCostPerM2 = calculateBaseCostPerM2(grandTotalProductionCost, totalEquivalentArea);

        BigDecimal costPerM2A = baseCostPerM2.multiply(QualityGrade.A.getMultiplier()).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal costPerM2B = baseCostPerM2.multiply(QualityGrade.B.getMultiplier()).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal costPerM2C = baseCostPerM2.multiply(QualityGrade.C.getMultiplier()).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);

        List<Slab> createdSlabs = new ArrayList<>();
        long sequence = System.currentTimeMillis() % 10000;

        createdSlabs.addAll(generateSlabsForGrade(order, block, slabCountGradeA, QualityGrade.A, effectiveThickness,
                effectiveWidth, effectiveLength, singleSlabAreaM2, costPerM2A, sequence));
        sequence += slabCountGradeA;

        createdSlabs.addAll(generateSlabsForGrade(order, block, slabCountGradeB, QualityGrade.B, effectiveThickness,
                effectiveWidth, effectiveLength, singleSlabAreaM2, costPerM2B, sequence));
        sequence += slabCountGradeB;

        createdSlabs.addAll(generateSlabsForGrade(order, block, slabCountGradeC, QualityGrade.C, effectiveThickness,
                effectiveWidth, effectiveLength, singleSlabAreaM2, costPerM2C, sequence));
        sequence += slabCountGradeC;

        slabRepository.saveAll(createdSlabs);

        recordScrapIfApplicable(order, block, scrapReason, scrapWeightKg, directExpenses, scrapNotes, operatorName, sequence);

        return order;
    }

    private ProductionOrder buildProductionOrder(Block block, String orderNo, String machineName,
                                                 BigDecimal durationHours, BigDecimal electricityKwh,
                                                 BigDecimal bladeWearMm, String operatorName, String notes) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal duration = durationHours != null ? durationHours : Constants.DEFAULT_DURATION_HOURS;
        String resolvedOrderNo = (orderNo != null && !orderNo.isBlank())
                ? orderNo
                : String.format("PRD-%d-%d", Year.now().getValue(), System.currentTimeMillis() % 100000);

        return ProductionOrder.builder()
                .orderNo(resolvedOrderNo)
                .block(block)
                .machineName(machineName)
                .processType(ProcessType.GANGSAW)
                .startTime(now.minusHours(duration.longValue()))
                .endTime(now)
                .durationHours(duration)
                .electricityKwh(electricityKwh != null ? electricityKwh : BigDecimal.ZERO)
                .bladeWearMm(bladeWearMm != null ? bladeWearMm : BigDecimal.ZERO)
                .operatorName(operatorName)
                .status(Constants.STATUS_COMPLETED)
                .notes(notes)
                .build();
    }

    private BigDecimal calculateSingleSlabAreaM2(BigDecimal widthCm, BigDecimal lengthCm) {
        return widthCm.multiply(lengthCm).divide(Constants.SQUARE_CENTIMETERS_PER_SQUARE_METER, Constants.AREA_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalEquivalentArea(BigDecimal singleSlabAreaM2, int countA, int countB, int countC) {
        BigDecimal totalAreaA = singleSlabAreaM2.multiply(BigDecimal.valueOf(countA));
        BigDecimal totalAreaB = singleSlabAreaM2.multiply(BigDecimal.valueOf(countB));
        BigDecimal totalAreaC = singleSlabAreaM2.multiply(BigDecimal.valueOf(countC));

        BigDecimal eqAreaA = totalAreaA.multiply(QualityGrade.A.getMultiplier());
        BigDecimal eqAreaB = totalAreaB.multiply(QualityGrade.B.getMultiplier());
        BigDecimal eqAreaC = totalAreaC.multiply(QualityGrade.C.getMultiplier());

        return eqAreaA.add(eqAreaB).add(eqAreaC);
    }

    private BigDecimal calculateBaseCostPerM2(BigDecimal totalProductionCost, BigDecimal totalEquivalentArea) {
        if (totalEquivalentArea.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return totalProductionCost.divide(totalEquivalentArea, Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    private List<Slab> generateSlabsForGrade(ProductionOrder order, Block block, int count, QualityGrade grade,
                                             BigDecimal thickness, BigDecimal width, BigDecimal length,
                                             BigDecimal area, BigDecimal costPerM2, long baseSequence) {
        List<Slab> slabs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String slabCode = String.format("SLB-%d-%d", Year.now().getValue(), baseSequence + i);
            slabs.add(createSlabEntity(order, block, slabCode, thickness, width, length, area, grade, costPerM2));
        }
        return slabs;
    }

    private void recordScrapIfApplicable(ProductionOrder order, Block block, ScrapReasonCode scrapReason,
                                         BigDecimal scrapWeightKg, BigDecimal directExpenses,
                                         String scrapNotes, String operatorName, long sequence) {
        if (scrapReason != null && scrapWeightKg != null && scrapWeightKg.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costImpact = directExpenses.multiply(Constants.SCRAP_COST_IMPACT_RATE).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
            String description = (scrapNotes != null && !scrapNotes.isBlank()) ? scrapNotes : scrapReason.getDescription();

            ScrapLog scrap = ScrapLog.builder()
                    .scrapCode(String.format("SCRAP-%d-%d", Year.now().getValue(), sequence))
                    .productionOrder(order)
                    .block(block)
                    .reasonCode(scrapReason)
                    .scrapWeightKg(scrapWeightKg)
                    .scrapAreaM2(BigDecimal.ZERO)
                    .costImpact(costImpact)
                    .description(description)
                    .loggedBy(operatorName)
                    .build();
            scrapLogRepository.save(scrap);
        }
    }

    private Slab createSlabEntity(ProductionOrder order, Block block, String code,
                                  BigDecimal thick, BigDecimal width, BigDecimal length,
                                  BigDecimal area, QualityGrade grade, BigDecimal cost) {
        return Slab.builder()
                .slabCode(code)
                .productionOrder(order)
                .block(block)
                .thicknessCm(thick)
                .widthCm(width)
                .lengthCm(length)
                .surfaceAreaM2(area)
                .surfaceFinish(SurfaceFinish.RAW)
                .qualityGrade(grade)
                .glossLevel(0)
                .costPerM2(cost)
                .status(SlabStatus.AVAILABLE)
                .build();
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<SlabDto> getSlabsPaged(int page, int size, String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : Constants.DEFAULT_PAGE_SIZE, sort);

        Page<Slab> slabPage = slabRepository.searchSlabs(search, pageable);
        List<SlabDto> dtos = slabPage.getContent().stream()
                .map(SlabDto::fromEntity)
                .toList();
        return TabulatorResponse.of(dtos, slabPage.getTotalPages(), slabPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ScrapLog> getAllScrapLogs() {
        return scrapLogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getScrapSummary() {
        return scrapLogRepository.getScrapSummaryByReason();
    }

    @Transactional(readOnly = true)
    public SlabLabelDto getSlabLabelData(Long id, String passportBaseUrl) {
        Slab slab = slabRepository.findByIdWithBlockAndQuarry(id)
                .orElseThrow(() -> new EntityNotFoundException(getMessage("error.slab.entity_not_found", id)));

        String baseUrl = passportBaseUrl != null && !passportBaseUrl.isBlank()
                ? passportBaseUrl
                : "http://localhost:8080/passport/";
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String passportUrl = baseUrl + slab.getSlabCode();
        String qrCodeBase64 = barcodeService.generateQrCodeBase64(passportUrl);

        String blockCode = slab.getBlock() != null ? slab.getBlock().getBlockCode() : "—";
        String stoneType = slab.getBlock() != null ? slab.getBlock().getStoneType() : "—";
        String quarryName = slab.getBlock() != null && slab.getBlock().getQuarry() != null
                ? slab.getBlock().getQuarry().getName() : "—";

        return SlabLabelDto.builder()
                .slab(slab)
                .qrCodeBase64(qrCodeBase64)
                .blockCode(blockCode)
                .stoneType(stoneType)
                .quarryName(quarryName)
                .build();
    }
}
