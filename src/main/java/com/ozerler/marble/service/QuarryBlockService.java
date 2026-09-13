package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.QuarryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuarryBlockService {

    private final BlockRepository blockRepository;
    private final QuarryRepository quarryRepository;
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
    public TabulatorResponse<BlockDto> getBlocksPaged(int page, int size, String search, String sortField, String sortDir) {
        String sortProperty = (sortField == null || sortField.isBlank() || "createdAt".equalsIgnoreCase(sortField))
                ? "createdDate" : sortField;
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(dir, sortProperty);

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : Constants.DEFAULT_PAGE_SIZE, sort);

        Page<Block> blockPage = blockRepository.searchBlocks(search, pageable);
        List<BlockDto> dtos = blockPage.getContent().stream()
                .map(BlockDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, blockPage.getTotalPages(), blockPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Block getBlockById(Long id) {
        Objects.requireNonNull(id, getMessage("error.block.id.required"));
        return blockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.block.not_found", id)));
    }

    @Transactional
    public Block registerBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                               int widthCm, int lengthCm, int heightCm,
                               BigDecimal actualWeightKg, String stoneType, String colorTone,
                               QualityGrade qualityGrade, int crackLevel,
                               BigDecimal extractionCost, String notes, String photoUrls) {

        Objects.requireNonNull(quarryId, getMessage("error.quarry.id.required"));
        Objects.requireNonNull(blockCode, getMessage("error.block.code.required"));

        Quarry quarry = quarryRepository.findById(quarryId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.quarry.not_found", quarryId)));

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
                .status(BlockStatus.QUARRY)
                .extractionCost(extractionCost != null ? extractionCost : BigDecimal.ZERO)
                .transportCost(BigDecimal.ZERO)
                .notes(notes)
                .photoUrls(photoUrls)
                .build();

        block.calculateMetrics(quarry.getSpecificGravity());
        return blockRepository.save(block);
    }

    @Transactional
    public Block updateBlock(Long id, Long quarryId, String blockCode, LocalDate extractionDate,
                             int widthCm, int lengthCm, int heightCm,
                             BigDecimal actualWeightKg, String stoneType, String colorTone,
                             QualityGrade qualityGrade, int crackLevel,
                             BigDecimal extractionCost, String notes, String photoUrls) {
        Block block = getBlockById(id);
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
        block.calculateMetrics(block.getQuarry().getSpecificGravity());
        return blockRepository.save(block);
    }

    @Transactional
    public Block transferToFactory(Long blockId, BigDecimal transportCost) {
        Block block = getBlockById(blockId);
        block.setStatus(BlockStatus.FACTORY_STOCK);
        block.setTransportCost(transportCost != null ? transportCost : BigDecimal.ZERO);
        block.calculateMetrics(block.getQuarry().getSpecificGravity());
        return blockRepository.save(block);
    }

    @Transactional
    public Block sellBlockExternally(Long blockId) {
        Block block = getBlockById(blockId);
        block.setStatus(BlockStatus.SOLD);
        return blockRepository.save(block);
    }

    @org.springframework.cache.annotation.Cacheable("quarries")
    @Transactional(readOnly = true)
    public List<Quarry> getAllQuarries() {
        return quarryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Block> getAvailableBlocksForProduction() {
        return blockRepository.findByStatus(BlockStatus.FACTORY_STOCK);
    }
}
