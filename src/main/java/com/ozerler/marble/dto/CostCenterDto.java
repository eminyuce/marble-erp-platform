package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.enums.BusinessUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostCenterDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("businessUnit")
    @JsonAlias("business_unit")
    private String businessUnit;

    @JsonProperty("businessUnitLabel")
    @JsonAlias("business_unit_label")
    private String businessUnitLabel;

    @JsonProperty("monthlyBudget")
    @JsonAlias("monthly_budget")
    private BigDecimal monthlyBudget;

    @JsonProperty("description")
    private String description;

    public static CostCenterDto fromEntity(CostCenter costCenter) {
        BusinessUnit unit = costCenter.getBusinessUnit();
        return CostCenterDto.builder()
                .id(costCenter.getId())
                .code(costCenter.getCode())
                .name(costCenter.getName())
                .businessUnit(unit != null ? unit.name() : null)
                .businessUnitLabel(unit != null ? unit.getLabel() : "")
                .monthlyBudget(costCenter.getMonthlyBudget())
                .description(costCenter.getDescription())
                .build();
    }
}
