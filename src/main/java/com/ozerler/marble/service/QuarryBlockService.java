package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.BlockCostFormula;
import com.ozerler.marble.domain.BlockMeasurement;
import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.QuarrySummaryDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.BlockDetailPage;
import com.ozerler.marble.dto.view.BlockMovementView;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.dto.view.QuarryRef;
import com.ozerler.marble.mapper.BlockMapper;
import com.ozerler.marble.mapper.QuarryMapper;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.BlockLocationMovement;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ExpenseType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockCustomerMarkRepository;
import com.ozerler.marble.repository.BlockLocationMovementRepository;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.FactoryWorkOrderRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.ShipmentItemRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.util.GridPages;
import com.ozerler.marble.util.MessageUtils;
import com.ozerler.marble.util.UniqueCodes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuarryBlockService {

    private static final String DEFAULT_QUARRY_SECTION = "A3";
    private static final int BLOCK_CODE_SEQUENCE_WIDTH = 3;

    private final BlockRepository blockRepository;
    private final BlockMapper blockMapper;
    private final QuarryMapper quarryMapper;
    private final QuarryRepository quarryRepository;
    private final MessageSource messageSource;
    private final StockLocationRepository stockLocationRepository;
    private final BlockLocationMovementRepository movementRepository;
    private final CostAnalysisService costAnalysisService;
    private final BlockCostCalculationService blockCostCalculationService;
    private final ExpenseService expenseService;
    private final CostCenterRepository costCenterRepository;
    private final CustomerRepository customerRepository;
    private final SlabRepository slabRepository;
    private final FactoryWorkOrderRepository factoryWorkOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final BlockCustomerMarkRepository blockCustomerMarkRepository;
    private final CostTransactionRepository costTransactionRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final FileStorageService fileStorageService;
    private final QuarryInventoryService quarryInventoryService;
    private final BlockViews blockViews;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        return MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<BlockDto> getBlocksPaged(
            int page,
            int size,
            String search,
            String sortField,
            String sortDir,
            StockLocationType locationType,
            BlockStatus status) {
        return getBlocksPaged(page, size, search, sortField, sortDir, locationType, status, "IN_STOCK");
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<BlockDto> getBlocksPaged(
            int page,
            int size,
            String search,
            String sortField,
            String sortDir,
            StockLocationType locationType,
            BlockStatus status,
            String stockFilter) {
        String effectiveFilter;
        if (stockFilter == null || stockFilter.isBlank()) {
            effectiveFilter = "IN_STOCK";
        } else {
            effectiveFilter = stockFilter.trim().toUpperCase(Locale.ROOT);
        }
        Page<Block> blockPage = GridPages.execute(page, size, sortField, sortDir, GridPages.BLOCK_SORTS,
                pageable -> blockRepository.searchBlocksWithStockFilter(
                        GridPages.normalizeSearch(search), locationType, status, effectiveFilter, pageable));
        String period = YearMonth.now().toString();
        Map<Long, BigDecimal> expensePerTonByQuarry = blockCostCalculationService.expensePerTonByQuarry(period);
        List<BlockDto> dtos = blockPage.getContent().stream()
                .map(b -> toBlockDto(b, expensePerTonByQuarry))
                .collect(Collectors.toList());

        Map<String, Object> meta = gridMetricsMetaWithStockFilter(
                GridPages.normalizeSearch(search), locationType, status, effectiveFilter, expensePerTonByQuarry);
        return TabulatorResponse.of(dtos, blockPage.getTotalPages(), blockPage.getTotalElements(), meta);
    }

    @Transactional(readOnly = true)
    public String generateStandardBlockCode(Long quarryId, String section) {
        String cleanSection = normalizeQuarrySection(section);
        int year = LocalDate.now().getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        int startSequence = (int) blockRepository.countByQuarrySectionAndYear(
                quarryId, cleanSection, yearStart, yearStart.plusYears(1)) + 1;
        return UniqueCodes.sequential(cleanSection, year, startSequence, BLOCK_CODE_SEQUENCE_WIDTH,
                code -> blockRepository.existsByBlockCode(code) || blockRepository.existsByBlockCodeIgnoreCase(code));
    }

    public static String normalizeQuarrySection(String section) {
        if (section == null || section.isBlank()) {
            return DEFAULT_QUARRY_SECTION;
        }
        return section.trim().toUpperCase(Locale.ROOT).replace(' ', '-');
    }

    private static String optionalQuarrySection(String quarrySection) {
        if (quarrySection == null || quarrySection.isBlank()) {
            return null;
        }
        return normalizeQuarrySection(quarrySection);
    }

    private BlockDto toBlockDto(Block block, Map<Long, BigDecimal> expensePerTonByQuarry) {
        Long quarryId;
        if (block.getQuarry() != null) {
            quarryId = block.getQuarry().getId();
        } else {
            quarryId = null;
        }
        BigDecimal expensePerTon;
        if (quarryId != null) {
            expensePerTon = expensePerTonByQuarry.get(quarryId);
        } else {
            expensePerTon = null;
        }
        BigDecimal extraction = blockCostCalculationService.calculatedExtractionCost(block, expensePerTon);
        BigDecimal total = blockCostCalculationService.calculatedTotalCost(block, expensePerTon);
        BigDecimal m2 = blockCostCalculationService.blockFootprintM2(block);
        BigDecimal market = blockCostCalculationService.blockMarketValue(block);
        return BlockDto.fromEntity(block, canDeleteBlock(block), extraction, total, m2, market);
    }

    private Map<String, Object> gridMetricsMetaWithStockFilter(String search, StockLocationType locationType,
                                                               BlockStatus status, String stockFilter,
                                                               Map<Long, BigDecimal> expensePerTonByQuarry) {
        List<Object[]> rows =
                blockRepository.sumGridMetricsWithStockFilterByQuarry(
                        search, locationType, status, stockFilter);
        BigDecimal totalTonnage = BigDecimal.ZERO;
        BigDecimal totalSurfaceM2 = BigDecimal.ZERO;
        BigDecimal totalTransport = BigDecimal.ZERO;
        BigDecimal totalExtraction = BigDecimal.ZERO;
        Map<Long, BigDecimal> perTon;
        if (expensePerTonByQuarry != null) {
            perTon = expensePerTonByQuarry;
        } else {
            perTon = Map.of();
        }
        if (rows != null) {
            for (Object[] row : rows) {
                Object[] values = unwrapMetricRow(row);
                if (values.length < 4) {
                    continue;
                }
                Long quarryId = toLong(values[0]);
                BigDecimal tons = toBigDecimal(values[1]);
                BigDecimal surfaceM2 = toBigDecimal(values[2]);
                BigDecimal transport = toBigDecimal(values[3]);
                totalTonnage = totalTonnage.add(tons);
                totalSurfaceM2 = totalSurfaceM2.add(surfaceM2);
                totalTransport = totalTransport.add(transport);
                BigDecimal blockExtractionCost;
                if (quarryId != null) {
                    blockExtractionCost = perTon.get(quarryId);
                } else {
                    blockExtractionCost = null;
                }
                totalExtraction = totalExtraction.add(
                        BlockCostFormula.blockExtractionCost(tons, blockExtractionCost));
            }
        }
        Map<String, Object> meta = new HashMap<>();
        meta.put("totalTonnage", totalTonnage);
        meta.put("totalSurfaceM2", totalSurfaceM2);
        meta.put("totalExtractionCost", totalExtraction);
        meta.put("totalCost", totalExtraction.add(totalTransport));
        return meta;
    }

    private static Object[] unwrapMetricRow(Object[] row) {
        if (row != null && row.length == 1 && row[0] instanceof Object[] nested) {
            return nested;
        }
        if (row != null) {
            return row;
        }
        return new Object[0];
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    static String normalizeBlockCode(String blockCode) {
        if (blockCode == null) {
            return null;
        }
        return blockCode.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public Block getBlockById(Long id) {
        Objects.requireNonNull(id, getMessage("error.block.id.required"));
        return blockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.block.not_found", id)));
    }

    @Transactional(readOnly = true)
    public Block getBlockWithDetails(Long id) {
        Objects.requireNonNull(id, getMessage("error.block.id.required"));
        return blockRepository.findByIdWithQuarry(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.block.not_found", id)));
    }

    @Transactional
    public Block registerBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                               int widthCm, int lengthCm, int heightCm,
                               BigDecimal actualWeightKg, String stoneType, String colorTone,
                               QualityGrade qualityGrade, int crackLevel,
                               String notes, String photoUrls) {
        return registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                notes, photoUrls, StockLocationType.PRODUCTION_YARD);
    }

    @Transactional
    public Block registerBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                               int widthCm, int lengthCm, int heightCm,
                               BigDecimal actualWeightKg, String stoneType, String colorTone,
                               QualityGrade qualityGrade, int crackLevel,
                               String notes, String photoUrls,
                               StockLocationType locationType) {
        return registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                notes, photoUrls, locationType, null, null);
    }

    @Transactional
    public Block registerBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                               int widthCm, int lengthCm, int heightCm,
                               BigDecimal actualWeightKg, String stoneType, String colorTone,
                               QualityGrade qualityGrade, int crackLevel,
                               String notes, String photoUrls,
                               StockLocationType locationType, List<Long> fileIds, String quarrySection) {

        Objects.requireNonNull(quarryId, getMessage("error.quarry.id.required"));
        Objects.requireNonNull(blockCode, getMessage("error.block.code.required"));
        String normalizedCode = requireNormalizedBlockCode(blockCode);

        Quarry quarry = quarryRepository.findById(quarryId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.quarry.not_found", quarryId)));
        assertBlockCodeAvailable(normalizedCode, null);

        StockLocation targetLocation =
                requireLocation(
                        Objects.requireNonNullElse(
                                locationType, StockLocationType.PRODUCTION_YARD));
        LocalDate extractionDateValue;
        if (extractionDate != null) {
            extractionDateValue = extractionDate;
        } else {
            extractionDateValue = LocalDate.now();
        }
        String stoneTypeValue;
        if (stoneType != null && !stoneType.isBlank()) {
            stoneTypeValue = stoneType.trim();
        } else {
            stoneTypeValue = null;
        }
        String colorToneValue;
        if (colorTone != null && !colorTone.isBlank()) {
            colorToneValue = colorTone.trim();
        } else {
            colorToneValue = null;
        }
        Block block = Block.builder()
                .quarry(quarry)
                .blockCode(normalizedCode)
                .quarrySection(optionalQuarrySection(quarrySection))
                .extractionDate(extractionDateValue)
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .heightCm(heightCm)
                .actualWeightKg(Objects.requireNonNullElse(actualWeightKg, BigDecimal.ZERO))
                .stoneType(stoneTypeValue)
                .colorTone(colorToneValue)
                .qualityGrade(Objects.requireNonNullElse(qualityGrade, QualityGrade.A))
                .crackLevel(crackLevel)
                .status(BlockStatus.PRODUCED)
                .currentLocation(targetLocation)
                .extractionCost(BigDecimal.ZERO)
                .transportCost(BigDecimal.ZERO)
                .notes(notes)
                .photoUrls(photoUrls)
                .build();

        block.calculateMetrics(quarry.getSpecificGravity());
        Block saved = blockRepository.save(block);
        recordMovement(saved, null, targetLocation, "Blok üretimi - " + targetLocation.getName());
        if (fileIds != null && !fileIds.isEmpty()) {
            fileStorageService.attachFilesToEntity(fileIds, "BLOCK", saved.getId());
        }
        return saved;
    }

    @Transactional
    public Block registerBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                               int widthCm, int lengthCm, int heightCm,
                               BigDecimal actualWeightKg, String stoneType, String colorTone,
                               QualityGrade qualityGrade, int crackLevel,
                               String notes, String photoUrls,
                               StockLocationType locationType, List<Long> fileIds) {
        return registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                notes, photoUrls, locationType, fileIds, null);
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal unitMarketValuePerTon, String notes, String photoUrls) {
        return updateBlock(id, quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                unitMarketValuePerTon, notes, photoUrls, null);
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal unitMarketValuePerTon, String notes, String photoUrls,
                             StockLocationType locationType) {
        return updateBlock(id, quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                unitMarketValuePerTon, notes, photoUrls, locationType, (String) null);
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal unitMarketValuePerTon, String notes, String photoUrls,
                             StockLocationType locationType, String quarrySection) {
        Block block = getBlockById(id);
        String normalizedCode = requireNormalizedBlockCode(blockCode);
        assertBlockCodeAvailable(normalizedCode, id);
        if (quarryId != null && !quarryId.equals(block.getQuarry().getId())) {
            Quarry quarry = quarryRepository.findById(quarryId)
                    .orElseThrow(() -> new IllegalArgumentException(getMessage("error.quarry.not_found", quarryId)));
            block.setQuarry(quarry);
        }
        block.setBlockCode(normalizedCode);
        if (extractionDate != null) {
            block.setExtractionDate(extractionDate);
        }
        block.setWidthCm(widthCm);
        block.setLengthCm(lengthCm);
        block.setHeightCm(heightCm);
        block.setActualWeightKg(Objects.requireNonNullElse(actualWeightKg, BigDecimal.ZERO));
        String stoneTypeValue;
        if (stoneType != null && !stoneType.isBlank()) {
            stoneTypeValue = stoneType.trim();
        } else {
            stoneTypeValue = null;
        }
        block.setStoneType(stoneTypeValue);
        String colorToneValue;
        if (colorTone != null && !colorTone.isBlank()) {
            colorToneValue = colorTone.trim();
        } else {
            colorToneValue = null;
        }
        block.setColorTone(colorToneValue);
        if (qualityGrade != null) {
            block.setQualityGrade(qualityGrade);
        }
        block.setCrackLevel(crackLevel);
        if (block.getStatus() != BlockStatus.SOLD) {
            block.setUnitMarketValuePerTon(unitMarketValuePerTon);
        }
        block.setNotes(notes);
        if (photoUrls != null && !photoUrls.isBlank()) {
            block.setPhotoUrls(photoUrls);
        }
        String normalizedSection = optionalQuarrySection(quarrySection);
        if (normalizedSection != null) {
            block.setQuarrySection(normalizedSection);
        }

        // Move to specific area if changed in edit page
        if (locationType != null
                && (block.getCurrentLocation() == null
                        || block.getCurrentLocation().getLocationType() != locationType)) {
            StockLocation oldLoc = block.getCurrentLocation();
            StockLocation newLoc = requireLocation(locationType);
            block.setCurrentLocation(newLoc);
            recordMovement(block, oldLoc, newLoc, "Düzenleme formundan saha taşıma: " + newLoc.getName());
        }

        block.calculateMetrics(block.getQuarry().getSpecificGravity());
        return blockRepository.save(block);
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal unitMarketValuePerTon, String notes, String photoUrls,
                             StockLocationType locationType, List<Long> fileIds) {
        return updateBlock(id, quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                unitMarketValuePerTon, notes, photoUrls, locationType, fileIds, null);
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal unitMarketValuePerTon, String notes, String photoUrls,
                             StockLocationType locationType, List<Long> fileIds, String quarrySection) {
        Block updated = updateBlock(id, quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                unitMarketValuePerTon, notes, photoUrls, locationType, quarrySection);
        if (fileIds != null && !fileIds.isEmpty()) {
            fileStorageService.attachFilesToEntity(fileIds, "BLOCK", updated.getId());
        }
        return updated;
    }

    @Transactional(readOnly = true)
    public boolean canDeleteBlock(Long blockId) {
        if (blockId == null) {
            return false;
        }
        return blockRepository.findById(blockId).map(this::canDeleteBlock).orElse(false);
    }

    public boolean canDeleteBlock(Block block) {
        if (block == null || block.getId() == null) {
            return false;
        }
        // Sadece ocak stoğunda olan ve satılmamış bloklar silinebilir
        if (!block.getCanonicalStatus().isAtQuarry()) {
            return false;
        }
        if (block.getStatus() == BlockStatus.SOLD || block.getSoldCustomer() != null) {
            return false;
        }
        if (block.getTransportCost() != null && block.getTransportCost().compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }
        Long id = block.getId();
        if (slabRepository.existsByBlockId(id)) {
            return false;
        }
        if (factoryWorkOrderRepository.findFirstByBlockIdOrderByIdDesc(id).isPresent()) {
            return false;
        }
        if (!productionOrderRepository.findByBlockId(id).isEmpty()) {
            return false;
        }
        if (blockCustomerMarkRepository.existsByBlockId(id)) {
            return false;
        }
        if (!costTransactionRepository.findByBlockId(id).isEmpty()) {
            return false;
        }
        if (shipmentItemRepository.existsByBlockId(id)) {
            return false;
        }
        return true;
    }

    @Transactional
    public void deleteBlock(Long blockId) {
        Objects.requireNonNull(blockId, getMessage("error.block.id.required"));
        Block block = getBlockById(blockId);
        if (!canDeleteBlock(block)) {
            throw new IllegalStateException(getMessage("error.block.cannot_delete", block.getBlockCode()));
        }
        List<BlockLocationMovement> movements = movementRepository.findByBlockIdOrderByCreatedDateDesc(blockId);
        if (!movements.isEmpty()) {
            movementRepository.deleteAll(movements);
        }
        fileStorageService.deleteAllFilesForEntity("BLOCK", blockId);
        blockRepository.delete(block);
    }

    @Transactional
    public Block moveToYard(Long blockId, StockLocationType targetType, String description) {
        Block block = getBlockById(blockId);
        if (!block.getCanonicalStatus().isAtQuarry()) {
            throw new IllegalArgumentException(getMessage("error.block.move.not_at_quarry"));
        }
        StockLocation target = requireLocation(targetType);
        if (targetType != StockLocationType.PRODUCTION_YARD && targetType != StockLocationType.DISPATCH_YARD) {
            throw new IllegalArgumentException(getMessage("error.block.move.invalid_yard"));
        }
        StockLocation from = block.getCurrentLocation();
        block.setCurrentLocation(target);
        Block saved = blockRepository.save(block);
        recordMovement(saved, from, target, description);
        return saved;
    }

    @Transactional
    public Block dispatchToFactory(Long blockId, BigDecimal transportCost) {
        return transferToFactory(blockId, transportCost);
    }

    @Transactional
    public Block transferToFactory(Long blockId, BigDecimal transportCost) {
        Block block = getBlockById(blockId);
        if (!block.getCanonicalStatus().isAtQuarry()) {
            throw new IllegalArgumentException(getMessage("error.block.move.not_at_quarry"));
        }
        requireDispatchYard(block, "error.block.dispatch.not_in_dispatch_yard");
        StockLocation from = block.getCurrentLocation();
        BigDecimal cost;
        if (transportCost != null) {
            cost = transportCost;
        } else {
            cost = BigDecimal.ZERO;
        }
        block.setCurrentLocation(null);
        block.setStatus(BlockStatus.IN_TRANSIT);
        block.setTransportCost(cost);
        block.calculateMetrics(block.getQuarry().getSpecificGravity());
        Block saved = blockRepository.save(block);
        recordMovement(saved, from, null, "Fabrikaya sevk edildi (Yolda)");
        postFactoryTransportCost(saved, cost);
        return saved;
    }

    @Transactional
    public Block receiveBlockAtFactory(Long blockId) {
        Block block = getBlockById(blockId);
        if (block.getStatus() != BlockStatus.IN_TRANSIT && block.getStatus() != BlockStatus.DISPATCHED) {
            throw new IllegalStateException("Yalnızca sevk edilmiş (Yolda) bloklar fabrika stoğuna kabul edilebilir.");
        }
        StockLocation factoryYard = requireLocation(StockLocationType.FACTORY_BLOCK_YARD);
        StockLocation from = block.getCurrentLocation();
        block.setCurrentLocation(factoryYard);
        block.setStatus(BlockStatus.AT_FACTORY);
        block.setArrivalDate(LocalDate.now());
        Block saved = blockRepository.save(block);
        recordMovement(saved, from, factoryYard, "Fabrika stoğuna kabul edildi");
        return saved;
    }

    @Transactional
    public Block updateActualTonnage(Long blockId, BigDecimal actualTonnage) {
        Block block = getBlockById(blockId);
        if (!block.getCanonicalStatus().isAtQuarry()) {
            throw new IllegalArgumentException(getMessage("error.block.move.not_at_quarry"));
        }
        if (actualTonnage == null || actualTonnage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Geçerli bir gerçek tonaj değeri giriniz.");
        }
        block.setActualTonnage(actualTonnage);
        block.setActualWeightKg(actualTonnage.multiply(BigDecimal.valueOf(1000)));
        block.calculateMetrics(block.getQuarry().getSpecificGravity());
        Block saved = blockRepository.save(block);
        recordMovement(saved, block.getCurrentLocation(), block.getCurrentLocation(),
                "Kantar tartımı yapıldı: " + actualTonnage + " Ton");
        return saved;
    }

    @Transactional
    public Block updateStatus(Long blockId, BlockStatus newStatus, String notes) {
        Block block = getBlockById(blockId);
        BlockStatus oldStatus = block.getStatus();
        if (oldStatus == BlockStatus.SOLD) {
            throw new IllegalStateException("Satılmış blok durumu değiştirilemez.");
        }
        block.setStatus(newStatus);
        Block saved = blockRepository.save(block);
        String value;
        if (oldStatus != null) {
            value = oldStatus.name();
        } else {
            value = "";
        }
        String valueValue;
        if (notes != null) {
            valueValue = " (" + notes + ")";
        } else {
            valueValue = "";
        }
        recordMovement(saved, block.getCurrentLocation(), block.getCurrentLocation(),
                "Durum güncellendi: " + value + " -> " + newStatus.name() + valueValue);
        return saved;
    }

    @Transactional
    public Block sellBlockExternally(Long blockId, Long customerId, BigDecimal salePrice,
                                     LocalDate saleDate, String saleNotes) {
        Block block = getBlockById(blockId);
        if (!block.getCanonicalStatus().isAtQuarry()) {
            throw new IllegalArgumentException(getMessage("error.block.sell.not_at_quarry"));
        }
        requireDispatchYard(block, "error.block.sell.not_in_dispatch_yard");
        Objects.requireNonNull(customerId, getMessage("error.block.sell.customer.required"));
        Objects.requireNonNull(salePrice, getMessage("error.block.sell.price.required"));
        if (salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getMessage("error.block.sell.price.required"));
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.customer.not_found", customerId)));
        StockLocation from = block.getCurrentLocation();
        block.setStatus(BlockStatus.SOLD);
        block.setSoldCustomer(customer);
        block.setSalePrice(salePrice);
        LocalDate saleDateValue;
        if (saleDate != null) {
            saleDateValue = saleDate;
        } else {
            saleDateValue = LocalDate.now();
        }
        block.setSaleDate(saleDateValue);
        block.setSaleNotes(saleNotes);
        Block saved = blockRepository.save(block);
        recordMovement(saved, from, from, "Satış: " + customer.getCompanyName()
                + " — " + salePrice + " TL");
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isBlockCodeAvailable(String blockCode, Long excludeId) {
        if (blockCode == null || blockCode.isBlank()) {
            return false;
        }
        String normalized = normalizeBlockCode(blockCode);
        if (excludeId == null) {
            return !blockRepository.existsByBlockCodeIgnoreCase(normalized);
        }
        return !blockRepository.existsByBlockCodeIgnoreCaseAndIdNot(normalized, excludeId);
    }

    @Cacheable(Constants.CACHE_QUARRIES)
    @Transactional(readOnly = true)
    public List<Quarry> getAllQuarries() {
        return BoundedReads.all(quarryRepository);
    }

    @CacheEvict(value = Constants.CACHE_QUARRIES, allEntries = true)
    @Transactional
    public Quarry saveQuarry(
            Long id,
            String code,
            String name,
            String location,
            BigDecimal specificGravity,
            String licenseNo) {
        Objects.requireNonNull(code, getMessage("error.quarry.code.required"));
        Objects.requireNonNull(name, getMessage("error.quarry.name.required"));
        if (specificGravity == null || specificGravity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getMessage("error.quarry.specific_gravity.required"));
        }
        Quarry quarry;
        if (id != null) {
            quarry = quarryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException(getMessage("error.quarry.not_found", id)));
        } else {
            quarry = new Quarry();
        }
        quarry.setCode(code.trim());
        quarry.setName(name.trim());
        String locationValue;
        if (location != null) {
            locationValue = location.trim();
        } else {
            locationValue = "";
        }
        quarry.setLocation(locationValue);
        quarry.setSpecificGravity(specificGravity);
        quarry.setLicenseNo(licenseNo);
        return quarryRepository.save(quarry);
    }

    @Transactional(readOnly = true)
    public List<Block> getAvailableBlocksForProduction() {
        return blockRepository.findByStatusIn(List.of(
                BlockStatus.AT_FACTORY, BlockStatus.FACTORY_STOCK, BlockStatus.DISPATCHED, BlockStatus.IN_TRANSIT));
    }

    @Transactional(readOnly = true)
    public List<Block> getDispatchedBlocks() {
        return blockRepository.findByStatusInOrderByBlockCodeAsc(List.of(
                BlockStatus.DISPATCHED, BlockStatus.IN_TRANSIT,
                BlockStatus.AT_FACTORY, BlockStatus.FACTORY_STOCK));
    }

    @Transactional(readOnly = true)
    public List<BlockLocationMovement> getMovements(Long blockId) {
        return movementRepository.findByBlockIdOrderByCreatedDateDesc(blockId);
    }

    @Transactional(readOnly = true)
    public QuarrySummaryDto quarrySummary() {
        String period = YearMonth.now().toString();
        var analysis = costAnalysisService.analyze(BusinessUnit.QUARRY, period);
        long inTransit = blockRepository.countInTransitBlocks();
        long readyForDispatch = blockRepository.countReadyForDispatchBlocks();
        long sold = blockRepository.countSoldBlocks();
        long dispatchedToFactory = blockRepository.countDispatchedToFactoryBlocks();
        long totalInStock = blockRepository.countInStockBlocks();
        BigDecimal fuelLiters;
        if (quarryInventoryService != null) {
            fuelLiters = quarryInventoryService.getFuelStockLiters();
        } else {
            fuelLiters = BigDecimal.ZERO;
        }
        long consumablesCount = 0L;
        if (quarryInventoryService != null) {
            consumablesCount = quarryInventoryService.getConsumableStockItems().stream()
                    .mapToLong(item -> {
                        if (item.getQuantity() != null) {
                            return item.getQuantity().longValue();
                        }
                        return 0L;
                    })
                    .sum();
        }

        BigDecimal totalCostInStock = blockRepository.sumTotalCostInStock();
        BigDecimal totalMarketValueInStock = blockRepository.sumTotalMarketValueInStock();

        return QuarrySummaryDto.builder()
                .producedTonsThisMonth(analysis.getProductionQuantity())
                .productionYardCount(blockRepository.countByCurrentLocationLocationTypeAndStatusNot(
                        StockLocationType.PRODUCTION_YARD, BlockStatus.SOLD))
                .dispatchYardCount(blockRepository.countByCurrentLocationLocationTypeAndStatusNot(
                        StockLocationType.DISPATCH_YARD, BlockStatus.SOLD))
                .factoryYardCount(dispatchedToFactory)
                .soldCount(sold)
                .readyForDispatchCount(readyForDispatch)
                .inTransitCount(inTransit)
                .totalInStockCount(totalInStock)
                .markedCount(blockRepository.countMarkedBlocks())
                .fuelStockLiters(fuelLiters)
                .consumablesCount(consumablesCount)
                .costPerTonThisMonth(Objects.requireNonNullElse(analysis.getUnitCost(), BigDecimal.ZERO))
                .unallocatedCarryForward(analysis.isUnallocatedCarryForward())
                .expensePeriod(period)
                .totalCostInStock(totalCostInStock)
                .totalMarketValueInStock(totalMarketValueInStock)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isWeightDeviationWarning(Block block) {
        return block != null && BlockMeasurement.exceedsDeviationWarning(block.getWeightDeviationPct())
                && BlockMeasurement.hasActualWeight(block.getActualWeightKg());
    }

    private String requireNormalizedBlockCode(String blockCode) {
        Objects.requireNonNull(blockCode, getMessage("error.block.code.required"));
        String normalized = normalizeBlockCode(blockCode);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException(getMessage("error.block.code.required"));
        }
        return normalized;
    }

    private void assertBlockCodeAvailable(String blockCode, Long excludeId) {
        if (!isBlockCodeAvailable(blockCode, excludeId)) {
            throw new IllegalArgumentException(getMessage("error.block.code.duplicate", blockCode));
        }
    }

    private void postFactoryTransportCost(Block block, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CostCenter center =
                costCenterRepository
                        .findByCode(Constants.COST_CENTER_FACTORY_TRANSPORT)
                        .or(
                                () ->
                                        costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(
                                                BusinessUnit.FACTORY))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                getMessage(
                                                        "error.cost_center.factory_transport.missing")));
        String period = YearMonth.now().toString();
        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                center.getId(), ExpenseType.TRANSPORTATION, null, BusinessUnit.FACTORY, amount,
                Constants.CURRENCY_TRY, null, LocalDate.now(), LocalDate.now(), period, period,
                block, null, null, null, null, null, null, block.getBlockCode(),
                "Fabrika nakliye: " + block.getBlockCode()));
    }

    private void requireDispatchYard(Block block, String messageKey) {
        StockLocation location = block.getCurrentLocation();
        if (location == null || location.getLocationType() != StockLocationType.DISPATCH_YARD) {
            throw new IllegalArgumentException(getMessage(messageKey));
        }
    }

    private StockLocation requireLocation(StockLocationType type) {
        return StockLocations.require(stockLocationRepository, type);
    }

    private void recordMovement(Block block, StockLocation from, StockLocation to, String description) {
        movementRepository.save(BlockLocationMovement.builder()
                .block(block)
                .fromLocation(from)
                .toLocation(to)
                .description(description)
                .build());
    }

    /**
     * Loads a block with its quarry, location and customers and copies it into a view.
     *
     * @param id block id
     * @return the block view
     */
    @Transactional(readOnly = true)
    public BlockView getBlock(Long id) {
        return blockViews.toView(getBlockWithDetails(id));
    }

    /**
     * Loads a block and calculates the cost, value, weight warning and delete flag shown on its detail page.
     *
     * @param id block id
     * @return the detail page figures
     */
    @Transactional(readOnly = true)
    public BlockDetailPage getBlockDetailPage(Long id) {
        Block block = getBlockWithDetails(id);
        Long quarryId = null;
        if (block.getQuarry() != null) {
            quarryId = block.getQuarry().getId();
        }
        BigDecimal expensePerTon = null;
        if (quarryId != null) {
            var breakdown = blockCostCalculationService.quarryBreakdown(quarryId, YearMonth.now().toString());
            if (breakdown != null) {
                expensePerTon = breakdown.getExpensePerTon();
            }
        }
        return new BlockDetailPage(
                blockViews.toView(block),
                blockCostCalculationService.calculatedExtractionCost(block, expensePerTon),
                blockCostCalculationService.calculatedTotalCost(block, expensePerTon),
                blockCostCalculationService.blockMarketValue(block),
                isWeightDeviationWarning(block),
                canDeleteBlock(block));
    }

    /**
     * Same as {@code getAllQuarries}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<QuarryRef> getAllQuarryViews() {
        return getAllQuarries().stream().map(quarryMapper::toDto).toList();
    }

    /**
     * Same as {@code getDispatchedBlocks}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<BlockView> getDispatchedBlockViews() {
        return getDispatchedBlocks().stream().map(blockViews::toView).toList();
    }

    /**
     * Same as {@code getAvailableBlocksForProduction}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<BlockView> getAvailableBlockViewsForProduction() {
        return getAvailableBlocksForProduction().stream().map(blockViews::toView).toList();
    }

    /**
     * Same as {@code getMovements}, copied into a view inside the transaction.
     *
     * @param blockId see {@code getMovements}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<BlockMovementView> getMovementViews(Long blockId) {
        return getMovements(blockId).stream().map(blockMapper::toMovement).toList();
    }
}
