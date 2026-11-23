package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.StockItemView;
import com.ozerler.marble.dto.view.StockRef;
import com.ozerler.marble.model.StockItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = {CustomerMapper.class, BlockMapper.class})
public interface StockItemMapper {

    StockItemView toDto(StockItem item);

    StockRef toRef(StockItem item);
}
