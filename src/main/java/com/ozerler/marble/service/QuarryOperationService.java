package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.model.enums.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.util.MessageUtils;
import com.ozerler.marble.util.UniqueCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarryOperationService {

    private final BlockRepository blockRepository;
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

        String code = (blockCode != null && !blockCode.isBlank())
                ? blockCode.trim().toUpperCase()
                : UniqueCodes.yearly("BLK", blockRepository::existsByBlockCode);

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

        Block block = Block.builder()
                .quarry(quarry)
                .blockCode(code)
                .quarrySection(quarrySection != null && !quarrySection.isBlank() ? quarrySection.trim() : "A1")
                .extractionDate(extractionDate != null ? extractionDate : LocalDate.now())
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .heightCm(heightCm)
                .estimatedTonnage(est)
                .actualTonnage(act)
                .actualWeightKg(act != null && act.compareTo(BigDecimal.ZERO) > 0 ? act.multiply(BigDecimal.valueOf(1000)) : BigDecimal.ZERO)
                .stoneType(stoneType != null && !stoneType.isBlank() ? stoneType.trim() : quarry.getName())
                .colorTone(colorTone)
                .qualityGrade(qualityGrade != null ? qualityGrade : QualityGrade.A)
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
        block.calculateMetrics(block.getQuarry() != null ? block.getQuarry().getSpecificGravity() : new BigDecimal("2.70"));
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
            block.setTargetDestination(targetDestination != null ? targetDestination : TargetDestination.OTHER);
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
        return quarryRepository.findAll();
    }
}
