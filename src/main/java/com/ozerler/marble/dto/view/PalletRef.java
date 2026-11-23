package com.ozerler.marble.dto.view;

import java.math.BigDecimal;

import com.ozerler.marble.model.enums.PackagingType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PalletRef {
    private Long id;
    private String palletCode;
    private String warehouseLocation;
    private PackagingType packagingType;
    private BigDecimal grossWeightKg;
}
