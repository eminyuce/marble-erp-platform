package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineDto {

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

    @JsonProperty("machineType")
    @JsonAlias("machine_type")
    private String machineType;

    @JsonProperty("machineTypeLabel")
    @JsonAlias("machine_type_label")
    private String machineTypeLabel;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("notes")
    private String notes;

    public static MachineDto fromEntity(Machine machine) {
        BusinessUnit unit = machine.getBusinessUnit();
        MachineType type = machine.getMachineType();
        return MachineDto.builder()
                .id(machine.getId())
                .code(machine.getCode())
                .name(machine.getName())
                .businessUnit(unit != null ? unit.name() : null)
                .businessUnitLabel(unit != null ? unit.getLabel() : "")
                .machineType(type != null ? type.name() : null)
                .machineTypeLabel(type != null ? type.getLabel() : "")
                .active(machine.isActive())
                .notes(machine.getNotes())
                .build();
    }
}
