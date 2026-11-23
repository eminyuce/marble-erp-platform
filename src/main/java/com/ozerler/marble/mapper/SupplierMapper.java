package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.form.SupplierForm;
import com.ozerler.marble.dto.view.PartyView;
import com.ozerler.marble.model.Supplier;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SupplierMapper {

    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "customerType", ignore = true)
    PartyView toDto(Supplier supplier);

    SupplierForm toForm(Supplier supplier);

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "addUserId", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    void updateEntity(SupplierForm form, @MappingTarget Supplier supplier);
}
