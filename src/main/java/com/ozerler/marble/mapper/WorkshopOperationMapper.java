package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.WorkshopOperationView;
import com.ozerler.marble.model.WorkshopOperation;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Copies workshop operations into views. No calculation happens here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = MachineMapper.class)
public interface WorkshopOperationMapper {

    /**
     * Copies one operation with its machine.
     *
     * @param operation the entity
     * @return the view
     */
    WorkshopOperationView toDto(WorkshopOperation operation);
}
