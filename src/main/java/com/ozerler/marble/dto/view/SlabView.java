package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SlabView extends AuditView {
    private Long id;
    private String slabCode;
    private String stoneType;
    private BigDecimal thicknessCm;
    private BigDecimal widthCm;
    private BigDecimal lengthCm;
    private BigDecimal surfaceAreaM2;
    private BigDecimal costPerM2;
    private Integer glossLevel;
    private QualityGrade qualityGrade;
    private SurfaceFinish surfaceFinish;
    private SlabStatus status;
    private PartyView customer;
    private BlockView block;
    private PalletRef pallet;
    private OrderRef productionOrder;
}
