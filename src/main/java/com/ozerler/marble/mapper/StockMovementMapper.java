package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.StockMovementView;
import com.ozerler.marble.model.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Copies stock movements into views. The invoice is left to the service, which sets it only when it is
 * already loaded, so a list never triggers one invoice query per row.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {BlockMapper.class, CustomerMapper.class})
public interface StockMovementMapper {

    /**
     * Copies one movement without its invoice.
     *
     * @param movement the entity
     * @return the view
     */
    @Mapping(target = "invoice", ignore = true)
    StockMovementView toDto(StockMovement movement);
}
