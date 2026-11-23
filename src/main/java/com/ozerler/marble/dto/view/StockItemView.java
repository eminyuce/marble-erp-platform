package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

import com.ozerler.marble.model.enums.SurfaceFinish;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockItemView {
    private Long id;
    private String itemCode;
    private String productCode;
    private String description;
    private String stoneType;
    private String status;
    private String unit;
    private String edgeFinish;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal actualProducedQuantity;
    private java.time.LocalDate productionDate;
    private java.time.LocalDateTime createdDate;
    private BlockRef sourceBlock;
    private BlockView.LocationView stockLocation;
    private String notes;
    private com.ozerler.marble.model.enums.QuarryCategory quarryCategory;
    private com.ozerler.marble.model.enums.StockProductType productType;
    private Boolean directExpense;
    private BigDecimal thicknessCm;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
    private Integer pieceCount;
    private SurfaceFinish surfaceFinish;
    private PartyView customer;
}
