package com.ozerler.marble.dto.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.model.enums.StockProductType;
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
    private LocalDate productionDate;
    private LocalDateTime createdDate;
    private BlockRef sourceBlock;
    private BlockView.LocationView stockLocation;
    private String notes;
    private QuarryCategory quarryCategory;
    private StockProductType productType;
    private Boolean directExpense;
    private BigDecimal thicknessCm;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
    private Integer pieceCount;
    private SurfaceFinish surfaceFinish;
    private PartyView customer;
}
