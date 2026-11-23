package com.ozerler.marble.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.ozerler.marble.dto.view.ProjectView;
import com.ozerler.marble.model.Project;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = CustomerMapper.class)
public interface ProjectMapper {

    ProjectView toDto(Project project);
}
