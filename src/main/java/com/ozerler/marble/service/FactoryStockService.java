package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.StockProductType;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.util.UniqueCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactoryStockService {

    private final BlockRepository blockRepository;
    private final BlockViews blockViews;
    private final com.ozerler.marble.mapper.StockItemMapper stockItemMapper;
    private final com.ozerler.marble.mapper.SlabMapper slabMapper;
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

        boolean isAtFactory = block.getStatus() == BlockStatus.AT_FACTORY || block.getStatus() == BlockStatus.FACTORY_STOCK
                || (block.getCurrentLocation() != null && block.getCurrentLocation().getLocationType() == StockLocationType.FACTORY_BLOCK_YARD);
        boolean isAlreadyCutOrSold = block.getStatus() == BlockStatus.IN_PROCESS || block.getStatus() == BlockStatus.SAWING
                || block.getStatus() == BlockStatus.SOLD || block.getStatus() == BlockStatus.SCRAPPED;
        if (!isAtFactory || isAlreadyCutOrSold) {
            throw new IllegalStateException("Yalnızca fabrikada bulunan ve henüz kesilmemiş bloklar kesime alınabilir.");
        }

        block.setStatus(BlockStatus.IN_PROCESS);
        Block saved = blockRepository.save(block);
        stockMovementService.recordBlockCuttingStart(saved, notes);
        return saved;
    }

    public record BlockYardFigures(int count, BigDecimal weightTon, BigDecimal costValue, BigDecimal saleValue) {
    }

    @Transactional(readOnly = true)
    public BlockYardFigures factoryBlockFigures(Long customerId, boolean generalStockOnly) {
        List<Block> blocks = getFactoryUncutBlocks(customerId, generalStockOnly);
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal sale = BigDecimal.ZERO;
        for (Block block : blocks) {
            weight = weight.add(block.getActualTonnage() != null ? block.getActualTonnage()
                    : block.getApproximateTonnage() != null ? block.getApproximateTonnage() : BigDecimal.ZERO);
            BigDecimal lineCost = block.getTotalCost() != null ? block.getTotalCost() : BigDecimal.ZERO;
            cost = cost.add(lineCost);
            sale = sale.add(saleValue(block, lineCost));
        }
        return new BlockYardFigures(blocks.size(), weight, cost, sale);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<Map<String, Object>> factoryBlockRows(int page, int size, String search,
                                                                   Long customerId, boolean generalStockOnly) {
        Page<Block> blocksPage = getFactoryUncutBlocksPaged(customerId, generalStockOnly, search, Math.max(0, page - 1), size);
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        List<Map<String, Object>> data = blocksPage.getContent().stream().map(block -> blockRow(block, dateFmt))
                .collect(Collectors.toList());
        return TabulatorResponse.of(data, blocksPage.getTotalPages(), blocksPage.getTotalElements());
    }

    @Transactional
    public String assignCustomerMessage(Long blockId, Long customerId) {
        Block block = assignCustomerToBlock(blockId, customerId);
        String custName = block.getAssignedCustomer() != null ? block.getAssignedCustomer().getCompanyName() : "Genel Stok";
        return "Blok (" + block.getBlockCode() + ") müşterisi güncellendi: " + custName;
    }

    @Transactional
    public String dispatchCuttingMessage(Long blockId, String notes) {
        Block block = dispatchBlockToCutting(blockId, notes);
        return "Blok (" + block.getBlockCode() + ") kesime alındı. Kesilmemiş blok stokundan düşüldü.";
    }

    @Transactional
    public String receiveAtFactoryMessage(Long blockId) {
        Block block = receiveBlockAtFactory(blockId);
        return "Blok (" + block.getBlockCode() + ") başarıyla fabrika stok sahasına kabul edildi.";
    }

    private static BigDecimal saleValue(Block block, BigDecimal cost) {
        if (block.getSalePrice() != null && block.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
            return block.getSalePrice();
        }
        if (block.getUnitMarketValuePerTon() != null && block.getUnitMarketValuePerTon().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ton = block.getActualTonnage() != null ? block.getActualTonnage()
                    : block.getApproximateTonnage() != null ? block.getApproximateTonnage() : BigDecimal.ZERO;
            return block.getUnitMarketValuePerTon().multiply(ton);
        }
        return cost;
    }

    private static Map<String, Object> blockRow(Block block, DateTimeFormatter dateFmt) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", block.getId());
        map.put("blockCode", block.getBlockCode());
        map.put("stoneType", block.getStoneType() != null ? block.getStoneType() : "—");
        map.put("quarryName", block.getQuarry() != null ? block.getQuarry().getName()
                : block.getQuarryName() != null ? block.getQuarryName() : "Merkez Ocak");
        map.put("arrivalDate", block.getArrivalDate() != null ? block.getArrivalDate().format(dateFmt)
                : block.getCreatedDate() != null ? block.getCreatedDate().format(dateFmt) : "—");
        map.put("estimatedTonnage", block.getEstimatedTonnage() != null ? block.getEstimatedTonnage()
                : block.getApproximateTonnage());
        map.put("actualTonnage", block.getActualTonnage());
        map.put("dimensions", block.getDimensions() != null ? block.getDimensions() : "—");
        map.put("customerName", block.getAssignedCustomer() != null ? block.getAssignedCustomer().getCompanyName() : null);
        map.put("customerId", block.getAssignedCustomer() != null ? block.getAssignedCustomer().getId() : null);
        map.put("isGeneralStock", block.getAssignedCustomer() == null);
        map.put("statusLabel", "Kesilmemiş Blok");
        map.put("notes", block.getNotes() != null ? block.getNotes() : "");
        BigDecimal cost = block.getTotalCost() != null ? block.getTotalCost() : BigDecimal.ZERO;
        BigDecimal saleOrMarket = block.getSalePrice();
        if ((saleOrMarket == null || saleOrMarket.compareTo(BigDecimal.ZERO) <= 0) && block.getUnitMarketValuePerTon() != null) {
            BigDecimal ton = block.getActualTonnage() != null ? block.getActualTonnage()
                    : block.getApproximateTonnage() != null ? block.getApproximateTonnage() : BigDecimal.ZERO;
            saleOrMarket = block.getUnitMarketValuePerTon().multiply(ton).setScale(2, RoundingMode.HALF_UP);
        }
        if (saleOrMarket == null || saleOrMarket.compareTo(BigDecimal.ZERO) <= 0) {
            saleOrMarket = cost;
        }
        map.put("totalCost", cost);
        map.put("salePrice", saleOrMarket);
        return map;
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
                .processType(ProcessType.ST)
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
                .processType(ProcessType.ST)
                .startTime(java.time.LocalDateTime.now())
                .status("COMPLETED")
                .operatorName("Operatör")
                .build());

        List<Slab> createdSlabs = new ArrayList<>();
        if (widths == null || widths.isEmpty()) {
            return createdSlabs;
        }

        for (int i = 0; i < widths.size(); i++) {
            RowDimensions.Row row = RowDimensions.positiveWidth(
                    widths, lengths, thicknesses, i,
                    commonThicknessCm != null ? commonThicknessCm : new BigDecimal("2.00"));
            if (row == null) {
                continue;
            }
            BigDecimal w = row.width();
            BigDecimal l = row.length();
            BigDecimal t = row.thickness();
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
        if (customerId == null) {
            return BigDecimal.ZERO;
        }
        return slabRepository.getTotalAreaByCustomer(customerId);
    }

    // --- 3. FABRİKA EBATLI STOK SAHASI ---

    @Transactional(readOnly = true)
    public Page<StockItem> getSizedStockItemsPaged(Long customerId, boolean generalStockOnly, boolean customerOnly, String search, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<StockItem> items = stockItemRepository.searchItemsDetailed(StockProductType.SIZED, null, customerId, generalStockOnly, customerOnly, search, pageable);
        if (items.hasContent()) {
            items.getContent().forEach(it -> {
                if (it.getSourceBlock() != null) {
                    it.getSourceBlock().getBlockCode();
                }
                if (it.getCustomer() != null) {
                    it.getCustomer().getCompanyName();
                }
                if (it.getStockLocation() != null) {
                    it.getStockLocation().getName();
                }
            });
        }
        return items;
    }

    @Transactional(readOnly = true)
    public Page<StockItem> getSizedStockItemsPaged(Long customerId, boolean generalStockOnly, String search, int page, int size) {
        return getSizedStockItemsPaged(customerId, generalStockOnly, false, search, page, size);
    }

    @Transactional
    public StockItem createSizedItem(Long blockId, String description, BigDecimal thicknessCm, BigDecimal widthCm,
                                     BigDecimal lengthCm, BigDecimal quantity, int pieceCount,
                                     BigDecimal actualProducedQuantity, Long customerId, String qualityGradeStr,
                                     String surfaceFinishStr, String edgeFinish, String notes) {
        return createSizedItem(blockId, null, description, thicknessCm, widthCm, lengthCm, quantity, pieceCount,
                actualProducedQuantity, customerId, qualityGradeStr, surfaceFinishStr, edgeFinish, notes);
    }

    @Transactional
    public StockItem createSizedItem(Long blockId, Long slabId, String description, BigDecimal thicknessCm, BigDecimal widthCm,
                                     BigDecimal lengthCm, BigDecimal quantity, int pieceCount,
                                     BigDecimal actualProducedQuantity, Long customerId, String qualityGradeStr,
                                     String surfaceFinishStr, String edgeFinish, String notes) {

        Block block = blockId != null ? blockRepository.findById(blockId).orElse(null) : null;
        Slab slab = slabId != null ? slabRepository.findById(slabId).orElse(null) : null;
        if (block == null && slab != null && slab.getBlock() != null) {
            block = slab.getBlock();
        }
        Customer customer = customerId != null ? customerRepository.findById(customerId).orElse(null) : null;
        if (customer == null && slab != null && slab.getCustomer() != null) {
            customer = slab.getCustomer();
        }

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
        BigDecimal qty = quantity != null
                ? quantity.setScale(2, RoundingMode.HALF_UP)
                : w.multiply(l).multiply(BigDecimal.valueOf(pieceCount)).divide(BigDecimal.valueOf(10000), 2, RoundingMode.HALF_UP);

        QualityGrade grade = QualityGrade.A;
        if (qualityGradeStr != null) {
            try { grade = QualityGrade.valueOf(qualityGradeStr); } catch (Exception ignored) {}
        }
        SurfaceFinish surface = SurfaceFinish.POLISHED;
        if (surfaceFinishStr != null) {
            try { surface = SurfaceFinish.valueOf(surfaceFinishStr); } catch (Exception ignored) {}
        }

        BigDecimal actualProduced = actualProducedQuantity != null ? actualProducedQuantity.setScale(2, RoundingMode.HALF_UP) : qty;

        String combinedNotes = notes;
        if (slab != null) {
            String slabInfo = "[Kaynak Plaka: " + slab.getSlabCode() + "]";
            combinedNotes = (combinedNotes != null && !combinedNotes.isBlank()) ? combinedNotes + " " + slabInfo : slabInfo;
        }

        StockItem item = StockItem.builder()
                .itemCode(code)
                .productType(StockProductType.SIZED)
                .sourceBlock(block)
                .stoneType(block != null ? block.getStoneType() : slab != null && slab.getBlock() != null ? slab.getBlock().getStoneType() : "Mermer")
                .description(description != null && !description.isBlank() ? description : "Ebatlı Mermer " + thick + "x" + w + "x" + l)
                .thicknessCm(thick)
                .widthCm(w)
                .lengthCm(l)
                .quantity(qty)
                .unit("m2")
                .pieceCount(Math.max(1, pieceCount))
                .actualProducedQuantity(actualProduced)
                .customer(customer)
                .stockLocation(sizedLoc)
                .qualityGrade(grade)
                .surfaceFinish(surface)
                .edgeFinish(edgeFinish != null ? edgeFinish : "DUZ")
                .status("AVAILABLE")
                .productionDate(LocalDate.now())
                .notes(combinedNotes)
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
        return createSizedItemsBatch(blockId, null, customerId, defaultDescription, qualityGradeStr, surfaceFinishStr,
                edgeFinish, notes, descriptions, thicknesses, widths, lengths, pieceCounts, quantities, null, null);
    }

    @Transactional
    public List<StockItem> createSizedItemsBatch(Long blockId, Long slabId, Long customerId, String defaultDescription,
                                                 String qualityGradeStr, String surfaceFinishStr, String edgeFinish,
                                                 String notes,
                                                 List<String> descriptions, List<BigDecimal> thicknesses,
                                                 List<BigDecimal> widths, List<BigDecimal> lengths,
                                                 List<Integer> pieceCounts, List<BigDecimal> quantities,
                                                 List<String> rowSurfaceFinishes, List<String> rowEdgeFinishes) {
        List<StockItem> created = new ArrayList<>();
        if (widths == null || widths.isEmpty()) {
            return created;
        }

        RowDimensions.eachPositiveSizedRow(widths, lengths, thicknesses, pieceCounts, quantities, new BigDecimal("2.0"),
                (index, width, length, thickness, count, quantity) -> {
            String desc = (descriptions != null && index < descriptions.size() && descriptions.get(index) != null && !descriptions.get(index).isBlank())
                    ? descriptions.get(index).trim()
                    : defaultDescription != null && !defaultDescription.isBlank() ? defaultDescription : ("Ebatlı Mermer " + thickness + "x" + width + "x" + length);

            String rowSurface = (rowSurfaceFinishes != null && index < rowSurfaceFinishes.size() && rowSurfaceFinishes.get(index) != null && !rowSurfaceFinishes.get(index).isBlank())
                    ? rowSurfaceFinishes.get(index).trim() : surfaceFinishStr;
            String rowEdge = (rowEdgeFinishes != null && index < rowEdgeFinishes.size() && rowEdgeFinishes.get(index) != null && !rowEdgeFinishes.get(index).isBlank())
                    ? rowEdgeFinishes.get(index).trim() : edgeFinish;

            StockItem item = createSizedItem(blockId, slabId, desc, thickness, width, length, quantity, count, quantity, customerId, qualityGradeStr, rowSurface, rowEdge, notes);
            created.add(item);
        });
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

    /**
     * Same as {@code createSlabDirect}, copied into a view inside the transaction.
     *
     * @param blockId see {@code createSlabDirect}
     * @param thicknessCm see {@code createSlabDirect}
     * @param widthCm see {@code createSlabDirect}
     * @param lengthCm see {@code createSlabDirect}
     * @param qualityGrade see {@code createSlabDirect}
     * @param surfaceFinish see {@code createSlabDirect}
     * @param customerId see {@code createSlabDirect}
     * @param notes see {@code createSlabDirect}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.SlabView createSlabDirectView(Long blockId, BigDecimal thicknessCm, BigDecimal widthCm, BigDecimal lengthCm,
                                QualityGrade qualityGrade, SurfaceFinish surfaceFinish, Long customerId, String notes) {
        return slabMapper.toDto(createSlabDirect(blockId, thicknessCm, widthCm, lengthCm, qualityGrade, surfaceFinish, customerId, notes));
    }

    /**
     * Same as {@code assignCustomerToSlab}, copied into a view inside the transaction.
     *
     * @param slabId see {@code assignCustomerToSlab}
     * @param customerId see {@code assignCustomerToSlab}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.SlabView assignCustomerToSlabView(Long slabId, Long customerId) {
        return slabMapper.toDto(assignCustomerToSlab(slabId, customerId));
    }

    /**
     * Same as {@code createSizedItem}, copied into a view inside the transaction.
     *
     * @param blockId see {@code createSizedItem}
     * @param description see {@code createSizedItem}
     * @param thicknessCm see {@code createSizedItem}
     * @param widthCm see {@code createSizedItem}
     * @param lengthCm see {@code createSizedItem}
     * @param quantity see {@code createSizedItem}
     * @param pieceCount see {@code createSizedItem}
     * @param actualProducedQuantity see {@code createSizedItem}
     * @param customerId see {@code createSizedItem}
     * @param qualityGradeStr see {@code createSizedItem}
     * @param surfaceFinishStr see {@code createSizedItem}
     * @param edgeFinish see {@code createSizedItem}
     * @param notes see {@code createSizedItem}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView createSizedItemView(Long blockId, String description, BigDecimal thicknessCm, BigDecimal widthCm,
                                     BigDecimal lengthCm, BigDecimal quantity, int pieceCount,
                                     BigDecimal actualProducedQuantity, Long customerId, String qualityGradeStr,
                                     String surfaceFinishStr, String edgeFinish, String notes) {
        return stockItemMapper.toDto(createSizedItem(blockId, description, thicknessCm, widthCm, lengthCm, quantity, pieceCount, actualProducedQuantity, customerId, qualityGradeStr, surfaceFinishStr, edgeFinish, notes));
    }

    /**
     * Same as {@code createSizedItem}, copied into a view inside the transaction.
     *
     * @param blockId see {@code createSizedItem}
     * @param slabId see {@code createSizedItem}
     * @param description see {@code createSizedItem}
     * @param thicknessCm see {@code createSizedItem}
     * @param widthCm see {@code createSizedItem}
     * @param lengthCm see {@code createSizedItem}
     * @param quantity see {@code createSizedItem}
     * @param pieceCount see {@code createSizedItem}
     * @param actualProducedQuantity see {@code createSizedItem}
     * @param customerId see {@code createSizedItem}
     * @param qualityGradeStr see {@code createSizedItem}
     * @param surfaceFinishStr see {@code createSizedItem}
     * @param edgeFinish see {@code createSizedItem}
     * @param notes see {@code createSizedItem}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView createSizedItemView(Long blockId, Long slabId, String description, BigDecimal thicknessCm, BigDecimal widthCm,
                                     BigDecimal lengthCm, BigDecimal quantity, int pieceCount,
                                     BigDecimal actualProducedQuantity, Long customerId, String qualityGradeStr,
                                     String surfaceFinishStr, String edgeFinish, String notes) {
        return stockItemMapper.toDto(createSizedItem(blockId, slabId, description, thicknessCm, widthCm, lengthCm, quantity, pieceCount, actualProducedQuantity, customerId, qualityGradeStr, surfaceFinishStr, edgeFinish, notes));
    }

    /**
     * Same as {@code assignCustomerToStockItem}, copied into a view inside the transaction.
     *
     * @param itemId see {@code assignCustomerToStockItem}
     * @param customerId see {@code assignCustomerToStockItem}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.StockItemView assignCustomerToStockItemView(Long itemId, Long customerId) {
        return stockItemMapper.toDto(assignCustomerToStockItem(itemId, customerId));
    }

    /**
     * Same as {@code createSlabsBatch}, copied into a view inside the transaction.
     *
     * @param blockId see {@code createSlabsBatch}
     * @param commonThicknessCm see {@code createSlabsBatch}
     * @param qualityGrade see {@code createSlabsBatch}
     * @param surfaceFinish see {@code createSlabsBatch}
     * @param customerId see {@code createSlabsBatch}
     * @param notes see {@code createSlabsBatch}
     * @param thicknesses see {@code createSlabsBatch}
     * @param widths see {@code createSlabsBatch}
     * @param lengths see {@code createSlabsBatch}
     * @param pieceCounts see {@code createSlabsBatch}
     * @return the view
     */
    @Transactional
    public List<com.ozerler.marble.dto.view.SlabView> createSlabsBatchViews(Long blockId, BigDecimal commonThicknessCm,
                                       QualityGrade qualityGrade, SurfaceFinish surfaceFinish,
                                       Long customerId, String notes,
                                       List<BigDecimal> thicknesses, List<BigDecimal> widths,
                                       List<BigDecimal> lengths, List<Integer> pieceCounts) {
        return createSlabsBatch(blockId, commonThicknessCm, qualityGrade, surfaceFinish, customerId, notes, thicknesses, widths, lengths, pieceCounts).stream().map(slabMapper::toDto).toList();
    }

    /**
     * Same as {@code createSizedItemsBatch}, copied into a view inside the transaction.
     *
     * @param blockId see {@code createSizedItemsBatch}
     * @param customerId see {@code createSizedItemsBatch}
     * @param defaultDescription see {@code createSizedItemsBatch}
     * @param qualityGradeStr see {@code createSizedItemsBatch}
     * @param surfaceFinishStr see {@code createSizedItemsBatch}
     * @param edgeFinish see {@code createSizedItemsBatch}
     * @param notes see {@code createSizedItemsBatch}
     * @param descriptions see {@code createSizedItemsBatch}
     * @param thicknesses see {@code createSizedItemsBatch}
     * @param widths see {@code createSizedItemsBatch}
     * @param lengths see {@code createSizedItemsBatch}
     * @param pieceCounts see {@code createSizedItemsBatch}
     * @param quantities see {@code createSizedItemsBatch}
     * @return the view
     */
    @Transactional
    public List<com.ozerler.marble.dto.view.StockItemView> createSizedItemsBatchViews(Long blockId, Long customerId, String defaultDescription,
                                                 String qualityGradeStr, String surfaceFinishStr, String edgeFinish,
                                                 String notes,
                                                 List<String> descriptions, List<BigDecimal> thicknesses,
                                                 List<BigDecimal> widths, List<BigDecimal> lengths,
                                                 List<Integer> pieceCounts, List<BigDecimal> quantities) {
        return createSizedItemsBatch(blockId, customerId, defaultDescription, qualityGradeStr, surfaceFinishStr, edgeFinish, notes, descriptions, thicknesses, widths, lengths, pieceCounts, quantities).stream().map(stockItemMapper::toDto).toList();
    }

    /**
     * Same as {@code createSizedItemsBatch}, copied into a view inside the transaction.
     *
     * @param blockId see {@code createSizedItemsBatch}
     * @param slabId see {@code createSizedItemsBatch}
     * @param customerId see {@code createSizedItemsBatch}
     * @param defaultDescription see {@code createSizedItemsBatch}
     * @param qualityGradeStr see {@code createSizedItemsBatch}
     * @param surfaceFinishStr see {@code createSizedItemsBatch}
     * @param edgeFinish see {@code createSizedItemsBatch}
     * @param notes see {@code createSizedItemsBatch}
     * @param descriptions see {@code createSizedItemsBatch}
     * @param thicknesses see {@code createSizedItemsBatch}
     * @param widths see {@code createSizedItemsBatch}
     * @param lengths see {@code createSizedItemsBatch}
     * @param pieceCounts see {@code createSizedItemsBatch}
     * @param quantities see {@code createSizedItemsBatch}
     * @param rowSurfaceFinishes see {@code createSizedItemsBatch}
     * @param rowEdgeFinishes see {@code createSizedItemsBatch}
     * @return the view
     */
    @Transactional
    public List<com.ozerler.marble.dto.view.StockItemView> createSizedItemsBatchViews(Long blockId, Long slabId, Long customerId, String defaultDescription,
                                                 String qualityGradeStr, String surfaceFinishStr, String edgeFinish,
                                                 String notes,
                                                 List<String> descriptions, List<BigDecimal> thicknesses,
                                                 List<BigDecimal> widths, List<BigDecimal> lengths,
                                                 List<Integer> pieceCounts, List<BigDecimal> quantities,
                                                 List<String> rowSurfaceFinishes, List<String> rowEdgeFinishes) {
        return createSizedItemsBatch(blockId, slabId, customerId, defaultDescription, qualityGradeStr, surfaceFinishStr, edgeFinish, notes, descriptions, thicknesses, widths, lengths, pieceCounts, quantities, rowSurfaceFinishes, rowEdgeFinishes).stream().map(stockItemMapper::toDto).toList();
    }

    /**
     * Same as {@code getFactorySlabsPaged}, copied into a view inside the transaction.
     *
     * @param status see {@code getFactorySlabsPaged}
     * @param customerId see {@code getFactorySlabsPaged}
     * @param generalStockOnly see {@code getFactorySlabsPaged}
     * @param search see {@code getFactorySlabsPaged}
     * @param page see {@code getFactorySlabsPaged}
     * @param size see {@code getFactorySlabsPaged}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.view.SlabView> getFactorySlabViewsPaged(SlabStatus status, Long customerId, boolean generalStockOnly, String search, int page, int size) {
        return getFactorySlabsPaged(status, customerId, generalStockOnly, search, page, size).map(slabMapper::toDto);
    }

    /**
     * Same as {@code getSizedStockItemsPaged}, copied into a view inside the transaction.
     *
     * @param customerId see {@code getSizedStockItemsPaged}
     * @param generalStockOnly see {@code getSizedStockItemsPaged}
     * @param customerOnly see {@code getSizedStockItemsPaged}
     * @param search see {@code getSizedStockItemsPaged}
     * @param page see {@code getSizedStockItemsPaged}
     * @param size see {@code getSizedStockItemsPaged}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.view.StockItemView> getSizedStockItemViewsPaged(Long customerId, boolean generalStockOnly, boolean customerOnly, String search, int page, int size) {
        return getSizedStockItemsPaged(customerId, generalStockOnly, customerOnly, search, page, size).map(stockItemMapper::toDto);
    }

    /**
     * Same as {@code getSizedStockItemsPaged}, copied into a view inside the transaction.
     *
     * @param customerId see {@code getSizedStockItemsPaged}
     * @param generalStockOnly see {@code getSizedStockItemsPaged}
     * @param search see {@code getSizedStockItemsPaged}
     * @param page see {@code getSizedStockItemsPaged}
     * @param size see {@code getSizedStockItemsPaged}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.view.StockItemView> getSizedStockItemViewsPaged(Long customerId, boolean generalStockOnly, String search, int page, int size) {
        return getSizedStockItemsPaged(customerId, generalStockOnly, search, page, size).map(stockItemMapper::toDto);
    }

    /**
     * Same as {@code getFactoryUncutBlocks}, copied into a view inside the transaction.
     *
     * @param customerId see {@code getFactoryUncutBlocks}
     * @param generalStockOnly see {@code getFactoryUncutBlocks}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.BlockView> getFactoryUncutBlockViews(Long customerId, boolean generalStockOnly) {
        return getFactoryUncutBlocks(customerId, generalStockOnly).stream().map(blockViews::toView).toList();
    }

    /**
     * Same as {@code getIncomingBlocksInTransit}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.BlockView> getIncomingBlockViewsInTransit() {
        return getIncomingBlocksInTransit().stream().map(blockViews::toView).toList();
    }

    /**
     * Same as {@code getFactoryUncutBlocksPaged}, copied into a view inside the transaction.
     *
     * @param customerId see {@code getFactoryUncutBlocksPaged}
     * @param generalStockOnly see {@code getFactoryUncutBlocksPaged}
     * @param search see {@code getFactoryUncutBlocksPaged}
     * @param page see {@code getFactoryUncutBlocksPaged}
     * @param size see {@code getFactoryUncutBlocksPaged}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.view.BlockView> getFactoryUncutBlockViewsPaged(Long customerId, boolean generalStockOnly, String search, int page, int size) {
        return getFactoryUncutBlocksPaged(customerId, generalStockOnly, search, page, size).map(blockViews::toView);
    }
}
