package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.WorkOrderView;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.OperationWorkOrderItem;
import com.ozerler.marble.model.WorkOrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {BlockMapper.class, SlabMapper.class, StockItemMapper.class})
public interface WorkOrderMapper {

    @Mapping(target = "orderNumber", source = "orderNo")
    @Mapping(target = "plannedQuantity", source = "quantity")
    @Mapping(target = "customerNameForView", ignore = true)
    WorkOrderView toDto(OperationWorkOrder order);

    WorkOrderView.ItemView toItem(OperationWorkOrderItem item);

    WorkOrderView.HistoryView toHistory(WorkOrderStatusHistory history);
}
