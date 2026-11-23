package com.ozerler.marble.dto;

import com.ozerler.marble.dto.view.SlabView;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SlabLabelDto {
    private final SlabView slab;
    private final String qrCodeBase64;
    private final String blockCode;
    private final String stoneType;
    private final String quarryName;
}
