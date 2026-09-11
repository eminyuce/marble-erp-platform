package com.ozerler.marble.dto;

import com.ozerler.marble.model.CutOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CutOrderDto {
    private Long id;
    private String cutOrderNo;
    private Long projectId;
    private String projectName;
    private String locationName;
    private String machineName;
    private String operatorName;
    private LocalDate plannedStart;
    private String status;
    private int itemCount;
    private BigDecimal totalAreaM2;
    private String notes;

    public static CutOrderDto fromEntity(CutOrder c) {
        BigDecimal area = c.getItems() != null ?
                c.getItems().stream()
                        .map(i -> i.getAreaM2() != null ? i.getAreaM2() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;

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
                .itemCount(c.getItems() != null ? c.getItems().size() : 0)
                .totalAreaM2(area)
                .notes(c.getNotes())
                .build();
    }
}
