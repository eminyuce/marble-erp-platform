package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.OrderRef;
import com.ozerler.marble.dto.view.PalletRef;
import com.ozerler.marble.dto.view.ProductionOrderView;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.ScrapLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {BlockMapper.class, SlabMapper.class})
public interface ProductionOrderMapper {

    @Mapping(target = "statusLabel", ignore = true)
    ProductionOrderView toDto(ProductionOrder order);

    OrderRef toRef(ProductionOrder order);

    PalletRef toPallet(Pallet pallet);

    ProductionOrderView.ScrapLine toScrap(ScrapLog scrap);
}
