package com.ozerler.marble.service;

import com.ozerler.marble.dto.GenealogyNodeDto;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GenealogyService {

    private final BlockRepository blockRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final SlabRepository slabRepository;
    private final CutItemRepository cutItemRepository;

    /**
     * Builds full digital stone genealogy tree starting from Quarry & Block (BRD Section 1.3)
     */
    @Transactional(readOnly = true)
    public GenealogyNodeDto buildTreeForBlock(Long blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("Blok bulunamadı: " + blockId));

        GenealogyNodeDto root = GenealogyNodeDto.builder()
                .id("BLK-" + block.getId())
                .type("BLOCK")
                .title("Ham Blok: " + block.getBlockCode())
                .subtitle(block.getStoneType() + " (" + block.getColorTone() + ")")
                .details(String.format("Ocak: %s | Ölçü: %dx%dx%d cm | Ağırlık: %,.0f kg | Kalite: %s",
                        block.getQuarry().getName(), block.getWidthCm(), block.getLengthCm(), block.getHeightCm(),
                        block.getActualWeightKg(), block.getQualityGrade()))
                .status(block.getStatus().getLabel())
                .qrCode("BLK-" + block.getBlockCode())
                .alert(block.getCrackLevel() > 1)
                .alertMessage(block.getCrackLevel() > 1 ? "Kritik Seviye İç Çatlak Riski Tespit Edildi!" : null)
                .children(new ArrayList<>())
                .build();

        List<ProductionOrder> orders = productionOrderRepository.findByBlockId(block.getId());
        for (ProductionOrder order : orders) {
            GenealogyNodeDto orderNode = GenealogyNodeDto.builder()
                    .id("PRD-" + order.getId())
                    .type("PRODUCTION")
                    .title("Fabrika Kesim: " + order.getOrderNo())
                    .subtitle(order.getMachineName() + " (" + order.getProcessType().getLabel() + ")")
                    .details(String.format("Operatör: %s | Süre: %s sa | Elektrik: %s kWh",
                            order.getOperatorName(), order.getDurationHours(), order.getElectricityKwh()))
                    .status(order.getStatus())
                    .qrCode(order.getOrderNo())
                    .children(new ArrayList<>())
                    .build();

            List<Slab> slabs = slabRepository.findByProductionOrderId(order.getId());
            for (Slab slab : slabs) {
                GenealogyNodeDto slabNode = GenealogyNodeDto.builder()
                        .id("SLB-" + slab.getId())
                        .type("SLAB")
                        .title("Plaka: " + slab.getSlabCode())
                        .subtitle(slab.getSurfaceFinish().getLabel() + " - " + slab.getQualityGrade().getLabel())
                        .details(String.format("Ebat: %sx%s cm (%s m²) | Maliyet: %,.2f TL/m²",
                                slab.getWidthCm(), slab.getLengthCm(), slab.getSurfaceAreaM2(), slab.getCostPerM2()))
                        .status(slab.getStatus().getLabel())
                        .qrCode(slab.getSlabCode())
                        .alert(slab.getQualityGrade().name().equals("C"))
                        .children(new ArrayList<>())
                        .build();

                List<CutItem> items = cutItemRepository.findBySourceSlabId(slab.getId());
                for (CutItem item : items) {
                    GenealogyNodeDto itemNode = GenealogyNodeDto.builder()
                            .id("ITM-" + item.getId())
                            .type("ITEM")
                            .title("Ebatlı Mamul: " + item.getItemCode())
                            .subtitle("Hedef: " + item.getTargetLocation())
                            .details(String.format("%sx%sx%s cm (%s m²) | İşlem: %s | Birim Maliyet: %,.2f TL",
                                    item.getWidthCm(), item.getLengthCm(), item.getThicknessCm(), item.getAreaM2(),
                                    item.getEdgeFinish(), item.getUnitCost()))
                            .status(item.getStatus())
                            .qrCode(item.getItemCode())
                            .build();

                    slabNode.getChildren().add(itemNode);
                }

                orderNode.getChildren().add(slabNode);
            }

            root.getChildren().add(orderNode);
        }

        return root;
    }

    /**
     * Reverse traceback analysis from a finished item or slab code back to the block (BRD Section 8.2)
     */
    @Transactional(readOnly = true)
    public Optional<GenealogyNodeDto> traceBack(String code) {
        if (code == null || code.isBlank()) return Optional.empty();

        String trimmed = code.trim().toUpperCase();

        // Check if code is item
        Optional<CutItem> itemOpt = cutItemRepository.findByItemCode(trimmed);
        if (itemOpt.isPresent()) {
            CutItem item = itemOpt.get();
            Block block = item.getSourceSlab().getBlock();
            return Optional.of(buildTreeForBlock(block.getId()));
        }

        // Check if code is slab
        Optional<Slab> slabOpt = slabRepository.findBySlabCode(trimmed);
        if (slabOpt.isPresent()) {
            return Optional.of(buildTreeForBlock(slabOpt.get().getBlock().getId()));
        }

        // Check if code is block
        Optional<Block> blockOpt = blockRepository.findByBlockCode(trimmed);
        if (blockOpt.isPresent()) {
            return Optional.of(buildTreeForBlock(blockOpt.get().getId()));
        }

        return Optional.empty();
    }
}
