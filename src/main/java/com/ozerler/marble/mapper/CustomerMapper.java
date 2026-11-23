package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.form.CustomerForm;
import com.ozerler.marble.dto.view.PartyView;
import com.ozerler.marble.model.Customer;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomerMapper {

    @Mapping(target = "supplierType", ignore = true)
    PartyView toDto(Customer customer);

    CustomerForm toForm(Customer customer);

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "addUserId", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "projects", ignore = true)
    void updateEntity(CustomerForm form, @MappingTarget Customer customer);
}
