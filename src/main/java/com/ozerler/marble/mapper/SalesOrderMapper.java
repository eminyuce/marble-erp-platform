package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.SalesOrderView;
import com.ozerler.marble.model.SalesOrder;
import com.ozerler.marble.model.SalesOrderItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {CustomerMapper.class, BlockMapper.class, SlabMapper.class})
public interface SalesOrderMapper {

    SalesOrderView toDto(SalesOrder order);

    @org.mapstruct.Mapping(target = "orderNo", source = "salesOrder.orderNo")
    @org.mapstruct.Mapping(target = "customerId", source = "salesOrder.customer.id")
    SalesOrderView.LineView toLine(SalesOrderItem item);
}
