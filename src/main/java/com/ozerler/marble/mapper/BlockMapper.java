package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.BlockMarkView;
import com.ozerler.marble.dto.view.BlockMovementView;
import com.ozerler.marble.dto.view.BlockRef;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.BlockCustomerMark;
import com.ozerler.marble.model.BlockLocationMovement;

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

    /**
     * Copies a customer mark.
     *
     * @param mark the entity
     * @return the view
     */
    BlockMarkView toMark(BlockCustomerMark mark);

    /**
     * Copies a location movement.
     *
     * @param movement the entity
     * @return the view
     */
    BlockMovementView toMovement(BlockLocationMovement movement);
}
