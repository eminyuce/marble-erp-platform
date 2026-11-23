package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLocationDto {

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

    @JsonProperty("locationType")
    @JsonAlias("location_type")
    private String locationType;

    @JsonProperty("locationTypeLabel")
    @JsonAlias("location_type_label")
    private String locationTypeLabel;

    @JsonProperty("active")
    private boolean active;

    public static StockLocationDto fromEntity(StockLocation location) {
        BusinessUnit unit = location.getBusinessUnit();
        StockLocationType type = location.getLocationType();
        String businessUnitValue;
        if (unit != null) {
            businessUnitValue = unit.name();
        } else {
            businessUnitValue = null;
        }
        String businessUnitLabelValue;
        if (unit != null) {
            businessUnitLabelValue = unit.getLabel();
        } else {
            businessUnitLabelValue = "";
        }
        String locationTypeValue;
        if (type != null) {
            locationTypeValue = type.name();
        } else {
            locationTypeValue = null;
        }
        String locationTypeLabelValue;
        if (type != null) {
            locationTypeLabelValue = type.getLabel();
        } else {
            locationTypeLabelValue = "";
        }
        return StockLocationDto.builder()
                .id(location.getId())
                .code(location.getCode())
                .name(location.getName())
                .businessUnit(businessUnitValue)
                .businessUnitLabel(businessUnitLabelValue)
                .locationType(locationTypeValue)
                .locationTypeLabel(locationTypeLabelValue)
                .active(location.isActive())
                .build();
    }
}
