package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
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
import com.ozerler.marble.util.GridPages;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectSiteService {

    private final ProjectRepository projectRepository;
    private final ProjectLocationRepository projectLocationRepository;
    private final SiteConsumptionRepository siteConsumptionRepository;
    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ProjectDto> getProjectsPaged(int page, int size, String search, String sortField, String sortDir) {
        Page<Project> projectPage = GridPages.execute(page, size, sortField, sortDir, GridPages.PROJECT_SORTS,
                pageable -> projectRepository.searchProjects(GridPages.normalizeSearch(search), pageable));
        List<ProjectDto> dtos = projectPage.getContent().stream()
                .map(ProjectDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, projectPage.getTotalPages(), projectPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        Objects.requireNonNull(id, getMessage("error.project.id.required"));
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.project.not_found", id)));
    }

    @Transactional
    public Project createProject(String projectCode, String name, String customerName,
                                 BigDecimal contractValue, BigDecimal estimatedCost,
                                 LocalDate startDate, LocalDate deliveryDate, String notes) {

        Objects.requireNonNull(projectCode, getMessage("error.project.code.required"));
        Objects.requireNonNull(name, getMessage("error.project.name.required"));

        Project project = Project.builder()
                .projectCode(projectCode.trim())
                .name(name.trim())
                .customerName(customerName != null ? customerName.trim() : "")
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
        Objects.requireNonNull(locationName, getMessage("error.location.name.required"));
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
                .status(Constants.STATUS_PLANNED)
                .build();

        return projectLocationRepository.save(location);
    }

    @Transactional
    public SiteConsumption recordConsumption(Long projectId, Long locationId,
                                             ConsumptionType type, String itemName,
                                             BigDecimal quantity, String unit, BigDecimal unitCost, String notes) {
        Objects.requireNonNull(type, getMessage("error.consumption.type.required"));
        Objects.requireNonNull(quantity, getMessage("error.consumption.quantity.required"));
        Objects.requireNonNull(unitCost, getMessage("error.consumption.unit_cost.required"));

        Project project = getProjectById(projectId);
        ProjectLocation location = projectLocationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.location.not_found", locationId)));

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

        updateLocationProgressIfStone(location, type, quantity);
        updateProjectActualCost(project, saved.getTotalCost());

        return saved;
    }

    private void updateLocationProgressIfStone(ProjectLocation location, ConsumptionType type, BigDecimal quantity) {
        if (type != ConsumptionType.STONE) {
            return;
        }
        BigDecimal currentInstalled = location.getInstalledAreaM2() != null ? location.getInstalledAreaM2() : BigDecimal.ZERO;
        BigDecimal newInstalled = currentInstalled.add(quantity);
        location.setInstalledAreaM2(newInstalled);

        BigDecimal plannedArea = location.getPlannedAreaM2() != null ? location.getPlannedAreaM2() : BigDecimal.ZERO;
        if (newInstalled.compareTo(plannedArea) >= 0) {
            location.setStatus(Constants.STATUS_COMPLETED);
        } else {
            location.setStatus(Constants.STATUS_IN_PROGRESS);
        }
        projectLocationRepository.save(location);
    }

    private void updateProjectActualCost(Project project, BigDecimal additionalCost) {
        if (additionalCost != null && additionalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentActual = project.getActualCost() != null ? project.getActualCost() : BigDecimal.ZERO;
            project.setActualCost(currentActual.add(additionalCost));
            projectRepository.save(project);
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectLocation> getLocationsByProject(Long projectId) {
        Objects.requireNonNull(projectId, getMessage("error.project.id.required"));
        return projectLocationRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<SiteConsumption> getConsumptionsByProject(Long projectId) {
        Objects.requireNonNull(projectId, getMessage("error.project.id.required"));
        return siteConsumptionRepository.findByProjectId(projectId);
    }

    /**
     * Requirement formula from Section 4.2:
     * Need = PlannedMetraj * (1 + ScrapPct) - (AvailableStock + InProduction)
     */
    public BigDecimal calculateProductionRequirement(BigDecimal plannedArea, BigDecimal scrapPct,
                                                     BigDecimal availableStock, BigDecimal inProduction) {
        BigDecimal effectivePlanned = plannedArea != null ? plannedArea : BigDecimal.ZERO;
        BigDecimal effectiveScrap = scrapPct != null ? scrapPct : BigDecimal.ZERO;
        BigDecimal effectiveStock = availableStock != null ? availableStock : BigDecimal.ZERO;
        BigDecimal effectiveInProduction = inProduction != null ? inProduction : BigDecimal.ZERO;

        BigDecimal scrapFactor = BigDecimal.ONE.add(effectiveScrap.divide(Constants.PERCENT_DIVISOR, Constants.CALCULATION_SCALE, RoundingMode.HALF_UP));
        BigDecimal grossRequirement = effectivePlanned.multiply(scrapFactor);
        BigDecimal currentCoverage = effectiveStock.add(effectiveInProduction);
        BigDecimal shortfall = grossRequirement.subtract(currentCoverage);

        return shortfall.compareTo(BigDecimal.ZERO) > 0 ? shortfall.setScale(Constants.RESULT_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
