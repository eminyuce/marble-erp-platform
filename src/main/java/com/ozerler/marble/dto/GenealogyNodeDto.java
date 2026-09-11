package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenealogyNodeDto {
    private String id;
    private String type; // QUARRY, BLOCK, PRODUCTION, SLAB, PALLET, WORKSHOP, ITEM, SITE
    private String title;
    private String subtitle;
    private String details;
    private String status;
    private String qrCode;
    private boolean alert;
    private String alertMessage;

    @Builder.Default
    private List<GenealogyNodeDto> children = new ArrayList<>();
}
