package com.ozerler.marble.web;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.SlabView;
import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.service.FactoryStockService;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tabulator rows shared by the factory and workshop stock yards.
 */
public final class StockYardResponses {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private StockYardResponses() {
    }

    public static Page<SlabView> slabPage(FactoryStockService factoryStockService, SlabStatus status, Long customerId,
                               boolean generalStockOnly, ListQuery query) {
        return factoryStockService.getFactorySlabViewsPaged(
                status, customerId, generalStockOnly, query.getSearch(), query.pageNumber() - 1, query.pageSize());
    }

    public static TabulatorResponse<Map<String, Object>> slabs(FactoryStockService factoryStockService, int page, int size,
                                                        String search, SlabStatus status, Long customerId,
                                                        boolean generalStockOnly) {
        int pageIndex = Math.max(0, page - 1);
        Page<SlabView> slabsPage = factoryStockService.getFactorySlabViewsPaged(
                status, customerId, generalStockOnly, search, pageIndex, size);
        List<Map<String, Object>> data = slabsPage.getContent().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("slabCode", s.getSlabCode());
            map.put("sourceBlockCode", s.getBlock() != null ? s.getBlock().getBlockCode() : "—");
            map.put("stoneType", s.getBlock() != null && s.getBlock().getStoneType() != null ? s.getBlock().getStoneType() : "—");
            map.put("qualityGrade", s.getQualityGrade() != null ? s.getQualityGrade().getDisplayName() : "1. Sınıf");
            map.put("surfaceFinish", s.getSurfaceFinish() != null ? s.getSurfaceFinish().getDisplayName() : "Cilalı");
            map.put("thicknessCm", s.getThicknessCm() != null ? s.getThicknessCm() + " cm" : "2 cm");
            map.put("dimensions", (s.getWidthCm() != null && s.getLengthCm() != null) ? (s.getWidthCm() + "×" + s.getLengthCm() + " cm") : "—");
            map.put("surfaceAreaM2", s.getSurfaceAreaM2());
            map.put("pieceCount", 1);
            map.put("customerName", s.getCustomer() != null ? s.getCustomer().getCompanyName() : null);
            map.put("customerId", s.getCustomer() != null ? s.getCustomer().getId() : null);
            map.put("isGeneralStock", s.getCustomer() == null);
            map.put("status", s.getStatus() != null ? s.getStatus().name() : "AVAILABLE");
            map.put("statusLabel", s.getStatus() != null ? s.getStatus().getDisplayName() : "Hazır");
            map.put("createdDate", s.getCreatedDate() != null ? s.getCreatedDate().format(DATE_FORMAT) : "—");
            map.put("notes", "");
            return map;
        }).collect(Collectors.toList());
        return TabulatorResponse.of(data, slabsPage.getTotalPages(), slabsPage.getTotalElements());
    }

    public static TabulatorResponse<Map<String, Object>> factorySized(Page<StockItemView> itemsPage) {
        return respond(itemsPage, item -> factorySizedRow(item));
    }

    public static TabulatorResponse<Map<String, Object>> workshopSized(Page<StockItemView> itemsPage) {
        return respond(itemsPage, item -> workshopSizedRow(item));
    }

    private static TabulatorResponse<Map<String, Object>> respond(Page<StockItemView> itemsPage,
                                                                  Function<StockItemView, Map<String, Object>> rowMapper) {
        List<Map<String, Object>> data = itemsPage.getContent().stream()
                .map(item -> rowMapper.apply(item))
                .collect(Collectors.toList());
        return TabulatorResponse.of(data, itemsPage.getTotalPages(), itemsPage.getTotalElements());
    }

    private static Map<String, Object> factorySizedRow(StockItemView it) {
        Map<String, Object> map = new HashMap<>();
        putSizedIdentity(map, it);
        map.put("edgeFinish", it.getEdgeFinish() != null ? it.getEdgeFinish() : "Düz");
        putSizedMeasurements(map, it, "m2");
        map.put("pieceCount", it.getPieceCount());
        map.put("actualProducedQuantity", it.getActualProducedQuantity());
        String customerName = null;
        Long custId = null;
        if (it.getCustomer() != null) {
            try {
                customerName = it.getCustomer().getCompanyName();
                custId = it.getCustomer().getId();
            } catch (Exception ignored) {
            }
        }
        map.put("customerName", customerName);
        map.put("customerId", custId);
        map.put("isGeneralStock", it.getCustomer() == null);
        map.put("productionDate", it.getProductionDate() != null ? it.getProductionDate().format(DATE_FORMAT) : it.getCreatedDate() != null ? it.getCreatedDate().format(DATE_FORMAT) : "—");
        map.put("status", it.getStatus() != null ? it.getStatus() : "AVAILABLE");
        map.put("statusLabel", "Hazır / Stokta");
        map.put("notes", it.getNotes() != null ? it.getNotes() : "");
        return map;
    }

    private static Map<String, Object> workshopSizedRow(StockItemView it) {
        Map<String, Object> map = new HashMap<>();
        putSizedIdentity(map, it);
        putSizedMeasurements(map, it, "m²");
        map.put("pieceCount", it.getPieceCount() != null ? it.getPieceCount() : "—");
        String crateLoc = "—";
        if (it.getStockLocation() != null) {
            try {
                crateLoc = it.getStockLocation().getName();
            } catch (Exception ignored) {
            }
        }
        map.put("crateNumber", crateLoc);
        map.put("customerName", it.getCustomer() != null ? it.getCustomer().getCompanyName() : null);
        map.put("customerId", it.getCustomer() != null ? it.getCustomer().getId() : null);
        map.put("isGeneralStock", it.getCustomer() == null);
        map.put("statusLabel", "Stokta");
        map.put("createdDate", it.getCreatedDate() != null ? it.getCreatedDate().format(DATE_FORMAT) : "—");
        map.put("notes", it.getNotes() != null ? it.getNotes() : "");
        return map;
    }

    private static void putSizedIdentity(Map<String, Object> map, StockItemView it) {
        map.put("id", it.getId());
        map.put("itemCode", it.getItemCode());
        String sourceBlockCode = "—";
        if (it.getSourceBlock() != null) {
            try {
                sourceBlockCode = it.getSourceBlock().getBlockCode();
            } catch (Exception e) {
                sourceBlockCode = "—";
            }
        }
        map.put("sourceBlockCode", sourceBlockCode != null ? sourceBlockCode : "—");
        map.put("stoneType", it.getStoneType() != null ? it.getStoneType() : "Mermer");
        map.put("description", it.getDescription() != null ? it.getDescription() : "Ebatlı Mermer");
        map.put("surfaceFinish", it.getSurfaceFinish() != null ? it.getSurfaceFinish().getDisplayName() : "Cilalı");
    }

    private static void putSizedMeasurements(Map<String, Object> map, StockItemView it, String defaultUnit) {
        map.put("thicknessCm", it.getThicknessCm() != null ? it.getThicknessCm() + " cm" : "2 cm");
        map.put("dimensions", (it.getWidthCm() != null && it.getLengthCm() != null) ? (it.getWidthCm() + "×" + it.getLengthCm() + " cm") : "—");
        map.put("quantity", it.getQuantity());
        map.put("unit", it.getUnit() != null ? it.getUnit() : defaultUnit);
    }
}
