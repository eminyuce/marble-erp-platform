package com.ozerler.marble.web;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.SlabView;
import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.service.FactoryStockService;

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
            Object blockCode;
            if (s.getBlock() != null) {
                blockCode = s.getBlock().getBlockCode();
            } else {
                blockCode = "—";
            }
            map.put("sourceBlockCode", blockCode);
            Object stoneType;
            if (s.getBlock() != null && s.getBlock().getStoneType() != null) {
                stoneType = s.getBlock().getStoneType();
            } else {
                stoneType = "—";
            }
            map.put("stoneType", stoneType);
            Object displayName;
            if (s.getQualityGrade() != null) {
                displayName = s.getQualityGrade().getDisplayName();
            } else {
                displayName = "1. Sınıf";
            }
            map.put("qualityGrade", displayName);
            Object displayNameValue;
            if (s.getSurfaceFinish() != null) {
                displayNameValue = s.getSurfaceFinish().getDisplayName();
            } else {
                displayNameValue = "Cilalı";
            }
            map.put("surfaceFinish", displayNameValue);
            Object value;
            if (s.getThicknessCm() != null) {
                value = s.getThicknessCm() + " cm";
            } else {
                value = "2 cm";
            }
            map.put("thicknessCm", value);
            Object valueValue;
            if (s.getWidthCm() != null && s.getLengthCm() != null) {
                valueValue = s.getWidthCm() + "×" + s.getLengthCm() + " cm";
            } else {
                valueValue = "—";
            }
            map.put("dimensions", valueValue);
            map.put("surfaceAreaM2", s.getSurfaceAreaM2());
            map.put("pieceCount", 1);
            Object companyName;
            if (s.getCustomer() != null) {
                companyName = s.getCustomer().getCompanyName();
            } else {
                companyName = null;
            }
            map.put("customerName", companyName);
            Long id;
            if (s.getCustomer() != null) {
                id = s.getCustomer().getId();
            } else {
                id = null;
            }
            map.put("customerId", id);
            map.put("isGeneralStock", s.getCustomer() == null);
            Object valueValue2;
            if (s.getStatus() != null) {
                valueValue2 = s.getStatus().name();
            } else {
                valueValue2 = "AVAILABLE";
            }
            map.put("status", valueValue2);
            Object displayNameValue2;
            if (s.getStatus() != null) {
                displayNameValue2 = s.getStatus().getDisplayName();
            } else {
                displayNameValue2 = "Hazır";
            }
            map.put("statusLabel", displayNameValue2);
            Object valueValue3;
            if (s.getCreatedDate() != null) {
                valueValue3 = s.getCreatedDate().format(DATE_FORMAT);
            } else {
                valueValue3 = "—";
            }
            map.put("createdDate", valueValue3);
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
        map.put("edgeFinish", Objects.requireNonNullElse(it.getEdgeFinish(), "Düz"));
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
                // best effort: fall back to the default below
            }
        }
        map.put("customerName", customerName);
        map.put("customerId", custId);
        map.put("isGeneralStock", it.getCustomer() == null);
        Object value;
        if (it.getProductionDate() != null) {
            value = it.getProductionDate().format(DATE_FORMAT);
        } else {
            if (it.getCreatedDate() != null) {
                value = it.getCreatedDate().format(DATE_FORMAT);
            } else {
                value = "—";
            }
        }
        map.put("productionDate", value);
        map.put("status", Objects.requireNonNullElse(it.getStatus(), "AVAILABLE"));
        map.put("statusLabel", "Hazır / Stokta");
        map.put("notes", Objects.requireNonNullElse(it.getNotes(), ""));
        return map;
    }

    private static Map<String, Object> workshopSizedRow(StockItemView it) {
        Map<String, Object> map = new HashMap<>();
        putSizedIdentity(map, it);
        putSizedMeasurements(map, it, "m²");
        map.put("pieceCount", Objects.requireNonNullElse(it.getPieceCount(), "—"));
        String crateLoc = "—";
        if (it.getStockLocation() != null) {
            try {
                crateLoc = it.getStockLocation().getName();
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        map.put("crateNumber", crateLoc);
        Object companyName;
        if (it.getCustomer() != null) {
            companyName = it.getCustomer().getCompanyName();
        } else {
            companyName = null;
        }
        map.put("customerName", companyName);
        Long id;
        if (it.getCustomer() != null) {
            id = it.getCustomer().getId();
        } else {
            id = null;
        }
        map.put("customerId", id);
        map.put("isGeneralStock", it.getCustomer() == null);
        map.put("statusLabel", "Stokta");
        Object value;
        if (it.getCreatedDate() != null) {
            value = it.getCreatedDate().format(DATE_FORMAT);
        } else {
            value = "—";
        }
        map.put("createdDate", value);
        map.put("notes", Objects.requireNonNullElse(it.getNotes(), ""));
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
        map.put("sourceBlockCode", Objects.requireNonNullElse(sourceBlockCode, "—"));
        map.put("stoneType", Objects.requireNonNullElse(it.getStoneType(), "Mermer"));
        map.put("description", Objects.requireNonNullElse(it.getDescription(), "Ebatlı Mermer"));
        Object displayName;
        if (it.getSurfaceFinish() != null) {
            displayName = it.getSurfaceFinish().getDisplayName();
        } else {
            displayName = "Cilalı";
        }
        map.put("surfaceFinish", displayName);
    }

    private static void putSizedMeasurements(Map<String, Object> map, StockItemView it, String defaultUnit) {
        Object value;
        if (it.getThicknessCm() != null) {
            value = it.getThicknessCm() + " cm";
        } else {
            value = "2 cm";
        }
        map.put("thicknessCm", value);
        Object valueValue;
        if (it.getWidthCm() != null && it.getLengthCm() != null) {
            valueValue = it.getWidthCm() + "×" + it.getLengthCm() + " cm";
        } else {
            valueValue = "—";
        }
        map.put("dimensions", valueValue);
        map.put("quantity", it.getQuantity());
        Object unit;
        if (it.getUnit() != null) {
            unit = it.getUnit();
        } else {
            unit = defaultUnit;
        }
        map.put("unit", unit);
    }
}
