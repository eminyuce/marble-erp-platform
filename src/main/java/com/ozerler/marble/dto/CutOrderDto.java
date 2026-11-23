package com.ozerler.marble.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.enums.OperationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing cut orders in the workshop.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CutOrderDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @JsonProperty("cut_order_no")
    @JsonAlias("cutOrderNo")
    private String cutOrderNo;

    @JsonProperty("project_id")
    @JsonAlias("projectId")
    private Long projectId;

    @JsonProperty("project_name")
    @JsonAlias("projectName")
    private String projectName;

    @JsonProperty("location_name")
    @JsonAlias("locationName")
    private String locationName;

    @JsonProperty("machine_name")
    @JsonAlias("machineName")
    private String machineName;

    @JsonProperty("operator_name")
    @JsonAlias("operatorName")
    private String operatorName;

    @JsonProperty("planned_start")
    @JsonAlias("plannedStart")
    private LocalDate plannedStart;

    @JsonProperty("status")
    @JsonAlias("status")
    private String status;

    @JsonProperty("status_label")
    @JsonAlias("statusLabel")
    private String statusLabel;

    @JsonProperty("item_count")
    @JsonAlias("itemCount")
    private int itemCount;

    @JsonProperty("total_area_m2")
    @JsonAlias("totalAreaM2")
    private BigDecimal totalAreaM2;

    @JsonProperty("notes")
    @JsonAlias("notes")
    private String notes;

    @JsonProperty("purpose_label")
    @JsonAlias("purposeLabel")
    private String purposeLabel;

    public static CutOrderDto fromEntity(CutOrder c) {
        return fromEntity(c, 0, BigDecimal.ZERO);
    }

    public static CutOrderDto fromEntity(CutOrder c, int itemCount, BigDecimal totalAreaM2) {
        Long projectIdValue;
        if (c.getProject() != null) {
            projectIdValue = c.getProject().getId();
        } else {
            projectIdValue = null;
        }
        String projectNameValue;
        if (c.getProject() != null) {
            projectNameValue = c.getProject().getName();
        } else {
            projectNameValue = "-";
        }
        String locationNameValue;
        if (c.getLocation() != null) {
            locationNameValue = c.getLocation().getLocationName();
        } else {
            locationNameValue = "-";
        }
        String purposeLabelValue;
        if (c.getPurpose() != null) {
            purposeLabelValue = c.getPurpose().getLabel();
        } else {
            purposeLabelValue = "";
        }
        return CutOrderDto.builder()
                .id(c.getId())
                .cutOrderNo(c.getCutOrderNo())
                .projectId(projectIdValue)
                .projectName(projectNameValue)
                .locationName(locationNameValue)
                .machineName(c.getMachineName())
                .operatorName(c.getOperatorName())
                .plannedStart(c.getPlannedStart())
                .status(c.getStatus())
                .statusLabel(OperationStatus.labelOf(c.getStatus()))
                .itemCount(itemCount)
                .totalAreaM2(Objects.requireNonNullElse(totalAreaM2, BigDecimal.ZERO))
                .notes(c.getNotes())
                .purposeLabel(purposeLabelValue)
                .build();
    }
}
