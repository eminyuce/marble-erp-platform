package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.CutOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @JsonProperty("item_count")
    @JsonAlias("itemCount")
    private int itemCount;

    @JsonProperty("total_area_m2")
    @JsonAlias("totalAreaM2")
    private BigDecimal totalAreaM2;

    @JsonProperty("notes")
    @JsonAlias("notes")
    private String notes;

    public static CutOrderDto fromEntity(CutOrder c) {
        return fromEntity(c, 0, BigDecimal.ZERO);
    }

    public static CutOrderDto fromEntity(CutOrder c, int itemCount, BigDecimal totalAreaM2) {
        return CutOrderDto.builder()
                .id(c.getId())
                .cutOrderNo(c.getCutOrderNo())
                .projectId(c.getProject() != null ? c.getProject().getId() : null)
                .projectName(c.getProject() != null ? c.getProject().getName() : "-")
                .locationName(c.getLocation() != null ? c.getLocation().getLocationName() : "-")
                .machineName(c.getMachineName())
                .operatorName(c.getOperatorName())
                .plannedStart(c.getPlannedStart())
                .status(c.getStatus())
                .itemCount(itemCount)
                .totalAreaM2(totalAreaM2 != null ? totalAreaM2 : BigDecimal.ZERO)
                .notes(c.getNotes())
                .build();
    }
}
