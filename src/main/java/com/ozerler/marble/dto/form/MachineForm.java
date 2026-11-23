package com.ozerler.marble.dto.form;

import com.ozerler.marble.dto.view.AuditView;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MachineForm extends AuditView {
    private Long id;
    private String code;
    private String name;
    private BusinessUnit businessUnit = BusinessUnit.FACTORY;
    private MachineType machineType = MachineType.GANGSAW;
    private boolean active = true;
    private String notes;
}
