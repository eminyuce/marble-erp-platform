package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.BlockRef;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.model.Block;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {QuarryMapper.class, CustomerMapper.class, StockLocationMapper.class})
public interface BlockMapper {

    @Mapping(target = "length", source = "lengthCm")
    @Mapping(target = "width", source = "widthCm")
    @Mapping(target = "height", source = "heightCm")
    @Mapping(target = "dimensions", ignore = true)
    @Mapping(target = "weightTons", ignore = true)
    @Mapping(target = "canonicalStatus", ignore = true)
    @Mapping(target = "approximateTonnage", ignore = true)
    BlockView toDto(Block block);

    BlockRef toRef(Block block);
}
