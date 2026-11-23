package com.ozerler.marble.dto.view;

import com.ozerler.marble.model.enums.SurfaceFinish;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

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
    private BigDecimal thicknessCm;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
    private Integer pieceCount;
    private SurfaceFinish surfaceFinish;
    private PartyView customer;
}
