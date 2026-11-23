package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.enums.TargetDestination;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.StockLocationRepository;
import com.ozerler.marble.util.UniqueCodes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarryOperationService {

    private final BlockRepository blockRepository;
    private final BlockViews blockViews;
    private final com.ozerler.marble.mapper.QuarryMapper quarryMapper;
    private final com.ozerler.marble.mapper.BlockMapper blockMapper;
    private final QuarryRepository quarryRepository;
    private final StockLocationRepository stockLocationRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final StockMovementService stockMovementService;

    @Transactional
    public Block createQuarryBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                                   int widthCm, int lengthCm, int heightCm,
                                   BigDecimal estimatedTonnage, BigDecimal actualTonnage,
                                   String stoneType, String colorTone, QualityGrade qualityGrade,
                                   int crackLevel, String quarrySection, String notes) {

        Objects.requireNonNull(quarryId, "Ocak seçimi zorunludur.");
        Quarry quarry = quarryRepository.findById(quarryId)
                .orElseThrow(() -> new IllegalArgumentException("Ocak bulunamadı: " + quarryId));

        String code;
        if (blockCode != null && !blockCode.isBlank()) {
            code = blockCode.trim().toUpperCase();
        } else {
            code = UniqueCodes.yearly("BLK", blockRepository::existsByBlockCode);
        }

        StockLocation prodYard = stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.PRODUCTION_YARD)
                .orElseGet(() -> stockLocationRepository.save(StockLocation.builder()
                        .code("OCAK-URETIM")
                        .name("Üretim Sahası")
                        .businessUnit(BusinessUnit.QUARRY)
                        .locationType(StockLocationType.PRODUCTION_YARD)
                        .active(true)
                        .build()));

        BigDecimal est = estimatedTonnage;
        BigDecimal act = actualTonnage;

        String quarrySectionValue;
        if (quarrySection != null && !quarrySection.isBlank()) {
            quarrySectionValue = quarrySection.trim();
        } else {
            quarrySectionValue = "A1";
        }
        LocalDate extractionDateValue;
        if (extractionDate != null) {
            extractionDateValue = extractionDate;
        } else {
            extractionDateValue = LocalDate.now();
        }
        BigDecimal actualWeightKg;
        if (act != null && act.compareTo(BigDecimal.ZERO) > 0) {
            actualWeightKg = act.multiply(BigDecimal.valueOf(1000));
        } else {
            actualWeightKg = BigDecimal.ZERO;
        }
        String stoneTypeValue;
        if (stoneType != null && !stoneType.isBlank()) {
            stoneTypeValue = stoneType.trim();
        } else {
            stoneTypeValue = quarry.getName();
        }
        Block block = Block.builder()
                .quarry(quarry)
                .blockCode(code)
                .quarrySection(quarrySectionValue)
                .extractionDate(extractionDateValue)
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .heightCm(heightCm)
                .estimatedTonnage(est)
                .actualTonnage(act)
                .actualWeightKg(actualWeightKg)
                .stoneType(stoneTypeValue)
                .colorTone(colorTone)
                .qualityGrade(Objects.requireNonNullElse(qualityGrade, QualityGrade.A))
                .crackLevel(crackLevel)
                .status(BlockStatus.PRODUCED)
                .currentLocation(prodYard)
                .targetDestination(TargetDestination.FACTORY)
                .notes(notes)
                .build();

        block.calculateMetrics(quarry.getSpecificGravity());
        if (est != null) {
            block.setEstimatedTonnage(est);
        }
        if (act != null) {
            block.setActualTonnage(act);
        }

        Block saved = blockRepository.save(block);
        stockMovementService.recordBlockQuarryProduction(saved, prodYard);
        return saved;
    }

    @Transactional
    public Block updateActualTonnage(Long blockId, BigDecimal actualTonnage) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));

        if (actualTonnage == null || actualTonnage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gerçek tonaj 0'dan büyük olmalıdır.");
        }

        block.setActualTonnage(actualTonnage);
        block.setActualWeightKg(actualTonnage.multiply(BigDecimal.valueOf(1000)));
        BigDecimal calculateMetrics;
        if (block.getQuarry() != null) {
            calculateMetrics = block.getQuarry().getSpecificGravity();
        } else {
            calculateMetrics = new BigDecimal("2.70");
        }
        block.calculateMetrics(calculateMetrics);
        block.setActualTonnage(actualTonnage); // preserve explicitly

        return blockRepository.save(block);
    }

    @Transactional
    public Block dispatchBlock(Long blockId, TargetDestination targetDestination, Long customerId, Long invoiceId, String notes) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));

        StockLocation fromLoc = block.getCurrentLocation();
        StockLocation toLoc = null;
        Customer customer = null;
        Invoice invoice = null;

        if (invoiceId != null) {
            invoice = invoiceRepository.findById(invoiceId).orElse(null);
        }

        if (targetDestination == TargetDestination.FACTORY) {
            toLoc = stockLocationRepository.findByLocationTypeAndActiveTrue(StockLocationType.FACTORY_BLOCK_YARD)
                    .orElseGet(() -> stockLocationRepository.save(StockLocation.builder()
                            .code("FAB-BLOK")
                            .name("Fabrika Blok Sahası")
                            .businessUnit(BusinessUnit.FACTORY)
                            .locationType(StockLocationType.FACTORY_BLOCK_YARD)
                            .active(true)
                            .build()));
            block.setCurrentLocation(toLoc);
            block.setStatus(BlockStatus.AT_FACTORY);
            block.setArrivalDate(LocalDate.now());
            block.setTargetDestination(TargetDestination.FACTORY);
        } else if (targetDestination == TargetDestination.CUSTOMER) {
            if (customerId == null) {
                throw new IllegalArgumentException("Müşteriye sevk için müşteri seçilmelidir.");
            }
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + customerId));
            block.setSoldCustomer(customer);
            block.setAssignedCustomer(customer);
            block.setStatus(BlockStatus.SOLD);
            block.setSaleDate(LocalDate.now());
            block.setTargetDestination(TargetDestination.CUSTOMER);
        } else {
            block.setTargetDestination(Objects.requireNonNullElse(targetDestination, TargetDestination.OTHER));
        }

        if (invoice != null) {
            block.setDispatchInvoiceId(invoice.getId());
        }

        Block saved = blockRepository.save(block);
        stockMovementService.recordBlockDispatch(saved, fromLoc, toLoc, targetDestination, customer, invoice, notes);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Block> searchQuarryBlocks(String search, BlockStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return blockRepository.searchBlocks(search, null, status, false, pageable);
    }

    @Transactional(readOnly = true)
    public List<Quarry> getAllQuarries() {
        return BoundedReads.all(quarryRepository);
    }

    /**
     * Same as {@code createQuarryBlock}, copied into a view inside the transaction.
     *
     * @param quarryId see {@code createQuarryBlock}
     * @param blockCode see {@code createQuarryBlock}
     * @param extractionDate see {@code createQuarryBlock}
     * @param widthCm see {@code createQuarryBlock}
     * @param lengthCm see {@code createQuarryBlock}
     * @param heightCm see {@code createQuarryBlock}
     * @param estimatedTonnage see {@code createQuarryBlock}
     * @param actualTonnage see {@code createQuarryBlock}
     * @param stoneType see {@code createQuarryBlock}
     * @param colorTone see {@code createQuarryBlock}
     * @param qualityGrade see {@code createQuarryBlock}
     * @param crackLevel see {@code createQuarryBlock}
     * @param quarrySection see {@code createQuarryBlock}
     * @param notes see {@code createQuarryBlock}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.BlockView createQuarryBlockView(Long quarryId, String blockCode, LocalDate extractionDate,
                                   int widthCm, int lengthCm, int heightCm,
                                   BigDecimal estimatedTonnage, BigDecimal actualTonnage,
                                   String stoneType, String colorTone, QualityGrade qualityGrade,
                                   int crackLevel, String quarrySection, String notes) {
        return blockMapper.toDto(createQuarryBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm, estimatedTonnage, actualTonnage, stoneType, colorTone, qualityGrade, crackLevel, quarrySection, notes));
    }

    /**
     * Same as {@code updateActualTonnage}, copied into a view inside the transaction.
     *
     * @param blockId see {@code updateActualTonnage}
     * @param actualTonnage see {@code updateActualTonnage}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.BlockView updateActualTonnageView(Long blockId, BigDecimal actualTonnage) {
        return blockMapper.toDto(updateActualTonnage(blockId, actualTonnage));
    }

    /**
     * Same as {@code dispatchBlock}, copied into a view inside the transaction.
     *
     * @param blockId see {@code dispatchBlock}
     * @param targetDestination see {@code dispatchBlock}
     * @param customerId see {@code dispatchBlock}
     * @param invoiceId see {@code dispatchBlock}
     * @param notes see {@code dispatchBlock}
     * @return the view
     */
    @Transactional
    public com.ozerler.marble.dto.view.BlockView dispatchBlockView(Long blockId, TargetDestination targetDestination, Long customerId, Long invoiceId, String notes) {
        return blockMapper.toDto(dispatchBlock(blockId, targetDestination, customerId, invoiceId, notes));
    }

    /**
     * Builds grid rows inside the read transaction (moved from the controller unchanged).
     *
     * @return grid rows
     */
    @Transactional(readOnly = true)
    public com.ozerler.marble.dto.TabulatorResponse<java.util.Map<String, Object>> quarryBlockRows(String search, BlockStatus status, int page, int size) {
        int pageIndex = Math.max(0, page - 1);
        Page<Block> blockPage = searchQuarryBlocks(search, status, pageIndex, size);

        List<java.util.Map<String, Object>> data = blockPage.getContent().stream().map(b -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            map.put("blockCode", b.getBlockCode());
            Object name;
            if (b.getQuarry() != null) {
                name = b.getQuarry().getName();
            } else {
                name = "—";
            }
            map.put("quarryName", name);
            map.put("quarryLocation", Objects.requireNonNullElse(b.getQuarrySection(), "—"));
            map.put("stoneType", Objects.requireNonNullElse(b.getStoneType(), "—"));
            map.put("colorQuality", Objects.requireNonNullElse(b.getColorQuality(), "—"));
            map.put("dimensions", Objects.requireNonNullElse(b.getDimensions(), "—"));
            map.put("approximateTonnage", Objects.requireNonNullElse(b.getApproximateTonnage(), BigDecimal.ZERO));
            map.put("actualTonnage", b.getActualTonnage());
            Object value;
            if (b.getExtractionDate() != null) {
                value = b.getExtractionDate().toString();
            } else {
                value = "";
            }
            map.put("extractionDate", value);
            Object valueValue;
            if (b.getStatus() != null) {
                valueValue = b.getStatus().name();
            } else {
                valueValue = "";
            }
            map.put("status", valueValue);
            Object displayName;
            if (b.getStatus() != null) {
                displayName = b.getStatus().getDisplayName();
            } else {
                displayName = "";
            }
            map.put("statusLabel", displayName);
            map.put("notes", Objects.requireNonNullElse(b.getNotes(), ""));
            return map;
        }).toList();

        return com.ozerler.marble.dto.TabulatorResponse.of(data, blockPage.getTotalPages(), blockPage.getTotalElements());
    }

    /**
     * Same as {@code getAllQuarries}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<com.ozerler.marble.dto.view.QuarryRef> getAllQuarryViews() {
        return getAllQuarries().stream().map(quarryMapper::toDto).toList();
    }

    /**
     * Same as {@code searchQuarryBlocks}, copied into a view inside the transaction.
     *
     * @param search see {@code searchQuarryBlocks}
     * @param status see {@code searchQuarryBlocks}
     * @param page see {@code searchQuarryBlocks}
     * @param size see {@code searchQuarryBlocks}
     * @return the view
     */
    @Transactional(readOnly = true)
    public Page<com.ozerler.marble.dto.view.BlockView> searchQuarryBlockViews(String search, BlockStatus status, int page, int size) {
        return searchQuarryBlocks(search, status, page, size).map(blockViews::toView);
    }
}
