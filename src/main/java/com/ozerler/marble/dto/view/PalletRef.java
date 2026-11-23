package com.ozerler.marble.dto.view;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PalletRef {
    private Long id;
    private String palletCode;
    private String warehouseLocation;
    private com.ozerler.marble.model.enums.PackagingType packagingType;
    private java.math.BigDecimal grossWeightKg;
}
