package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.form.QuarryForm;
import com.ozerler.marble.dto.view.QuarryRef;
import com.ozerler.marble.model.Quarry;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface QuarryMapper {

    QuarryRef toDto(Quarry quarry);

    QuarryForm toForm(Quarry quarry);

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "addUserId", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    void updateEntity(QuarryForm form, @MappingTarget Quarry quarry);
}
