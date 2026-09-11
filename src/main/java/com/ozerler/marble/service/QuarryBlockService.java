package com.ozerler.marble.service;

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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuarryBlockService {

    private final BlockRepository blockRepository;
    private final QuarryRepository quarryRepository;

    @Transactional(readOnly = true)
    public TabulatorResponse<BlockDto> getBlocksPaged(int page, int size, String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : 10, sort);

        Page<Block> blockPage = blockRepository.searchBlocks(search, pageable);
        List<BlockDto> dtos = blockPage.getContent().stream()
                .map(BlockDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, blockPage.getTotalPages(), blockPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Block getBlockById(Long id) {
        return blockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + id));
    }

    @Transactional
    public Block registerBlock(Long quarryId, String blockCode, LocalDate extractionDate,
                              int widthCm, int lengthCm, int heightCm,
                              BigDecimal actualWeightKg, String stoneType, String colorTone,
                              QualityGrade qualityGrade, int crackLevel,
                              BigDecimal extractionCost, String notes, String photoUrls) {

        Quarry quarry = quarryRepository.findById(quarryId)
                .orElseThrow(() -> new IllegalArgumentException("Ocak bulunamadı: " + quarryId));

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

    @Transactional(readOnly = true)
    public List<Quarry> getAllQuarries() {
        return quarryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Block> getAvailableBlocksForProduction() {
        return blockRepository.findByStatus(BlockStatus.FACTORY_STOCK);
    }
}
