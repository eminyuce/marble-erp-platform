package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.ProductionOrderView;
import com.ozerler.marble.dto.view.SlabRef;
import com.ozerler.marble.dto.view.SlabView;
import com.ozerler.marble.model.Slab;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {BlockMapper.class, CustomerMapper.class})
public interface SlabMapper {

    SlabView toDto(Slab slab);

    SlabRef toRef(Slab slab);

    ProductionOrderView.SlabLine toLine(Slab slab);
}
