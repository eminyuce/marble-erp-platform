package com.ozerler.marble.service;

import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.SiteConsumption;
import com.ozerler.marble.model.enums.ConsumptionType;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SiteConsumptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectSiteService {

    private final ProjectRepository projectRepository;
    private final ProjectLocationRepository projectLocationRepository;
    private final SiteConsumptionRepository siteConsumptionRepository;

    @Transactional(readOnly = true)
    public TabulatorResponse<ProjectDto> getProjectsPaged(int page, int size, String search, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortField != null && !sortField.isBlank()) {
            Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(dir, sortField);
        }

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : 10, sort);

        Page<Project> projectPage = projectRepository.searchProjects(search, pageable);
        List<ProjectDto> dtos = projectPage.getContent().stream()
                .map(ProjectDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, projectPage.getTotalPages(), projectPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + id));
    }

    @Transactional
    public Project createProject(String projectCode, String name, String customerName,
                                 BigDecimal contractValue, BigDecimal estimatedCost,
                                 LocalDate startDate, LocalDate deliveryDate, String notes) {

        Project project = Project.builder()
                .projectCode(projectCode.trim())
                .name(name.trim())
                .customerName(customerName.trim())
                .contractValue(contractValue != null ? contractValue : BigDecimal.ZERO)
                .estimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO)
                .actualCost(BigDecimal.ZERO)
                .startDate(startDate)
                .deliveryDate(deliveryDate)
                .status(ProjectStatus.ACTIVE)
                .notes(notes)
                .build();

        return projectRepository.save(project);
    }

    @Transactional
    public ProjectLocation addLocation(Long projectId, Long parentId, String locationName,
                                      String floorLevel, String stoneSpec, BigDecimal plannedAreaM2) {
        Project project = getProjectById(projectId);
        ProjectLocation parent = parentId != null ? projectLocationRepository.findById(parentId).orElse(null) : null;

        ProjectLocation location = ProjectLocation.builder()
                .project(project)
                .parent(parent)
                .locationName(locationName.trim())
                .floorLevel(floorLevel)
                .stoneSpec(stoneSpec)
                .plannedAreaM2(plannedAreaM2 != null ? plannedAreaM2 : BigDecimal.ZERO)
                .installedAreaM2(BigDecimal.ZERO)
                .status("PLANNED")
                .build();

        return projectLocationRepository.save(location);
    }

    @Transactional
    public SiteConsumption recordConsumption(Long projectId, Long locationId,
                                            ConsumptionType type, String itemName,
                                            BigDecimal quantity, String unit, BigDecimal unitCost, String notes) {
        Project project = getProjectById(projectId);
        ProjectLocation location = projectLocationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Mahal bulunamadı: " + locationId));

        SiteConsumption consumption = SiteConsumption.builder()
                .project(project)
                .location(location)
                .consumptionType(type)
                .itemName(itemName)
                .quantity(quantity)
                .unit(unit)
                .unitCost(unitCost)
                .notes(notes)
                .build();

        consumption.calculateTotal();
        SiteConsumption saved = siteConsumptionRepository.save(consumption);

        // Update location installation if stone
        if (type == ConsumptionType.STONE) {
            location.setInstalledAreaM2(location.getInstalledAreaM2().add(quantity));
            if (location.getInstalledAreaM2().compareTo(location.getPlannedAreaM2()) >= 0) {
                location.setStatus("COMPLETED");
            } else {
                location.setStatus("IN_PROGRESS");
            }
            projectLocationRepository.save(location);
        }

        // Add to project actual cost
        project.setActualCost(project.getActualCost().add(saved.getTotalCost()));
        projectRepository.save(project);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ProjectLocation> getLocationsByProject(Long projectId) {
        return projectLocationRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<SiteConsumption> getConsumptionsByProject(Long projectId) {
        return siteConsumptionRepository.findByProjectId(projectId);
    }

    /**
     * Requirement formula from Section 4.2:
     * Need = PlannedMetraj * (1 + ScrapPct) - (AvailableStock + InProduction)
     */
    public BigDecimal calculateProductionRequirement(BigDecimal plannedArea, BigDecimal scrapPct,
                                                     BigDecimal availableStock, BigDecimal inProduction) {
        BigDecimal factor = BigDecimal.ONE.add(scrapPct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        BigDecimal grossRequirement = plannedArea.multiply(factor);
        BigDecimal currentCoverage = availableStock.add(inProduction);
        BigDecimal shortfall = grossRequirement.subtract(currentCoverage);
        return shortfall.compareTo(BigDecimal.ZERO) > 0 ? shortfall.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
