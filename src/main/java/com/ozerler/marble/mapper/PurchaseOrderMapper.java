package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.PurchaseOrderView;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.PurchaseOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {SupplierMapper.class, ProjectMapper.class, CostCenterMapper.class})
public interface PurchaseOrderMapper {

    PurchaseOrderView toDto(PurchaseOrder order);

    PurchaseOrderView.LineView toLine(PurchaseOrderItem item);
}
