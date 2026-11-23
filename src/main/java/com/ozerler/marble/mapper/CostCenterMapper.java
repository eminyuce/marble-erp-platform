package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.form.CostCenterForm;
import com.ozerler.marble.dto.view.CostCenterRef;
import com.ozerler.marble.model.CostCenter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CostCenterMapper {

    CostCenterRef toDto(CostCenter costCenter);

    CostCenterForm toForm(CostCenter costCenter);

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "addUserId", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    void updateEntity(CostCenterForm form, @MappingTarget CostCenter costCenter);
}
