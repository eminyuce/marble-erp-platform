package com.ozerler.marble.dto;

import com.ozerler.marble.model.Slab;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SlabLabelDto {
    private final Slab slab;
    private final String qrCodeBase64;
    private final String blockCode;
    private final String stoneType;
    private final String quarryName;
}
