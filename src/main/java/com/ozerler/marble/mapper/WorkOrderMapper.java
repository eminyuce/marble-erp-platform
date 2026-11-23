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

    /**
     * Copies the id and number of an order.
     *
     * @param order the entity
     * @return the reference
     */
    /**
     * Copies the header fields only, for list rows; items and history are left empty.
     *
     * @param order the entity
     * @return the header view
     */
    @org.mapstruct.Named("row")
    @Mapping(target = "orderNumber", source = "orderNo")
    @Mapping(target = "plannedQuantity", source = "quantity")
    @Mapping(target = "customerNameForView", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    WorkOrderView toRow(OperationWorkOrder order);

    com.ozerler.marble.dto.view.OrderRef toRef(OperationWorkOrder order);

    WorkOrderView.ItemView toItem(OperationWorkOrderItem item);

    WorkOrderView.HistoryView toHistory(WorkOrderStatusHistory history);
}
