package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.OrderChildAggregate;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.util.GridPages;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkshopCutService {

    private final CutOrderRepository cutOrderRepository;
    private final CutItemRepository cutItemRepository;
    private final SlabRepository slabRepository;
    private final ProjectRepository projectRepository;
    private final ProjectLocationRepository projectLocationRepository;
    private final ScrapLogRepository scrapLogRepository;
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
    public TabulatorResponse<CutOrderDto> getCutOrdersPaged(int page, int size, String search, String sortField, String sortDir) {
        Page<CutOrder> orderPage = GridPages.execute(page, size, sortField, sortDir,
                pageable -> cutOrderRepository.searchCutOrders(search, pageable));
        return TabulatorResponse.of(
                toCutOrderDtos(orderPage.getContent()),
                orderPage.getTotalPages(),
                orderPage.getTotalElements());
    }

    private List<CutOrderDto> toCutOrderDtos(List<CutOrder> orders) {
        Map<Long, OrderChildAggregate> metricsByOrderId = loadItemMetrics(orders);
        return orders.stream()
                .map(order -> {
                    OrderChildAggregate metrics = metricsByOrderId.get(order.getId());
                    return CutOrderDto.fromEntity(
                            order,
                            OrderChildAggregate.itemCountOrZero(metrics),
                            OrderChildAggregate.totalAreaOrZero(metrics));
                })
                .toList();
    }

    private Map<Long, OrderChildAggregate> loadItemMetrics(List<CutOrder> orders) {
        if (orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(CutOrder::getId).toList();
        return cutOrderRepository.aggregateItemMetrics(orderIds).stream()
                .collect(Collectors.toMap(OrderChildAggregate::getParentId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public CutOrder getCutOrderById(Long id) {
        Objects.requireNonNull(id, getMessage("error.cut_order.id.required"));
        return cutOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.cut_order.not_found", id)));
    }

    @Transactional(readOnly = true)
    public CutOrder getCutOrderWithDetails(Long id) {
        Objects.requireNonNull(id, getMessage("error.cut_order.id.required"));
        return cutOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.cut_order.not_found", id)));
    }

    @Transactional
    public CutOrder createCutOrder(Long projectId, Long locationId, Long slabId,
                                   String machineName, String operatorName,
                                   int piecesCount, BigDecimal targetWidthCm, BigDecimal targetLengthCm,
                                   String edgeFinish, String targetLocationDesc, String notes) {

        if (piecesCount <= 0) {
            throw new IllegalArgumentException(getMessage("error.cut_order.pieces.invalid"));
        }
        if (targetWidthCm == null || targetWidthCm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getMessage("error.cut_order.width.invalid"));
        }
        if (targetLengthCm == null || targetLengthCm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getMessage("error.cut_order.length.invalid"));
        }

        Slab sourceSlab = slabRepository.findById(slabId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.slab.not_found", slabId)));

        Project project = resolveProject(projectId);
        ProjectLocation location = locationId != null ? projectLocationRepository.findById(locationId).orElse(null) : null;

        sourceSlab.setStatus(SlabStatus.IN_CUTTING);
        slabRepository.save(sourceSlab);

        CutOrder order = buildCutOrderEntity(project, location, machineName, operatorName, notes);
        order = cutOrderRepository.save(order);

        BigDecimal itemArea = calculateItemArea(targetWidthCm, targetLengthCm);
        BigDecimal unitCost = calculateLoadedUnitCost(sourceSlab.getCostPerM2());

        List<CutItem> items = generateCutItems(order, sourceSlab, location, piecesCount,
                targetWidthCm, targetLengthCm, itemArea, unitCost, edgeFinish, targetLocationDesc);
        cutItemRepository.saveAll(items);

        BigDecimal totalCutArea = itemArea.multiply(BigDecimal.valueOf(piecesCount));
        recordNestingScrapIfApplicable(order, sourceSlab, totalCutArea, operatorName);

        return order;
    }

    @Transactional
    public CutOrder updateCutOrder(Long id, Long projectId, String machineName, String operatorName,
                                   String edgeFinish, String targetLocationDesc, String notes) {
        CutOrder order = getCutOrderById(id);
        order.setProject(resolveProject(projectId));
        order.setMachineName(machineName);
        order.setOperatorName(operatorName);
        order.setNotes(notes);
        updateExistingItemMetadata(id, edgeFinish, targetLocationDesc);
        return cutOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<CutItem> getItemsByCutOrder(Long cutOrderId) {
        Objects.requireNonNull(cutOrderId, getMessage("error.cut_order.id.required"));
        return cutItemRepository.findByCutOrderId(cutOrderId);
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectRepository.findById(projectId).orElse(null);
    }

    private void updateExistingItemMetadata(Long cutOrderId, String edgeFinish, String targetLocationDesc) {
        if (StringUtils.isBlank(edgeFinish) && StringUtils.isBlank(targetLocationDesc)) {
            return;
        }
        List<CutItem> items = cutItemRepository.findByCutOrderId(cutOrderId);
        if (items.isEmpty()) {
            return;
        }
        for (CutItem item : items) {
            if (StringUtils.isNotBlank(edgeFinish)) {
                item.setEdgeFinish(edgeFinish);
            }
            if (StringUtils.isNotBlank(targetLocationDesc)) {
                item.setTargetLocation(targetLocationDesc);
            }
        }
        cutItemRepository.saveAll(items);
    }

    private CutOrder buildCutOrderEntity(Project project, ProjectLocation location,
                                         String machineName, String operatorName, String notes) {
        String cutOrderNo = String.format("CUT-%d-%d", Year.now().getValue(), System.currentTimeMillis() % 100000);
        return CutOrder.builder()
                .cutOrderNo(cutOrderNo)
                .project(project)
                .location(location)
                .machineName(machineName)
                .operatorName(operatorName)
                .plannedStart(LocalDate.now())
                .status(Constants.STATUS_COMPLETED)
                .notes(notes)
                .build();
    }

    private BigDecimal calculateItemArea(BigDecimal widthCm, BigDecimal lengthCm) {
        return widthCm.multiply(lengthCm).divide(Constants.SQUARE_CENTIMETERS_PER_SQUARE_METER, Constants.AREA_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLoadedUnitCost(BigDecimal baseCostPerM2) {
        if (baseCostPerM2 == null) {
            return BigDecimal.ZERO;
        }
        return baseCostPerM2.multiply(Constants.WORKSHOP_OVERHEAD_FACTOR).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);
    }

    private List<CutItem> generateCutItems(CutOrder order, Slab sourceSlab, ProjectLocation location,
                                           int piecesCount, BigDecimal targetWidthCm, BigDecimal targetLengthCm,
                                           BigDecimal itemArea, BigDecimal unitCost,
                                           String edgeFinish, String targetLocationDesc) {
        String finish = StringUtils.isNotBlank(edgeFinish) ? edgeFinish : Constants.DEFAULT_EDGE_FINISH;
        String resolvedLocation = StringUtils.isNotBlank(targetLocationDesc)
                ? targetLocationDesc
                : location != null ? location.getLocationName() : Constants.DEFAULT_TARGET_LOCATION;

        List<CutItem> items = new ArrayList<>(piecesCount);
        long sequence = System.currentTimeMillis() % 10000;

        for (int i = 1; i <= piecesCount; i++) {
            String itemCode = String.format("ITM-%d-%d", Year.now().getValue(), sequence++);
            CutItem item = CutItem.builder()
                    .itemCode(itemCode)
                    .cutOrder(order)
                    .sourceSlab(sourceSlab)
                    .widthCm(targetWidthCm)
                    .lengthCm(targetLengthCm)
                    .thicknessCm(sourceSlab.getThicknessCm())
                    .areaM2(itemArea)
                    .edgeFinish(finish)
                    .unitCost(unitCost)
                    .targetLocation(resolvedLocation)
                    .status(Constants.STATUS_READY)
                    .build();
            items.add(item);
        }
        return items;
    }

    private void recordNestingScrapIfApplicable(CutOrder order, Slab sourceSlab, BigDecimal totalCutArea, String operatorName) {
        BigDecimal scrapArea = sourceSlab.getSurfaceAreaM2().subtract(totalCutArea);
        if (scrapArea.compareTo(BigDecimal.ZERO) > 0) {
            long sequence = System.currentTimeMillis() % 10000;
            BigDecimal costImpact = scrapArea.multiply(sourceSlab.getCostPerM2()).setScale(Constants.COST_SCALE, RoundingMode.HALF_UP);

            ScrapLog scrap = ScrapLog.builder()
                    .scrapCode("SCRAP-W-" + sequence)
                    .cutOrder(order)
                    .slab(sourceSlab)
                    .block(sourceSlab.getBlock())
                    .reasonCode(ScrapReasonCode.FR_09)
                    .scrapAreaM2(scrapArea)
                    .costImpact(costImpact)
                    .description(getMessage("erp.workshop.scrap.description"))
                    .loggedBy(operatorName)
                    .build();
            scrapLogRepository.save(scrap);
        }
    }

    @Transactional(readOnly = true)
    public List<Slab> getAvailableSlabs() {
        return slabRepository.findByStatus(SlabStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
}
