package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.form.MachineForm;
import com.ozerler.marble.dto.view.MachineRef;
import com.ozerler.marble.model.Machine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MachineMapper {

    MachineRef toDto(Machine machine);

    MachineForm toForm(Machine machine);

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "addUserId", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    void updateEntity(MachineForm form, @MappingTarget Machine machine);
}
