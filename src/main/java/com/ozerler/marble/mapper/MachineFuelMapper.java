package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.MachineFuelEntryView;
import com.ozerler.marble.model.MachineFuelEntry;

/**
 * Copies fuel entries into views. No calculation happens here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = MachineMapper.class)
public interface MachineFuelMapper {

    /**
     * Copies one fuel entry with its machine.
     *
     * @param entry the entity
     * @return the view
     */
    MachineFuelEntryView toDto(MachineFuelEntry entry);
}
