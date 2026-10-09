package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactoryStockService {

    private final BlockRepository blockRepository;
    private final SlabRepository slabRepository;
    private final StockItemRepository stockItemRepository;
    private final StockLocationRepository stockLocationRepository;
    private final CustomerRepository customerRepository;
    private final StockMovementService stockMovementService;
    private final ProductionOrderRepository productionOrderRepository;

    // --- 1. FABRİKA BLOK STOK SAHASI ---

    @Transactional(readOnly = true)
    public List<Block> getFactoryUncutBlocks(Long customerId, boolean generalStockOnly) {
        return blockRepository.findFactoryUncutBlocksFiltered(customerId, generalStockOnly);
    }

    @Transactional(readOnly = true)
    public List<Block> getIncomingBlocksInTransit() {
        return blockRepository.findByStatusIn(List.of(BlockStatus.IN_TRANSIT, BlockStatus.DISPATCHED));
    }

    @Transactional
    public Block receiveBlockAtFactory(Long blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));
        if (block.getStatus() != BlockStatus.IN_TRANSIT && block.getStatus() != BlockStatus.DISPATCHED) {
            throw new IllegalStateException("Yalnızca sevk edilmiş (Yolda) bloklar fabrikanın stoğuna kabul edilebilir.");
        }
        StockLocation factoryYard = StockLocations.require(stockLocationRepository, StockLocationType.FACTORY_BLOCK_YARD);
        StockLocation oldLoc = block.getCurrentLocation();
        block.setCurrentLocation(factoryYard);
        block.setStatus(BlockStatus.AT_FACTORY);
        block.setArrivalDate(LocalDate.now());
        Block saved = blockRepository.save(block);
        stockMovementService.recordBlockMovement(saved, oldLoc, factoryYard, "Fabrika stoğuna kabul edildi (Stoğa Al)");
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Block> getFactoryUncutBlocksPaged(Long customerId, boolean generalStockOnly, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return blockRepository.searchFactoryUncutBlocks(customerId, generalStockOnly, search, pageable);
    }

    @Transactional
    public Block assignCustomerToBlock(Long blockId, Long customerId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));

        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        block.setAssignedCustomer(customer);
        return blockRepository.save(block);
    }

    @Transactional
    public Block dispatchBlockToCutting(Long blockId, String notes) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));

        if (block.getStatus() != BlockStatus.AT_FACTORY && block.getStatus() != BlockStatus.FACTORY_STOCK) {
            throw new IllegalStateException("Yalnızca fabrikada ve henüz kesilmemiş bloklar kesime alınabilir.");
        }

        block.setStatus(BlockStatus.IN_PROCESS);
        Block saved = blockRepository.save(block);
        stockMovementService.recordBlockCuttingStart(saved, notes);
        return saved;
    }

    // --- 2. FABRİKA PLAKA STOK SAHASI ---

    @Transactional(readOnly = true)
    public Page<Slab> getFactorySlabsPaged(SlabStatus status, Long customerId, boolean generalStockOnly, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return slabRepository.searchSlabsWithCustomer(status, customerId, generalStockOnly, search, pageable);
    }

    @Transactional
    public Slab assignCustomerToSlab(Long slabId, Long customerId) {
        Slab slab = slabRepository.findById(slabId)
                .orElseThrow(() -> new IllegalArgumentException("Plaka bulunamadı: " + slabId));

        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        slab.setCustomer(customer);
        if (customer != null && slab.getStatus() == SlabStatus.AVAILABLE) {
            slab.setStatus(SlabStatus.RESERVED);
        } else if (customer == null && slab.getStatus() == SlabStatus.RESERVED) {
            slab.setStatus(SlabStatus.AVAILABLE);
        }
        return slabRepository.save(slab);
    }

    @Transactional
    public Slab createSlabDirect(Long blockId, BigDecimal thicknessCm, BigDecimal widthCm, BigDecimal lengthCm,
                                QualityGrade qualityGrade, SurfaceFinish surfaceFinish, Long customerId, String notes) {

        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Kaynak blok bulunamadı: " + blockId));

        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        BigDecimal thick = thicknessCm != null ? thicknessCm : new BigDecimal("2.00");
        BigDecimal width = widthCm != null ? widthCm : BigDecimal.valueOf(block.getWidthCm());
        BigDecimal length = lengthCm != null ? lengthCm : BigDecimal.valueOf(block.getLengthCm());
        BigDecimal area = width.multiply(length).divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);

        String orderNo = UniqueCodes.yearly("PRD", productionOrderRepository::existsByOrderNo);
        ProductionOrder order = productionOrderRepository.save(ProductionOrder.builder()
                .orderNo(orderNo)
                .block(block)
                .machineName("ST / Katrak")
                .processType(com.ozerler.marble.model.enums.ProcessType.ST)
                .startTime(java.time.LocalDateTime.now())
                .status("COMPLETED")
                .operatorName("Operatör")
                .build());

        String slabCode = UniqueCodes.yearly("SLB", slabRepository::existsBySlabCode);

        Slab slab = Slab.builder()
                .slabCode(slabCode)
                .productionOrder(order)
                .block(block)
                .thicknessCm(thick)
                .widthCm(width)
                .lengthCm(length)
                .surfaceAreaM2(area)
                .qualityGrade(qualityGrade != null ? qualityGrade : QualityGrade.A)
                .surfaceFinish(surfaceFinish != null ? surfaceFinish : SurfaceFinish.RAW)
                .customer(customer)
                .status(customer != null ? SlabStatus.RESERVED : SlabStatus.AVAILABLE)
                .costPerM2(BigDecimal.ZERO)
                .build();

        Slab saved = slabRepository.save(slab);
        stockMovementService.recordSlabProduction(saved, block, customer);
        return saved;
    }

    @Transactional
    public List<Slab> createSlabsBatch(Long blockId, BigDecimal commonThicknessCm,
                                       QualityGrade qualityGrade, SurfaceFinish surfaceFinish,
                                       Long customerId, String notes,
                                       List<BigDecimal> thicknesses, List<BigDecimal> widths,
                                       List<BigDecimal> lengths, List<Integer> pieceCounts) {

        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Kaynak blok bulunamadı: " + blockId));
        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;

        String orderNo = UniqueCodes.yearly("PRD", productionOrderRepository::existsByOrderNo);
        ProductionOrder order = productionOrderRepository.save(ProductionOrder.builder()
                .orderNo(orderNo)
                .block(block)
                .machineName("ST / Katrak")
                .processType(com.ozerler.marble.model.enums.ProcessType.ST)
                .startTime(java.time.LocalDateTime.now())
                .status("COMPLETED")
                .operatorName("Operatör")
                .build());

        List<Slab> createdSlabs = new ArrayList<>();
        if (widths == null || widths.isEmpty()) return createdSlabs;

        for (int i = 0; i < widths.size(); i++) {
            BigDecimal w = widths.get(i);
            if (w == null || w.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal l = (lengths != null && i < lengths.size() && lengths.get(i) != null) ? lengths.get(i) : w;
            BigDecimal t = (thicknesses != null && i < thicknesses.size() && thicknesses.get(i) != null) ? thicknesses.get(i) : (commonThicknessCm != null ? commonThicknessCm : new BigDecimal("2.00"));
            int count = (pieceCounts != null && i < pieceCounts.size() && pieceCounts.get(i) != null) ? pieceCounts.get(i) : 1;
            BigDecimal area = w.multiply(l).divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);

            for (int k = 0; k < count; k++) {
                String slabCode = UniqueCodes.yearly("SLB", slabRepository::existsBySlabCode);
                Slab slab = Slab.builder()
                        .slabCode(slabCode)
                        .productionOrder(order)
                        .block(block)
                        .thicknessCm(t)
                        .widthCm(w)
                        .lengthCm(l)
                        .surfaceAreaM2(area)
                        .qualityGrade(qualityGrade != null ? qualityGrade : QualityGrade.A)
                        .surfaceFinish(surfaceFinish != null ? surfaceFinish : SurfaceFinish.RAW)
                        .customer(customer)
                        .status(customer != null ? SlabStatus.RESERVED : SlabStatus.AVAILABLE)
                        .costPerM2(BigDecimal.ZERO)
                        .build();

                Slab saved = slabRepository.save(slab);
                stockMovementService.recordSlabProduction(saved, block, customer);
                createdSlabs.add(saved);
            }
        }
        return createdSlabs;
    }

    @Transactional(readOnly = true)
    public BigDecimal getCustomerSlabAreaTotal(Long customerId) {
        if (customerId == null) return BigDecimal.ZERO;
        return slabRepository.getTotalAreaByCustomer(customerId);
    }

    // --- 3. FABRİKA EBATLI STOK SAHASI ---

    @Transactional(readOnly = true)
    public Page<StockItem> getSizedStockItemsPaged(Long customerId, boolean generalStockOnly, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return stockItemRepository.searchItems(StockProductType.SIZED, null, customerId, generalStockOnly, search, pageable);
    }

    @Transactional
    public StockItem createSizedItem(Long blockId, String description, BigDecimal thicknessCm, BigDecimal widthCm,
                                     BigDecimal lengthCm, BigDecimal quantity, int pieceCount,
                                     BigDecimal actualProducedQuantity, Long customerId, String qualityGradeStr,
                                     String surfaceFinishStr, String edgeFinish, String notes) {

        Block block = blockId != null ? blockRepository.findById(blockId).orElse(null) : null;
        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;

        StockLocation sizedLoc = stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.SIZED_STOCK_YARD)
                .orElseGet(() -> stockLocationRepository.save(StockLocation.builder()
                        .code("FAB-EBATLI")
                        .name("Fabrika Ebatlı Stok Sahası")
                        .businessUnit(BusinessUnit.FACTORY)
                        .locationType(StockLocationType.SIZED_STOCK_YARD)
                        .active(true)
                        .build()));

        String code = UniqueCodes.yearly("EBT", stockItemRepository::existsByItemCode);

        BigDecimal thick = thicknessCm != null ? thicknessCm : new BigDecimal("2.00");
        BigDecimal w = widthCm != null ? widthCm : new BigDecimal("60.00");
        BigDecimal l = lengthCm != null ? lengthCm : new BigDecimal("60.00");
        BigDecimal qty = quantity != null ? quantity : w.multiply(l).multiply(BigDecimal.valueOf(pieceCount)).divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);

        QualityGrade grade = QualityGrade.A;
        if (qualityGradeStr != null) {
            try { grade = QualityGrade.valueOf(qualityGradeStr); } catch (Exception ignored) {}
        }
        SurfaceFinish surface = SurfaceFinish.POLISHED;
        if (surfaceFinishStr != null) {
            try { surface = SurfaceFinish.valueOf(surfaceFinishStr); } catch (Exception ignored) {}
        }

        StockItem item = StockItem.builder()
                .itemCode(code)
                .productType(StockProductType.SIZED)
                .sourceBlock(block)
                .stoneType(block != null ? block.getStoneType() : "Mermer")
                .description(description != null && !description.isBlank() ? description : "Ebatlı Mermer " + thick + "x" + w + "x" + l)
                .thicknessCm(thick)
                .widthCm(w)
                .lengthCm(l)
                .quantity(qty)
                .unit("m2")
                .pieceCount(Math.max(1, pieceCount))
                .actualProducedQuantity(actualProducedQuantity != null ? actualProducedQuantity : qty)
                .customer(customer)
                .stockLocation(sizedLoc)
                .qualityGrade(grade)
                .surfaceFinish(surface)
                .edgeFinish(edgeFinish != null ? edgeFinish : "DUZ")
                .status("AVAILABLE")
                .productionDate(LocalDate.now())
                .notes(notes)
                .build();

        StockItem saved = stockItemRepository.save(item);
        stockMovementService.recordSizedItemProduction(saved, block, customer);
        return saved;
    }

    @Transactional
    public List<StockItem> createSizedItemsBatch(Long blockId, Long customerId, String defaultDescription,
                                                 String qualityGradeStr, String surfaceFinishStr, String edgeFinish,
                                                 String notes,
                                                 List<String> descriptions, List<BigDecimal> thicknesses,
                                                 List<BigDecimal> widths, List<BigDecimal> lengths,
                                                 List<Integer> pieceCounts, List<BigDecimal> quantities) {
        List<StockItem> created = new ArrayList<>();
        if (widths == null || widths.isEmpty()) return created;

        for (int i = 0; i < widths.size(); i++) {
            BigDecimal w = widths.get(i);
            if (w == null || w.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal l = (lengths != null && i < lengths.size() && lengths.get(i) != null) ? lengths.get(i) : w;
            BigDecimal t = (thicknesses != null && i < thicknesses.size() && thicknesses.get(i) != null) ? thicknesses.get(i) : new BigDecimal("2.0");
            int count = (pieceCounts != null && i < pieceCounts.size() && pieceCounts.get(i) != null) ? pieceCounts.get(i) : 1;
            BigDecimal qty = (quantities != null && i < quantities.size() && quantities.get(i) != null)
                    ? quantities.get(i)
                    : w.multiply(l).multiply(BigDecimal.valueOf(count)).divide(BigDecimal.valueOf(10000), 4, RoundingMode.HALF_UP);
            String desc = (descriptions != null && i < descriptions.size() && descriptions.get(i) != null && !descriptions.get(i).isBlank())
                    ? descriptions.get(i).trim()
                    : (defaultDescription != null && !defaultDescription.isBlank() ? defaultDescription : ("Ebatlı Mermer " + t + "x" + w + "x" + l));

            StockItem item = createSizedItem(blockId, desc, t, w, l, qty, count, qty, customerId, qualityGradeStr, surfaceFinishStr, edgeFinish, notes);
            created.add(item);
        }
        return created;
    }

    @Transactional
    public StockItem assignCustomerToStockItem(Long itemId, Long customerId) {
        StockItem item = stockItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Stok ürünü bulunamadı: " + itemId));

        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        item.setCustomer(customer);
        return stockItemRepository.save(item);
    }
}
