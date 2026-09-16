package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.domain.BlockMeasurement;
import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.QuarrySummaryDto;
import com.ozerler.marble.dto.TabulatorResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuarryBlockService {

    private final BlockRepository blockRepository;
    private final QuarryRepository quarryRepository;
    private final org.springframework.context.MessageSource messageSource;
    private final StockLocationRepository stockLocationRepository;
    private final BlockLocationMovementRepository movementRepository;
    private final CostAnalysisService costAnalysisService;
    private final ExpenseService expenseService;
    private final CostCenterRepository costCenterRepository;
    private final CustomerRepository customerRepository;
    private final SlabRepository slabRepository;
    private final FactoryWorkOrderRepository factoryWorkOrderRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final BlockCustomerMarkRepository blockCustomerMarkRepository;
    private final CostTransactionRepository costTransactionRepository;
    private final ShipmentItemRepository shipmentItemRepository;

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
    public TabulatorResponse<BlockDto> getBlocksPaged(int page, int size, String search, String sortField, String sortDir,
                                                    StockLocationType locationType, BlockStatus status) {
        boolean unsoldOnly = status == null;
        Page<Block> blockPage = GridPages.execute(page, size, sortField, sortDir, GridPages.BLOCK_SORTS,
                pageable -> blockRepository.searchBlocks(
                        GridPages.normalizeSearch(search), locationType, status, unsoldOnly, pageable));
        List<BlockDto> dtos = blockPage.getContent().stream()
                .map(b -> BlockDto.fromEntity(b, canDeleteBlock(b)))
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, blockPage.getTotalPages(), blockPage.getTotalElements());
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
                               BigDecimal extractionCost, String notes, String photoUrls) {
        return registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                extractionCost, notes, photoUrls, StockLocationType.PRODUCTION_YARD);
    }

    @Transactional
    public Block registerBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                               int widthCm, int lengthCm, int heightCm,
                               BigDecimal actualWeightKg, String stoneType, String colorTone,
                               QualityGrade qualityGrade, int crackLevel,
                               BigDecimal extractionCost, String notes, String photoUrls,
                               StockLocationType locationType) {

        Objects.requireNonNull(quarryId, getMessage("error.quarry.id.required"));
        Objects.requireNonNull(blockCode, getMessage("error.block.code.required"));

        Quarry quarry = quarryRepository.findById(quarryId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.quarry.not_found", quarryId)));
        assertBlockCodeAvailable(blockCode.trim(), null);

        StockLocation targetLocation = requireLocation(locationType != null ? locationType : StockLocationType.PRODUCTION_YARD);
        Block block = Block.builder()
                .quarry(quarry)
                .blockCode(blockCode.trim())
                .extractionDate(extractionDate != null ? extractionDate : LocalDate.now())
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .heightCm(heightCm)
                .actualWeightKg(actualWeightKg != null ? actualWeightKg : BigDecimal.ZERO)
                .stoneType(stoneType)
                .colorTone(colorTone)
                .qualityGrade(qualityGrade != null ? qualityGrade : QualityGrade.A)
                .crackLevel(crackLevel)
                .status(BlockStatus.PRODUCED)
                .currentLocation(targetLocation)
                .extractionCost(extractionCost != null ? extractionCost : BigDecimal.ZERO)
                .transportCost(BigDecimal.ZERO)
                .notes(notes)
                .photoUrls(photoUrls)
                .build();

        block.calculateMetrics(quarry.getSpecificGravity());
        Block saved = blockRepository.save(block);
        recordMovement(saved, null, targetLocation, "Blok üretimi - " + targetLocation.getName());
        return saved;
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal extractionCost, String notes, String photoUrls) {
        return updateBlock(id, quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel,
                extractionCost, notes, photoUrls, null);
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal extractionCost, String notes, String photoUrls,
                             StockLocationType locationType) {
        Block block = getBlockById(id);
        assertBlockCodeAvailable(blockCode.trim(), id);
        if (quarryId != null && !quarryId.equals(block.getQuarry().getId())) {
            Quarry quarry = quarryRepository.findById(quarryId)
                    .orElseThrow(() -> new IllegalArgumentException(getMessage("error.quarry.not_found", quarryId)));
            block.setQuarry(quarry);
        }
        block.setBlockCode(blockCode.trim());
        if (extractionDate != null) {
            block.setExtractionDate(extractionDate);
        }
        block.setWidthCm(widthCm);
        block.setLengthCm(lengthCm);
        block.setHeightCm(heightCm);
        block.setActualWeightKg(actualWeightKg != null ? actualWeightKg : BigDecimal.ZERO);
        block.setStoneType(stoneType);
        block.setColorTone(colorTone);
        if (qualityGrade != null) {
            block.setQualityGrade(qualityGrade);
        }
        block.setCrackLevel(crackLevel);
        block.setExtractionCost(extractionCost != null ? extractionCost : BigDecimal.ZERO);
        block.setNotes(notes);
        if (photoUrls != null && !photoUrls.isBlank()) {
            block.setPhotoUrls(photoUrls);
        }

        // Move to specific area if changed in edit page
        if (locationType != null && (block.getCurrentLocation() == null || block.getCurrentLocation().getLocationType() != locationType)) {
            StockLocation oldLoc = block.getCurrentLocation();
            StockLocation newLoc = requireLocation(locationType);
            block.setCurrentLocation(newLoc);
            recordMovement(block, oldLoc, newLoc, "Düzenleme formundan saha taşıma: " + newLoc.getName());
        }

        block.calculateMetrics(block.getQuarry().getSpecificGravity());
        return blockRepository.save(block);
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
        // Sadece ocakta üretilmiş (PRODUCED) ve satılmamış bloklar silinebilir
        if (block.getStatus() != BlockStatus.PRODUCED) {
            return false;
        }
        if (block.getSoldCustomer() != null) {
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
        StockLocation factoryYard = requireLocation(StockLocationType.FACTORY_BLOCK_YARD);
        StockLocation from = block.getCurrentLocation();
        BigDecimal cost = transportCost != null ? transportCost : BigDecimal.ZERO;
        block.setCurrentLocation(factoryYard);
        block.setStatus(BlockStatus.AT_FACTORY);
        block.setTransportCost(cost);
        block.calculateMetrics(block.getQuarry().getSpecificGravity());
        Block saved = blockRepository.save(block);
        recordMovement(saved, from, factoryYard, "Fabrika Blok Sahasına sevk edildi");
        postFactoryTransportCost(saved, cost);
        return saved;
    }

    @Transactional
    public Block sellBlockExternally(Long blockId, Long customerId) {
        Block block = getBlockById(blockId);
        if (!block.getCanonicalStatus().isAtQuarry()) {
            throw new IllegalArgumentException(getMessage("error.block.sell.not_at_quarry"));
        }
        Objects.requireNonNull(customerId, getMessage("error.block.sell.customer.required"));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.customer.not_found", customerId)));
        StockLocation from = block.getCurrentLocation();
        block.setStatus(BlockStatus.SOLD);
        block.setSoldCustomer(customer);
        Block saved = blockRepository.save(block);
        recordMovement(saved, from, from, "Satış: " + customer.getCompanyName());
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isBlockCodeAvailable(String blockCode, Long excludeId) {
        if (blockCode == null || blockCode.isBlank()) {
            return false;
        }
        String trimmed = blockCode.trim();
        if (excludeId == null) {
            return !blockRepository.existsByBlockCode(trimmed);
        }
        return !blockRepository.existsByBlockCodeAndIdNot(trimmed, excludeId);
    }

    @Cacheable(Constants.CACHE_QUARRIES)
    @Transactional(readOnly = true)
    public List<Quarry> getAllQuarries() {
        return quarryRepository.findAll();
    }

    @CacheEvict(value = Constants.CACHE_QUARRIES, allEntries = true)
    @Transactional
    public Quarry saveQuarry(Long id, String code, String name, String location, BigDecimal specificGravity, String licenseNo) {
        Objects.requireNonNull(code, getMessage("error.quarry.code.required"));
        Objects.requireNonNull(name, getMessage("error.quarry.name.required"));
        if (specificGravity == null || specificGravity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getMessage("error.quarry.specific_gravity.required"));
        }
        Quarry quarry = id != null ? quarryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.quarry.not_found", id)))
                : new Quarry();
        quarry.setCode(code.trim());
        quarry.setName(name.trim());
        quarry.setLocation(location != null ? location.trim() : "");
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
        return blockRepository.findByStatusIn(List.of(
                BlockStatus.DISPATCHED, BlockStatus.IN_TRANSIT, BlockStatus.AT_FACTORY));
    }

    @Transactional(readOnly = true)
    public List<BlockLocationMovement> getMovements(Long blockId) {
        return movementRepository.findByBlockIdOrderByCreatedDateDesc(blockId);
    }

    @Transactional(readOnly = true)
    public QuarrySummaryDto quarrySummary() {
        String period = YearMonth.now().toString();
        var analysis = costAnalysisService.analyze(BusinessUnit.QUARRY, period);
        return QuarrySummaryDto.builder()
                .producedTonsThisMonth(analysis.getProductionQuantity())
                .productionYardCount(blockRepository.countByCurrentLocation_LocationTypeAndStatusNot(
                        StockLocationType.PRODUCTION_YARD, BlockStatus.SOLD))
                .dispatchYardCount(blockRepository.countByCurrentLocation_LocationTypeAndStatusNot(
                        StockLocationType.DISPATCH_YARD, BlockStatus.SOLD))
                .factoryYardCount(0L)
                .soldCount(blockRepository.countByStatus(BlockStatus.SOLD))
                .costPerTonThisMonth(analysis.getUnitCost() != null ? analysis.getUnitCost() : BigDecimal.ZERO)
                .unallocatedCarryForward(analysis.isUnallocatedCarryForward())
                .expensePeriod(period)
                .build();
    }

    @Transactional(readOnly = true)
    public boolean isWeightDeviationWarning(Block block) {
        return block != null && BlockMeasurement.exceedsDeviationWarning(block.getWeightDeviationPct())
                && BlockMeasurement.hasActualWeight(block.getActualWeightKg());
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
        CostCenter center = costCenterRepository.findByCode(Constants.COST_CENTER_FACTORY_TRANSPORT)
                .or(() -> costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.FACTORY))
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.cost_center.factory_transport.missing")));
        String period = YearMonth.now().toString();
        expenseService.recordExpense(new ExpenseService.ExpenseDraft(
                center.getId(), ExpenseType.TRANSPORTATION, null, BusinessUnit.FACTORY, amount,
                Constants.CURRENCY_TRY, null, LocalDate.now(), LocalDate.now(), period, period,
                block, null, null, null, null, null, block.getBlockCode(),
                "Fabrika nakliye: " + block.getBlockCode()));
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
}
