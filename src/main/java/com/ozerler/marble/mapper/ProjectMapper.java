package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.ProjectView;
import com.ozerler.marble.model.Project;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = CustomerMapper.class)
public interface ProjectMapper {

    ProjectView toDto(Project project);
}
