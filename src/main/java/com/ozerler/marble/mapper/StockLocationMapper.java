package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.form.StockLocationForm;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.model.StockLocation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StockLocationMapper {

    BlockView.LocationView toDto(StockLocation location);

    StockLocationForm toForm(StockLocation location);

    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "addUserId", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    void updateEntity(StockLocationForm form, @MappingTarget StockLocation location);
}
