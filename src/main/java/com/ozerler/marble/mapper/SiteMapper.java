package com.ozerler.marble.mapper;

import com.ozerler.marble.dto.view.SiteViews;
import com.ozerler.marble.model.ConstructionSiteStonePlan;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.SiteConsumption;
import com.ozerler.marble.model.SiteInstallation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SiteMapper {

    @Mapping(target = "statusLabel", ignore = true)
    @Mapping(target = "progressPercentage", ignore = true)
    SiteViews.LocationView toDto(ProjectLocation location);

    SiteViews.ConsumptionView toConsumption(SiteConsumption consumption);

    SiteViews.PlanView toPlan(ConstructionSiteStonePlan plan);

    SiteViews.InstallationView toInstallation(SiteInstallation installation);
}
