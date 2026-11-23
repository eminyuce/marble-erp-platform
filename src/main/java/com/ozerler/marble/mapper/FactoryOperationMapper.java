package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.FactoryOperationView;
import com.ozerler.marble.model.FactoryOperation;
import com.ozerler.marble.model.FactoryWorkOrder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Copies factory operations into views. No calculation happens here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {MachineMapper.class, BlockMapper.class})
public interface FactoryOperationMapper {

    /**
     * Copies one operation.
     *
     * @param operation the entity
     * @return the view
     */
    FactoryOperationView toDto(FactoryOperation operation);

    /**
     * Copies the work order reference.
     *
     * @param workOrder the entity
     * @return the reference
     */
    FactoryOperationView.WorkOrderRef toWorkOrderRef(FactoryWorkOrder workOrder);
}
