package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.RoleView;
import com.ozerler.marble.model.Role;

/**
 * Copies roles into views. No calculation happens here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapper {

    /**
     * Copies one role.
     *
     * @param role the entity
     * @return the view
     */
    RoleView toDto(Role role);
}
