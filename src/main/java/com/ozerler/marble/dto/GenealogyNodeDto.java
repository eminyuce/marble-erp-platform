package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing a node in the block/slab genealogy lineage tree.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenealogyNodeDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private String id;

    @JsonProperty("type")
    @JsonAlias("type")
    private String type; // QUARRY, BLOCK, PRODUCTION, SLAB, PALLET, WORKSHOP, ITEM, SITE

    @JsonProperty("title")
    @JsonAlias("title")
    private String title;

    @JsonProperty("subtitle")
    @JsonAlias("subtitle")
    private String subtitle;

    @JsonProperty("details")
    @JsonAlias("details")
    private String details;

    @JsonProperty("status")
    @JsonAlias("status")
    private String status;

    @JsonProperty("qr_code")
    @JsonAlias("qrCode")
    private String qrCode;

    @JsonProperty("alert")
    @JsonAlias("alert")
    private boolean alert;

    @JsonProperty("alert_message")
    @JsonAlias("alertMessage")
    private String alertMessage;

    @Builder.Default
    @JsonProperty("children")
    @JsonAlias("children")
    private List<GenealogyNodeDto> children = new ArrayList<>();
}
