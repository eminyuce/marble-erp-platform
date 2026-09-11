package com.ozerler.marble.service;

import com.ozerler.marble.dto.GenealogyNodeDto;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.CutOrder;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GenealogyService {

    private static final int CRITICAL_CRACK_LEVEL_THRESHOLD = 1;
    private static final String CRITICAL_CRACK_WARNING = "Kritik Seviye İç Çatlak Riski Tespit Edildi!";

    private final BlockRepository blockRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final SlabRepository slabRepository;
    private final CutItemRepository cutItemRepository;

    /**
     * Builds full digital stone genealogy tree starting from Quarry & Block (BRD Section 1.3)
     */
    @Transactional(readOnly = true)
    public GenealogyNodeDto buildTreeForBlock(Long blockId) {
        Objects.requireNonNull(blockId, "Blok ID boş olamaz");
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));

        GenealogyNodeDto rootNode = buildBlockNode(block);

        List<ProductionOrder> orders = productionOrderRepository.findByBlockId(block.getId());
        for (ProductionOrder order : orders) {
            GenealogyNodeDto orderNode = buildProductionOrderNode(order);

            List<Slab> slabs = slabRepository.findByProductionOrderId(order.getId());
            for (Slab slab : slabs) {
                GenealogyNodeDto slabNode = buildSlabNode(slab);

                List<CutItem> items = cutItemRepository.findBySourceSlabId(slab.getId());
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
        boolean hasCriticalCrack = block.getCrackLevel() > CRITICAL_CRACK_LEVEL_THRESHOLD;
        String details = String.format("Ocak: %s | Ölçü: %dx%dx%d cm | Ağırlık: %,.0f kg | Kalite: %s",
                block.getQuarry().getName(), block.getWidthCm(), block.getLengthCm(), block.getHeightCm(),
                block.getActualWeightKg(), block.getQualityGrade());

        return GenealogyNodeDto.builder()
                .id("BLK-" + block.getId())
                .type("BLOCK")
                .title("Ham Blok: " + block.getBlockCode())
                .subtitle(block.getStoneType() + " (" + block.getColorTone() + ")")
                .details(details)
                .status(block.getStatus().getLabel())
                .qrCode("BLK-" + block.getBlockCode())
                .alert(hasCriticalCrack)
                .alertMessage(hasCriticalCrack ? CRITICAL_CRACK_WARNING : null)
                .children(new ArrayList<>())
                .build();
    }

    private GenealogyNodeDto buildProductionOrderNode(ProductionOrder order) {
        String details = String.format("Operatör: %s | Süre: %s sa | Elektrik: %s kWh",
                order.getOperatorName(), order.getDurationHours(), order.getElectricityKwh());

        return GenealogyNodeDto.builder()
                .id("PRD-" + order.getId())
                .type("PRODUCTION")
                .title("Fabrika Kesim: " + order.getOrderNo())
                .subtitle(order.getMachineName() + " (" + order.getProcessType().getLabel() + ")")
                .details(details)
                .status(order.getStatus())
                .qrCode(order.getOrderNo())
                .children(new ArrayList<>())
                .build();
    }

    private GenealogyNodeDto buildSlabNode(Slab slab) {
        String details = String.format("Ebat: %sx%s cm (%s m²) | Maliyet: %,.2f TL/m²",
                slab.getWidthCm(), slab.getLengthCm(), slab.getSurfaceAreaM2(), slab.getCostPerM2());
        boolean isGradeC = slab.getQualityGrade() == QualityGrade.C;

        return GenealogyNodeDto.builder()
                .id("SLB-" + slab.getId())
                .type("SLAB")
                .title("Plaka: " + slab.getSlabCode())
                .subtitle(slab.getSurfaceFinish().getLabel() + " - " + slab.getQualityGrade().getLabel())
                .details(details)
                .status(slab.getStatus().getLabel())
                .qrCode(slab.getSlabCode())
                .alert(isGradeC)
                .children(new ArrayList<>())
                .build();
    }

    private GenealogyNodeDto buildCutItemNode(CutItem item) {
        String details = String.format("%sx%sx%s cm (%s m²) | İşlem: %s | Birim Maliyet: %,.2f TL",
                item.getWidthCm(), item.getLengthCm(), item.getThicknessCm(), item.getAreaM2(),
                item.getEdgeFinish(), item.getUnitCost());

        return GenealogyNodeDto.builder()
                .id("ITM-" + item.getId())
                .type("ITEM")
                .title("Ebatlı Mamul: " + item.getItemCode())
                .subtitle("Hedef: " + item.getTargetLocation())
                .details(details)
                .status(item.getStatus())
                .qrCode(item.getItemCode())
                .build();
    }
}
