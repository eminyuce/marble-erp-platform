package com.ozerler.marble.dto.view;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineRef {
    private Long id;
    private String code;
    private String name;
    private com.ozerler.marble.model.enums.BusinessUnit businessUnit;
    private com.ozerler.marble.model.enums.MachineType machineType;
    private boolean active;
    private String notes;
}
