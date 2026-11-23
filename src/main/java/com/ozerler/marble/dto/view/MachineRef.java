package com.ozerler.marble.dto.view;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineRef {
    private Long id;
    private String code;
    private String name;
    private BusinessUnit businessUnit;
    private MachineType machineType;
    private boolean active;
    private String notes;
}
