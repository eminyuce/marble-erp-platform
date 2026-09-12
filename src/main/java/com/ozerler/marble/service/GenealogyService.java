package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.GenealogyNodeDto;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenealogyService {

    private final BlockRepository blockRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final SlabRepository slabRepository;
    private final CutItemRepository cutItemRepository;
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

    /**
     * Builds full digital stone genealogy tree starting from Quarry & Block (BRD Section 1.3)
     */
    @Transactional(readOnly = true)
    public GenealogyNodeDto buildTreeForBlock(Long blockId) {
        Objects.requireNonNull(blockId, getMessage("error.block.id.required"));
        Block block = blockRepository.findByIdWithQuarry(blockId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.block.not_found", blockId)));

        GenealogyNodeDto rootNode = buildBlockNode(block);

        List<ProductionOrder> orders = productionOrderRepository.findByBlockId(block.getId());
        if (orders.isEmpty()) {
            return rootNode;
        }

        List<Slab> allSlabs = slabRepository.findByBlockId(block.getId());
        Map<Long, List<Slab>> slabsByOrderId = allSlabs.stream()
                .filter(s -> s.getProductionOrder() != null)
                .collect(Collectors.groupingBy(s -> s.getProductionOrder().getId()));

        List<Long> slabIds = allSlabs.stream().map(Slab::getId).toList();
        Map<Long, List<CutItem>> itemsBySlabId = slabIds.isEmpty()
                ? Collections.emptyMap()
                : cutItemRepository.findBySourceSlabIdIn(slabIds).stream()
                        .filter(i -> i.getSourceSlab() != null)
                        .collect(Collectors.groupingBy(i -> i.getSourceSlab().getId()));

        for (ProductionOrder order : orders) {
            GenealogyNodeDto orderNode = buildProductionOrderNode(order);

            List<Slab> slabs = slabsByOrderId.getOrDefault(order.getId(), Collections.emptyList());
            for (Slab slab : slabs) {
                GenealogyNodeDto slabNode = buildSlabNode(slab);

                List<CutItem> items = itemsBySlabId.getOrDefault(slab.getId(), Collections.emptyList());
                for (CutItem item : items) {
                    slabNode.getChildren().add(buildCutItemNode(item));
                }

                orderNode.getChildren().add(slabNode);
            }

            rootNode.getChildren().add(orderNode);
        }

        return rootNode;
    }

    /**
     * Reverse traceback analysis from a finished item or slab code back to the block (BRD Section 8.2)
     */
    @Transactional(readOnly = true)
    public Optional<GenealogyNodeDto> traceBack(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String normalizedCode = code.trim().toUpperCase();

        Optional<CutItem> itemOpt = cutItemRepository.findByItemCode(normalizedCode);
        if (itemOpt.isPresent()) {
            Block block = itemOpt.get().getSourceSlab().getBlock();
            return Optional.of(buildTreeForBlock(block.getId()));
        }

        Optional<Slab> slabOpt = slabRepository.findBySlabCode(normalizedCode);
        if (slabOpt.isPresent()) {
            return Optional.of(buildTreeForBlock(slabOpt.get().getBlock().getId()));
        }

        Optional<Block> blockOpt = blockRepository.findByBlockCode(normalizedCode);
        if (blockOpt.isPresent()) {
            return Optional.of(buildTreeForBlock(blockOpt.get().getId()));
        }

        return Optional.empty();
    }

    private GenealogyNodeDto buildBlockNode(Block block) {
        boolean hasCriticalCrack = block.getCrackLevel() > Constants.CRITICAL_CRACK_LEVEL_THRESHOLD;
        String details = getMessage("genealogy.node.block.details",
                block.getQuarry().getName(), block.getWidthCm(), block.getLengthCm(), block.getHeightCm(),
                block.getActualWeightKg(), block.getQualityGrade().getLabel());

        return GenealogyNodeDto.builder()
                .id("BLK-" + block.getId())
                .type("BLOCK")
                .title(getMessage("genealogy.node.block.title", block.getBlockCode()))
                .subtitle(block.getStoneType() + " (" + block.getColorTone() + ")")
                .details(details)
                .status(block.getStatus().getLabel())
                .qrCode("BLK-" + block.getBlockCode())
                .alert(hasCriticalCrack)
                .alertMessage(hasCriticalCrack ? getMessage("genealogy.node.block.crack_warning") : null)
                .children(new ArrayList<>())
                .build();
    }

    private GenealogyNodeDto buildProductionOrderNode(ProductionOrder order) {
        String details = getMessage("genealogy.node.production.details",
                order.getOperatorName(), order.getDurationHours(), order.getElectricityKwh());

        return GenealogyNodeDto.builder()
                .id("PRD-" + order.getId())
                .type("PRODUCTION")
                .title(getMessage("genealogy.node.production.title", order.getOrderNo()))
                .subtitle(order.getMachineName() + " (" + order.getProcessType().getLabel() + ")")
                .details(details)
                .status(order.getStatus())
                .qrCode(order.getOrderNo())
                .children(new ArrayList<>())
                .build();
    }

    private GenealogyNodeDto buildSlabNode(Slab slab) {
        String details = getMessage("genealogy.node.slab.details",
                slab.getWidthCm(), slab.getLengthCm(), slab.getSurfaceAreaM2(), slab.getCostPerM2());
        boolean isGradeC = slab.getQualityGrade() == QualityGrade.C;

        return GenealogyNodeDto.builder()
                .id("SLB-" + slab.getId())
                .type("SLAB")
                .title(getMessage("genealogy.node.slab.title", slab.getSlabCode()))
                .subtitle(slab.getSurfaceFinish().getLabel() + " - " + slab.getQualityGrade().getLabel())
                .details(details)
                .status(slab.getStatus().getLabel())
                .qrCode(slab.getSlabCode())
                .alert(isGradeC)
                .children(new ArrayList<>())
                .build();
    }

    private GenealogyNodeDto buildCutItemNode(CutItem item) {
        String details = getMessage("genealogy.node.item.details",
                item.getWidthCm(), item.getLengthCm(), item.getThicknessCm(), item.getAreaM2(),
                item.getEdgeFinish(), item.getUnitCost());

        return GenealogyNodeDto.builder()
                .id("ITM-" + item.getId())
                .type("ITEM")
                .title(getMessage("genealogy.node.item.title", item.getItemCode()))
                .subtitle(getMessage("genealogy.node.item.target", item.getTargetLocation()))
                .details(details)
                .status(item.getStatus())
                .qrCode(item.getItemCode())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Block> getAllBlocks() {
        return blockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<GenealogyNodeDto> getDefaultTree() {
        return blockRepository.findAll().stream().findFirst()
                .map(block -> buildTreeForBlock(block.getId()));
    }
}
