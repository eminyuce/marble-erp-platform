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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectSiteService {

    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int CALCULATION_SCALE = 4;
    private static final int RESULT_SCALE = 2;

    private static final String LOCATION_STATUS_PLANNED = "PLANNED";
    private static final String LOCATION_STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String LOCATION_STATUS_COMPLETED = "COMPLETED";

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
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : DEFAULT_PAGE_SIZE, sort);

        Page<Project> projectPage = projectRepository.searchProjects(search, pageable);
        List<ProjectDto> dtos = projectPage.getContent().stream()
                .map(ProjectDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, projectPage.getTotalPages(), projectPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        Objects.requireNonNull(id, "Proje ID boş olamaz");
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proje bulunamadı: " + id));
    }

    @Transactional
    public Project createProject(String projectCode, String name, String customerName,
                                 BigDecimal contractValue, BigDecimal estimatedCost,
                                 LocalDate startDate, LocalDate deliveryDate, String notes) {

        Objects.requireNonNull(projectCode, "Proje kodu boş olamaz");
        Objects.requireNonNull(name, "Proje adı boş olamaz");

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
        Objects.requireNonNull(locationName, "Mahal adı boş olamaz");
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
                .status(LOCATION_STATUS_PLANNED)
                .build();

        return projectLocationRepository.save(location);
    }

    @Transactional
    public SiteConsumption recordConsumption(Long projectId, Long locationId,
                                            ConsumptionType type, String itemName,
                                            BigDecimal quantity, String unit, BigDecimal unitCost, String notes) {
        Objects.requireNonNull(type, "Sarfiyat tipi boş olamaz");
        Objects.requireNonNull(quantity, "Miktar boş olamaz");
        Objects.requireNonNull(unitCost, "Birim maliyet boş olamaz");

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
            location.setStatus(LOCATION_STATUS_COMPLETED);
        } else {
            location.setStatus(LOCATION_STATUS_IN_PROGRESS);
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
        Objects.requireNonNull(projectId, "Proje ID boş olamaz");
        return projectLocationRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<SiteConsumption> getConsumptionsByProject(Long projectId) {
        Objects.requireNonNull(projectId, "Proje ID boş olamaz");
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

        BigDecimal scrapFactor = BigDecimal.ONE.add(effectiveScrap.divide(PERCENT_DIVISOR, CALCULATION_SCALE, RoundingMode.HALF_UP));
        BigDecimal grossRequirement = effectivePlanned.multiply(scrapFactor);
        BigDecimal currentCoverage = effectiveStock.add(effectiveInProduction);
        BigDecimal shortfall = grossRequirement.subtract(currentCoverage);

        return shortfall.compareTo(BigDecimal.ZERO) > 0 ? shortfall.setScale(RESULT_SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
