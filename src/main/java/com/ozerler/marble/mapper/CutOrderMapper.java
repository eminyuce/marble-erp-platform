package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.CutOrderView;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.CutOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {ProjectMapper.class, MachineMapper.class, SlabMapper.class})
public interface CutOrderMapper {

    @Mapping(target = "statusLabel", ignore = true)
    CutOrderView toDto(CutOrder order);

    @Mapping(target = "statusLabel", ignore = true)
    CutOrderView.ItemView toItem(CutItem item);
}
