package com.ozerler.marble.service;

import com.ozerler.marble.dto.*;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.ScrapLog;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.FactoryProcessType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.QuantityUnit;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.util.GridPages;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionService {

    private final ProductionOrderRepository productionOrderRepository;
    private final SlabRepository slabRepository;
    private final ScrapLogRepository scrapLogRepository;
    private final BarcodeService barcodeService;
    private final org.springframework.context.MessageSource messageSource;
    private final FactoryProductionService factoryProductionService;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    public record ProductionSummaryDto(long totalOrders, long activeOrders, long completedOrders, BigDecimal totalSlabAreaM2) {}

    @Transactional(readOnly = true)
    public ProductionSummaryDto getProductionSummary() {
        long totalOrders = productionOrderRepository.count();
        long activeOrders = productionOrderRepository.countActiveOrders();
        long completedOrders = Math.max(0, totalOrders - activeOrders);
        BigDecimal totalSlabArea = slabRepository.findAll().stream()
                .map(com.ozerler.marble.model.Slab::getSurfaceAreaM2)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProductionSummaryDto(totalOrders, activeOrders, completedOrders, totalSlabArea);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ProductionOrderDto> getOrdersPaged(int page, int size, String search, String sortField, String sortDir) {
        return getOrdersPaged(page, size, search, null, sortField, sortDir);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ProductionOrderDto> getOrdersPaged(int page, int size, String search, String status, String sortField, String sortDir) {
        Page<ProductionOrder> orderPage = GridPages.execute(page, size, sortField, sortDir, GridPages.PRODUCTION_ORDER_SORTS,
                pageable -> productionOrderRepository.searchOrders(GridPages.normalizeSearch(search), pageable));
        List<ProductionOrder> orders = orderPage.getContent();
        if (status != null && !status.isBlank()) {
            orders = orders.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus().equalsIgnoreCase(status))
                    .toList();
        }
        return TabulatorResponse.of(
                toOrderDtos(orders),
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

    @Transactional(readOnly = true)
    public ProductionOrder getOrderWithDetails(Long id) {
        Objects.requireNonNull(id, getMessage("error.production.order.id.required"));
        ProductionOrder order = productionOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.production.order.not_found", id)));
        Hibernate.initialize(order.getSlabs());
        Hibernate.initialize(order.getScrapLogs());
        return order;
    }

    /**
     * Legacy entry point kept for existing callers; ST/Katrak cutting is recorded through factory operations.
     */
    @Transactional
    public ProductionOrder executeGangsawCut(Long blockId, String orderNo, String machineName,
                                             BigDecimal durationHours, BigDecimal electricityKwh,
                                             BigDecimal bladeWearMm, BigDecimal directCuttingExpense,
                                             String operatorName, String notes,
                                             int slabCountGradeA, int slabCountGradeB, int slabCountGradeC,
                                             BigDecimal slabWidthCm, BigDecimal slabLengthCm, BigDecimal thicknessCm,
                                             ScrapReasonCode scrapReason, BigDecimal scrapWeightKg, String scrapNotes) {
        return factoryProductionService.recordCutting(new FactoryProductionService.CuttingRequest(
                blockId, orderNo, null, machineName, FactoryProcessType.GANGSAW_CUTTING,
                durationHours, electricityKwh, bladeWearMm, directCuttingExpense,
                operatorName, notes, 0, slabCountGradeA, slabCountGradeB, slabCountGradeC,
                slabWidthCm, slabLengthCm, thicknessCm, null, null, scrapWeightKg, QuantityUnit.KG,
                scrapReason, scrapWeightKg, scrapNotes));
    }

    public record SlabSummaryDto(long totalSlabs, BigDecimal totalAreaM2, BigDecimal avgCostPerM2, long reservedCount) {}

    @Transactional(readOnly = true)
    public SlabSummaryDto getSlabSummary() {
        List<Slab> all = slabRepository.findAll();
        long totalSlabs = all.size();
        BigDecimal totalArea = all.stream()
                .map(Slab::getSurfaceAreaM2)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = all.stream()
                .map(s -> (s.getCostPerM2() != null && s.getSurfaceAreaM2() != null) ? s.getCostPerM2().multiply(s.getSurfaceAreaM2()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgCost = (totalArea.compareTo(BigDecimal.ZERO) > 0)
                ? totalCost.divide(totalArea, 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        long reserved = all.stream().filter(s -> s.getStatus() != null && "RESERVED".equalsIgnoreCase(s.getStatus().name())).count();
        return new SlabSummaryDto(totalSlabs, totalArea, avgCost, reserved);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<SlabDto> getSlabsPaged(int page, int size, String search, String sortField, String sortDir) {
        return getSlabsPaged(page, size, search, null, null, sortField, sortDir);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<SlabDto> getSlabsPaged(int page, int size, String search, String status, String quality, String sortField, String sortDir) {
        Page<Slab> slabPage = GridPages.execute(page, size, sortField, sortDir, GridPages.SLAB_SORTS,
                pageable -> slabRepository.searchSlabs(GridPages.normalizeSearch(search), pageable));
        List<Slab> slabs = slabPage.getContent();
        if (status != null && !status.isBlank()) {
            slabs = slabs.stream().filter(s -> s.getStatus() != null && s.getStatus().name().equalsIgnoreCase(status)).toList();
        }
        if (quality != null && !quality.isBlank()) {
            slabs = slabs.stream().filter(s -> s.getQualityGrade() != null && s.getQualityGrade().name().equalsIgnoreCase(quality)).toList();
        }
        List<SlabDto> dtos = slabs.stream()
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
    public Slab getSlabWithDetails(Long id) {
        Objects.requireNonNull(id, getMessage("error.slab.id.required"));
        return slabRepository.findWithDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(getMessage("error.slab.entity_not_found", id)));
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

    @Transactional
    public Slab updateSlab(Long id,
                           String slabCode,
                           BigDecimal thicknessCm,
                           BigDecimal widthCm,
                           BigDecimal lengthCm,
                           SurfaceFinish surfaceFinish,
                           QualityGrade qualityGrade,
                           Integer glossLevel,
                           BigDecimal costPerM2,
                           SlabStatus status) {
        Objects.requireNonNull(id, getMessage("error.slab.id.required"));
        Objects.requireNonNull(slabCode, getMessage("error.slab.code.required"));

        Slab slab = slabRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(getMessage("error.slab.entity_not_found", id)));

        slab.setSlabCode(slabCode.trim());
        if (thicknessCm != null) {
            slab.setThicknessCm(thicknessCm);
        }
        if (widthCm != null) {
            slab.setWidthCm(widthCm);
        }
        if (lengthCm != null) {
            slab.setLengthCm(lengthCm);
        }
        slab.calculateArea();
        if (surfaceFinish != null) {
            slab.setSurfaceFinish(surfaceFinish);
        }
        if (qualityGrade != null) {
            slab.setQualityGrade(qualityGrade);
        }
        if (glossLevel != null) {
            slab.setGlossLevel(glossLevel);
        }
        if (costPerM2 != null) {
            slab.setCostPerM2(costPerM2);
        }
        if (status != null) {
            slab.setStatus(status);
        }
        return slabRepository.save(slab);
    }
}
