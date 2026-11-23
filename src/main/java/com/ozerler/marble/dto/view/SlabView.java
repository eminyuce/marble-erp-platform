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

    public String getCustomerName() {
        return ViewLabels.customerName(customer);
    }

    public Long getCustomerId() {
        if (customer == null) {
            return null;
        }
        return customer.getId();
    }

    public String getDisplayStoneType() {
        return ViewLabels.displayStoneType(stoneType, block, ViewLabels.DEFAULT_STONE_TYPE);
    }

    public String getBlockQuarryName() {
        return ViewLabels.displayQuarryName(block);
    }

    public String getPrefillStoneType() {
        return ViewLabels.displayStoneType(stoneType, block, ViewLabels.DEFAULT_SLAB_DESCRIPTION);
    }

    public String getFactorySizedOptionLabel() {
        return sizedBaseLabel() + " | " + getCustomerName();
    }

    public String getWorkshopSizedOptionLabel() {
        return sizedBaseLabel() + " | Alan: " + nullSafe(surfaceAreaM2) + " m² | " + getCustomerName();
    }

    private String sizedBaseLabel() {
        return nullSafe(slabCode)
                + " | "
                + getDisplayStoneType()
                + " | "
                + nullSafe(thicknessCm)
                + " cm ("
                + nullSafe(widthCm)
                + "x"
                + nullSafe(lengthCm)
                + " cm)";
    }

    private static String nullSafe(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }
}

