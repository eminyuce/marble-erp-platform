package com.ozerler.marble.service;

import com.ozerler.marble.dto.ProductionOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.ScrapLog;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionOrderRepository productionOrderRepository;
    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final ScrapLogRepository scrapLogRepository;

    @Transactional(readOnly = true)
    public TabulatorResponse<ProductionOrderDto> getOrdersPaged(int page, int size, String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : 10, sort);

        Page<ProductionOrder> orderPage = productionOrderRepository.searchOrders(search, pageable);
        List<ProductionOrderDto> dtos = orderPage.getContent().stream()
                .map(ProductionOrderDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, orderPage.getTotalPages(), orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductionOrder getOrderById(Long id) {
        return productionOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Üretim emri bulunamadı: " + id));
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

        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));

        block.setStatus(BlockStatus.SAWING);
        blockRepository.save(block);

        LocalDateTime now = LocalDateTime.now();
        ProductionOrder order = ProductionOrder.builder()
                .orderNo(orderNo != null && !orderNo.isBlank() ? orderNo : "PRD-" + Year.now().getValue() + "-" + System.currentTimeMillis() % 100000)
                .block(block)
                .machineName(machineName)
                .processType(ProcessType.GANGSAW)
                .startTime(now.minusHours(durationHours != null ? durationHours.longValue() : 8))
                .endTime(now)
                .durationHours(durationHours != null ? durationHours : new BigDecimal("8.0"))
                .electricityKwh(electricityKwh != null ? electricityKwh : BigDecimal.ZERO)
                .bladeWearMm(bladeWearMm != null ? bladeWearMm : BigDecimal.ZERO)
                .operatorName(operatorName)
                .status("COMPLETED")
                .notes(notes)
                .build();

        order = productionOrderRepository.save(order);

        // Calculate single slab area
        BigDecimal width = slabWidthCm != null ? slabWidthCm : new BigDecimal(block.getWidthCm());
        BigDecimal length = slabLengthCm != null ? slabLengthCm : new BigDecimal(block.getLengthCm());
        BigDecimal thick = thicknessCm != null ? thicknessCm : new BigDecimal("2.0");
        BigDecimal singleSlabAreaM2 = width.multiply(length).divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP);

        BigDecimal totalAreaA = singleSlabAreaM2.multiply(BigDecimal.valueOf(slabCountGradeA));
        BigDecimal totalAreaB = singleSlabAreaM2.multiply(BigDecimal.valueOf(slabCountGradeB));
        BigDecimal totalAreaC = singleSlabAreaM2.multiply(BigDecimal.valueOf(slabCountGradeC));

        // Grade Multipliers: Extra=1.30, A=1.15, B=1.00, C=0.65
        BigDecimal eqAreaA = totalAreaA.multiply(QualityGrade.A.getMultiplier());
        BigDecimal eqAreaB = totalAreaB.multiply(QualityGrade.B.getMultiplier());
        BigDecimal eqAreaC = totalAreaC.multiply(QualityGrade.C.getMultiplier());
        BigDecimal totalEquivalentArea = eqAreaA.add(eqAreaB).add(eqAreaC);

        BigDecimal blockTotalCost = block.getTotalCost();
        BigDecimal cuttingCosts = directCuttingExpense != null ? directCuttingExpense : BigDecimal.ZERO;
        BigDecimal grandTotalProductionCost = blockTotalCost.add(cuttingCosts);

        // C_base = Total Cost / Total Equivalent Area
        BigDecimal baseCostPerM2 = totalEquivalentArea.compareTo(BigDecimal.ZERO) > 0
                ? grandTotalProductionCost.divide(totalEquivalentArea, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal costPerM2A = baseCostPerM2.multiply(QualityGrade.A.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal costPerM2B = baseCostPerM2.multiply(QualityGrade.B.getMultiplier()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal costPerM2C = baseCostPerM2.multiply(QualityGrade.C.getMultiplier()).setScale(2, RoundingMode.HALF_UP);

        List<Slab> createdSlabs = new ArrayList<>();
        long seq = System.currentTimeMillis() % 10000;

        // Generate Grade A Slabs
        for (int i = 1; i <= slabCountGradeA; i++) {
            createdSlabs.add(createSlabEntity(order, block, "SLB-" + Year.now().getValue() + "-" + (seq++),
                    thick, width, length, singleSlabAreaM2, QualityGrade.A, costPerM2A));
        }
        // Generate Grade B Slabs
        for (int i = 1; i <= slabCountGradeB; i++) {
            createdSlabs.add(createSlabEntity(order, block, "SLB-" + Year.now().getValue() + "-" + (seq++),
                    thick, width, length, singleSlabAreaM2, QualityGrade.B, costPerM2B));
        }
        // Generate Grade C Slabs
        for (int i = 1; i <= slabCountGradeC; i++) {
            createdSlabs.add(createSlabEntity(order, block, "SLB-" + Year.now().getValue() + "-" + (seq++),
                    thick, width, length, singleSlabAreaM2, QualityGrade.C, costPerM2C));
        }

        slabRepository.saveAll(createdSlabs);

        // Mandatory Scrap Logging
        if (scrapReason != null && scrapWeightKg != null && scrapWeightKg.compareTo(BigDecimal.ZERO) > 0) {
            ScrapLog scrap = ScrapLog.builder()
                    .scrapCode("SCRAP-" + Year.now().getValue() + "-" + (seq++))
                    .productionOrder(order)
                    .block(block)
                    .reasonCode(scrapReason)
                    .scrapWeightKg(scrapWeightKg)
                    .scrapAreaM2(BigDecimal.ZERO)
                    .costImpact(cuttingCosts.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP))
                    .description(scrapNotes != null ? scrapNotes : scrapReason.getDescription())
                    .loggedBy(operatorName)
                    .build();
            scrapLogRepository.save(scrap);
        }

        return order;
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
    public TabulatorResponse<Slab> getSlabsPaged(int page, int size, String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : 10, sort);

        Page<Slab> slabPage = slabRepository.searchSlabs(search, pageable);
        return TabulatorResponse.of(slabPage.getContent(), slabPage.getTotalPages(), slabPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<ScrapLog> getAllScrapLogs() {
        return scrapLogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getScrapSummary() {
        return scrapLogRepository.getScrapSummaryByReason();
    }
}
