package com.ozerler.marble.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.GenealogyNodeDto;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.PalletItem;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.ShipmentItem;
import com.ozerler.marble.model.SiteInstallation;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.CutItemStatus;
import com.ozerler.marble.model.enums.OperationStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.PalletItemRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ShipmentItemRepository;
import com.ozerler.marble.repository.SiteInstallationRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.util.MessageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenealogyService {

    private final BlockRepository blockRepository;
    private final BlockViews blockViews;
    private final ProductionOrderRepository productionOrderRepository;
    private final SlabRepository slabRepository;
    private final CutItemRepository cutItemRepository;
    private final MaterialLotRepository materialLotRepository;
    private final PalletItemRepository palletItemRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final SiteInstallationRepository siteInstallationRepository;
    private final CostTransactionRepository costTransactionRepository;
    private final MessageSource messageSource;

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

        List<Long> slabIds = allSlabs.stream().map(slab -> slab.getId()).toList();
        Map<Long, List<CutItem>> itemsBySlabId;
        if (slabIds.isEmpty()) {
            itemsBySlabId = Collections.emptyMap();
        } else {
            itemsBySlabId = cutItemRepository.findBySourceSlabIdIn(slabIds).stream()
                    .filter(i -> i.getSourceSlab() != null)
                    .collect(Collectors.groupingBy(i -> i.getSourceSlab().getId()));
        }
        LotEventContext lotEvents = preloadLotEvents(slabIds);

        for (ProductionOrder order : orders) {
            GenealogyNodeDto orderNode = buildProductionOrderNode(order);

            List<Slab> slabs = slabsByOrderId.getOrDefault(order.getId(), Collections.emptyList());
            for (Slab slab : slabs) {
                GenealogyNodeDto slabNode = buildSlabNode(slab);

                List<CutItem> items = itemsBySlabId.getOrDefault(slab.getId(), Collections.emptyList());
                for (CutItem item : items) {
                    slabNode.getChildren().add(buildCutItemNode(item));
                }
                MaterialLot lot = lotEvents.lotBySlabId().get(slab.getId());
                if (lot != null) {
                    attachLotEvents(slabNode, lot, lotEvents);
                }

                orderNode.getChildren().add(slabNode);
            }

            rootNode.getChildren().add(orderNode);
        }

        for (CostTransaction tx : costTransactionRepository.findByBlockId(block.getId())) {
            rootNode.getChildren().add(buildCostNode(tx));
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

    private LotEventContext preloadLotEvents(List<Long> slabIds) {
        if (slabIds == null || slabIds.isEmpty()) {
            return LotEventContext.empty();
        }
        Map<Long, MaterialLot> lotBySlabId = materialLotRepository.findBySlabIdIn(slabIds).stream()
                .filter(lot -> lot.getSlab() != null)
                .collect(Collectors.toMap(lot -> lot.getSlab().getId(), lot -> lot, (left, right) -> left));
        if (lotBySlabId.isEmpty()) {
            return new LotEventContext(lotBySlabId, Map.of(), Map.of(), Map.of());
        }

        List<Long> lotIds = lotBySlabId.values().stream().map(lot -> lot.getId()).toList();
        Map<Long, List<PalletItem>> palletItemsByLotId = palletItemRepository.findByMaterialLotIdIn(lotIds).stream()
                .collect(Collectors.groupingBy(item -> item.getMaterialLot().getId()));

        List<Long> palletIds = palletItemsByLotId.values().stream()
                .flatMap(list -> list.stream())
                .map(item -> item.getPallet().getId())
                .distinct()
                .toList();
        Map<Long, List<ShipmentItem>> shipmentItemsByPalletId;
        if (palletIds.isEmpty()) {
            shipmentItemsByPalletId = Map.of();
        } else {
            shipmentItemsByPalletId = shipmentItemRepository.findByPalletIdIn(palletIds).stream()
                    .filter(item -> item.getPallet() != null)
                    .collect(Collectors.groupingBy(item -> item.getPallet().getId()));
        }

        Map<Long, List<SiteInstallation>> installationsByLotId =
                siteInstallationRepository.findByMaterialLotIdIn(lotIds).stream()
                        .filter(installation -> installation.getMaterialLot() != null)
                        .collect(Collectors.groupingBy(installation -> installation.getMaterialLot().getId()));

        return new LotEventContext(lotBySlabId, palletItemsByLotId, shipmentItemsByPalletId, installationsByLotId);
    }

    private void attachLotEvents(GenealogyNodeDto parent, MaterialLot lot, LotEventContext lotEvents) {
        for (PalletItem palletItem : lotEvents.palletItemsByLotId().getOrDefault(lot.getId(), List.of())) {
            GenealogyNodeDto palletNode =
                    GenealogyNodeDto.builder()
                            .id("PAL-" + palletItem.getPallet().getId())
                            .type("PALLET")
                            .title(
                                    getMessage(
                                            "genealogy.node.pallet.title",
                                            palletItem.getPallet().getPalletCode()))
                            .subtitle(palletItem.getPallet().getStatusLabel())
                            .details(
                                    getMessage(
                                            "genealogy.node.pallet.details",
                                            palletItem.getQuantity(),
                                            palletItem.getAreaM2()))
                            .status(palletItem.getPallet().getStatusLabel())
                            .qrCode(palletItem.getPallet().getPalletCode())
                            .children(new ArrayList<>())
                            .build();
            for (ShipmentItem shipmentItem : lotEvents.shipmentItemsByPalletId()
                    .getOrDefault(palletItem.getPallet().getId(), List.of())) {
                palletNode.getChildren().add(GenealogyNodeDto.builder()
                        .id("SHP-" + shipmentItem.getShipment().getId())
                        .type("SHIPMENT")
                        .title(getMessage("genealogy.node.shipment.title", shipmentItem.getShipment().getWaybillNo()))
                        .subtitle(shipmentItem.getShipment().getDeliveryStatusLabel())
                        .details(getMessage("genealogy.node.shipment.details",
                                shipmentItem.getShipment().getVehiclePlate(),
                                shipmentItem.getShipment().getDriverName()))
                        .status(shipmentItem.getShipment().getDeliveryStatusLabel())
                        .qrCode(shipmentItem.getShipment().getWaybillNo())
                        .children(new ArrayList<>())
                        .build());
            }
            parent.getChildren().add(palletNode);
        }
        for (SiteInstallation installation : lotEvents.installationsByLotId().getOrDefault(lot.getId(), List.of())) {
            String subtitle;
            if (installation.getLocation() != null) {
                subtitle = installation.getLocation().getLocationName();
            } else {
                subtitle = "";
            }
            parent.getChildren().add(GenealogyNodeDto.builder()
                    .id("INS-" + installation.getId())
                    .type("SITE")
                    .title(getMessage("genealogy.node.site.title", installation.getProject().getName()))
                    .subtitle(subtitle)
                    .details(getMessage("genealogy.node.site.details",
                            installation.getInstalledAreaM2(), installation.getWasteAreaM2()))
                    .status(installation.getCrewName())
                    .children(new ArrayList<>())
                    .build());
        }
    }

    private record LotEventContext(
            Map<Long, MaterialLot> lotBySlabId,
            Map<Long, List<PalletItem>> palletItemsByLotId,
            Map<Long, List<ShipmentItem>> shipmentItemsByPalletId,
            Map<Long, List<SiteInstallation>> installationsByLotId) {

        static LotEventContext empty() {
            return new LotEventContext(Map.of(), Map.of(), Map.of(), Map.of());
        }
    }

    private GenealogyNodeDto buildCostNode(CostTransaction tx) {
        String status;
        if (tx.getAmount() != null) {
            status = tx.getAmount().toPlainString();
        } else {
            status = "0";
        }
        return GenealogyNodeDto.builder()
                .id("CST-" + tx.getId())
                .type("COST")
                .title(getMessage("genealogy.node.cost.title", tx.getExpenseType().getLabel()))
                .subtitle(tx.getExpensePeriod())
                .details(tx.getDescription())
                .status(status)
                .children(new ArrayList<>())
                .build();
    }

    private GenealogyNodeDto buildBlockNode(Block block) {
        boolean hasCriticalCrack = block.getCrackLevel() > Constants.CRITICAL_CRACK_LEVEL_THRESHOLD;
        String details = getMessage("genealogy.node.block.details",
                block.getQuarry().getName(), block.getWidthCm(), block.getLengthCm(), block.getHeightCm(),
                block.getActualWeightKg(), block.getQualityGrade().getLabel());

        String alertMessage;
        if (hasCriticalCrack) {
            alertMessage = getMessage("genealogy.node.block.crack_warning");
        } else {
            alertMessage = null;
        }
        return GenealogyNodeDto.builder()
                .id("BLK-" + block.getId())
                .type("BLOCK")
                .title(getMessage("genealogy.node.block.title", block.getBlockCode()))
                .subtitle(block.getStoneType() + " (" + block.getColorTone() + ")")
                .details(details)
                .status(block.getStatus().getLabel())
                .qrCode("BLK-" + block.getBlockCode())
                .alert(hasCriticalCrack)
                .alertMessage(alertMessage)
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
                .status(OperationStatus.labelOf(order.getStatus()))
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
                .status(CutItemStatus.labelOf(item.getStatus()))
                .qrCode(item.getItemCode())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Block> getAllBlocks() {
        return BoundedReads.all(blockRepository);
    }

    @Transactional(readOnly = true)
    public Optional<GenealogyNodeDto> getDefaultTree() {
        return blockRepository.findTop1ByOrderByIdAsc()
                .map(block -> buildTreeForBlock(block.getId()));
    }

    /**
     * Same as {@code getAllBlocks}, copied into a view inside the transaction.
     *
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<BlockView> getAllBlockViews() {
        return getAllBlocks().stream().map(blockViews::toView).toList();
    }
}
