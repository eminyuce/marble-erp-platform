package com.ozerler.marble.service;

import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.ScrapLog;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ScrapReasonCode;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
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
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkshopCutService {

    private static final BigDecimal SQUARE_CENTIMETERS_PER_SQUARE_METER = new BigDecimal("10000");
    private static final BigDecimal WORKSHOP_OVERHEAD_FACTOR = new BigDecimal("1.15");
    private static final String DEFAULT_EDGE_FINISH = "PAHLI_CILALI";
    private static final String DEFAULT_TARGET_LOCATION = "Genel";
    private static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    private static final String ITEM_STATUS_READY = "READY";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int COST_SCALE = 2;
    private static final int AREA_SCALE = 4;

    private final CutOrderRepository cutOrderRepository;
    private final CutItemRepository cutItemRepository;
    private final SlabRepository slabRepository;
    private final ProjectRepository projectRepository;
    private final ProjectLocationRepository projectLocationRepository;
    private final ScrapLogRepository scrapLogRepository;

    @Transactional(readOnly = true)
    public TabulatorResponse<CutOrderDto> getCutOrdersPaged(int page, int size, String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : DEFAULT_PAGE_SIZE, sort);

        Page<CutOrder> orderPage = cutOrderRepository.searchCutOrders(search, pageable);
        List<CutOrderDto> dtos = orderPage.getContent().stream()
                .map(CutOrderDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, orderPage.getTotalPages(), orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public CutOrder getCutOrderById(Long id) {
        Objects.requireNonNull(id, "Kesim iş emri ID boş olamaz");
        return cutOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kesim iş emri bulunamadı: " + id));
    }

    @Transactional
    public CutOrder createCutOrder(Long projectId, Long locationId, Long slabId,
                                   String machineName, String operatorName,
                                   int piecesCount, BigDecimal targetWidthCm, BigDecimal targetLengthCm,
                                   String edgeFinish, String targetLocationDesc, String notes) {

        if (piecesCount <= 0) {
            throw new IllegalArgumentException("Parça adedi sıfırdan büyük olmalıdır");
        }
        if (targetWidthCm == null || targetWidthCm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hedef genişlik sıfırdan büyük olmalıdır");
        }
        if (targetLengthCm == null || targetLengthCm.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Hedef uzunluk sıfırdan büyük olmalıdır");
        }

        Slab sourceSlab = slabRepository.findById(slabId)
                .orElseThrow(() -> new IllegalArgumentException("Kaynak plaka bulunamadı: " + slabId));

        Project project = projectId != null ? projectRepository.findById(projectId).orElse(null) : null;
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

    @Transactional(readOnly = true)
    public List<CutItem> getItemsByCutOrder(Long cutOrderId) {
        Objects.requireNonNull(cutOrderId, "Kesim iş emri ID boş olamaz");
        return cutItemRepository.findByCutOrderId(cutOrderId);
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
                .status(ORDER_STATUS_COMPLETED)
                .notes(notes)
                .build();
    }

    private BigDecimal calculateItemArea(BigDecimal widthCm, BigDecimal lengthCm) {
        return widthCm.multiply(lengthCm).divide(SQUARE_CENTIMETERS_PER_SQUARE_METER, AREA_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLoadedUnitCost(BigDecimal baseCostPerM2) {
        if (baseCostPerM2 == null) {
            return BigDecimal.ZERO;
        }
        return baseCostPerM2.multiply(WORKSHOP_OVERHEAD_FACTOR).setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    private List<CutItem> generateCutItems(CutOrder order, Slab sourceSlab, ProjectLocation location,
                                          int piecesCount, BigDecimal targetWidthCm, BigDecimal targetLengthCm,
                                          BigDecimal itemArea, BigDecimal unitCost,
                                          String edgeFinish, String targetLocationDesc) {
        String finish = (edgeFinish != null && !edgeFinish.isBlank()) ? edgeFinish : DEFAULT_EDGE_FINISH;
        String resolvedLocation = (targetLocationDesc != null && !targetLocationDesc.isBlank())
                ? targetLocationDesc
                : (location != null ? location.getLocationName() : DEFAULT_TARGET_LOCATION);

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
                    .status(ITEM_STATUS_READY)
                    .build();
            items.add(item);
        }
        return items;
    }

    private void recordNestingScrapIfApplicable(CutOrder order, Slab sourceSlab, BigDecimal totalCutArea, String operatorName) {
        BigDecimal scrapArea = sourceSlab.getSurfaceAreaM2().subtract(totalCutArea);
        if (scrapArea.compareTo(BigDecimal.ZERO) > 0) {
            long sequence = System.currentTimeMillis() % 10000;
            BigDecimal costImpact = scrapArea.multiply(sourceSlab.getCostPerM2()).setScale(COST_SCALE, RoundingMode.HALF_UP);

            ScrapLog scrap = ScrapLog.builder()
                    .scrapCode("SCRAP-W-" + sequence)
                    .cutOrder(order)
                    .slab(sourceSlab)
                    .block(sourceSlab.getBlock())
                    .reasonCode(ScrapReasonCode.FR_09)
                    .scrapAreaM2(scrapArea)
                    .costImpact(costImpact)
                    .description("Atölye köprü kesmede sipariş ebatlarından artan kenar atığı.")
                    .loggedBy(operatorName)
                    .build();
            scrapLogRepository.save(scrap);
        }
    }
}
