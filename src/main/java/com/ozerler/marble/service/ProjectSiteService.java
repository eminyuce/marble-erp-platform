package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.SiteViews;
import com.ozerler.marble.mapper.SiteMapper;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.SiteConsumption;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.ConsumptionType;
import com.ozerler.marble.model.enums.CustomerType;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SiteConsumptionRepository;
import com.ozerler.marble.util.GridPages;
import com.ozerler.marble.util.MessageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectSiteService {

    private static final int CODE_SUFFIX_MODULUS = 100000;

    private final ProjectRepository projectRepository;
    private final SiteMapper siteMapper;
    private final ProjectLocationRepository projectLocationRepository;
    private final SiteConsumptionRepository siteConsumptionRepository;
    private final CustomerRepository customerRepository;
    private final MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        return MessageUtils.getMessage(code, args);
    }

    public record ProjectSummaryDto(long totalProjects, long activeProjects, long completedProjects, BigDecimal totalContractValue) {
    }

    @Transactional(readOnly = true)
    public ProjectSummaryDto getProjectSummary() {
        List<Project> all = BoundedReads.all(projectRepository);
        long total = all.size();
        long active = all.stream().filter(p -> p.getStatus() != null && "IN_PROGRESS".equalsIgnoreCase(p.getStatus().name())).count();
        long completed = all.stream().filter(p -> p.getStatus() != null && "COMPLETED".equalsIgnoreCase(p.getStatus().name())).count();
        BigDecimal totalContract = all.stream()
                .map(proj -> proj.getContractValue())
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        return new ProjectSummaryDto(total, active, completed, totalContract);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ProjectDto> getProjectsPaged(int page, int size, String search, String sortField, String sortDir) {
        return getProjectsPaged(page, size, search, null, null, sortField, sortDir);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ProjectDto> getProjectsPaged(int page, int size, String search, String status, String sortField, String sortDir) {
        return getProjectsPaged(page, size, search, status, null, sortField, sortDir);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<ProjectDto> getProjectsPaged(int page, int size, String search, String status, Long customerId, String sortField, String sortDir) {
        ProjectStatus pStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                pStatus = ProjectStatus.valueOf(status.trim().toUpperCase());
            } catch (Exception ignored) {
                // best effort: fall back to the default below
            }
        }
        final ProjectStatus finalStatus = pStatus;
        Page<Project> projectPage = GridPages.execute(page, size, sortField, sortDir, GridPages.PROJECT_SORTS,
                pageable -> projectRepository.searchProjectsFiltered(GridPages.normalizeSearch(search), finalStatus, customerId, pageable));
        List<Project> projects = projectPage.getContent();
        List<ProjectDto> dtos = projects.stream()
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
    public Project createProject(String projectCode, String name, String customerName, Long customerId,
                                 BigDecimal contractValue, BigDecimal estimatedCost,
                                 LocalDate startDate, LocalDate deliveryDate, String notes) {

        Objects.requireNonNull(projectCode, getMessage("error.project.code.required"));
        Objects.requireNonNull(name, getMessage("error.project.name.required"));

        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
            if (customer != null && (customerName == null || customerName.isBlank())) {
                customerName = customer.getCompanyName();
            }
        } else if (customerName != null && !customerName.isBlank()) {
            final String finalCustName = customerName.trim();
            customer = customerRepository.findByCompanyNameIgnoreCase(finalCustName)
                    .orElseGet(() -> {
                        String generatedCode = "CARI-" + (System.currentTimeMillis() % CODE_SUFFIX_MODULUS);
                        Customer newCust = Customer.builder()
                                .customerCode(generatedCode)
                                .companyName(finalCustName)
                                .customerType(CustomerType.CONSTRUCTION)
                                .businessUnit(BusinessUnit.SITE)
                                .build();
                        return customerRepository.save(newCust);
                    });
        }

        String customerNameValue;
        if (customerName != null) {
            customerNameValue = customerName.trim();
        } else {
            if (customer != null) {
                customerNameValue = customer.getCompanyName();
            } else {
                customerNameValue = "";
            }
        }
        Project project = Project.builder()
                .projectCode(projectCode.trim())
                .name(name.trim())
                .customer(customer)
                .customerName(customerNameValue)
                .contractValue(Objects.requireNonNullElse(contractValue, BigDecimal.ZERO))
                .estimatedCost(Objects.requireNonNullElse(estimatedCost, BigDecimal.ZERO))
                .actualCost(BigDecimal.ZERO)
                .startDate(startDate)
                .deliveryDate(deliveryDate)
                .status(ProjectStatus.ACTIVE)
                .notes(notes)
                .build();

        return projectRepository.save(project);
    }

    @Transactional
    public Project createProject(String projectCode, String name, String customerName,
                                 BigDecimal contractValue, BigDecimal estimatedCost,
                                 LocalDate startDate, LocalDate deliveryDate, String notes) {
        return createProject(projectCode, name, customerName, null, contractValue, estimatedCost, startDate, deliveryDate, notes);
    }

    @Transactional
    public ProjectLocation addLocation(Long projectId, Long parentId, String locationName,
                                       String floorLevel, String stoneSpec, BigDecimal plannedAreaM2) {
        Objects.requireNonNull(locationName, getMessage("error.location.name.required"));
        Project project = getProjectById(projectId);
        ProjectLocation parent;
        if (parentId != null) {
            parent = projectLocationRepository.findById(parentId).orElse(null);
        } else {
            parent = null;
        }

        ProjectLocation location = ProjectLocation.builder()
                .project(project)
                .parent(parent)
                .locationName(locationName.trim())
                .floorLevel(floorLevel)
                .stoneSpec(stoneSpec)
                .plannedAreaM2(Objects.requireNonNullElse(plannedAreaM2, BigDecimal.ZERO))
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
        BigDecimal currentInstalled;
        if (location.getInstalledAreaM2() != null) {
            currentInstalled = location.getInstalledAreaM2();
        } else {
            currentInstalled = BigDecimal.ZERO;
        }
        BigDecimal newInstalled = currentInstalled.add(quantity);
        location.setInstalledAreaM2(newInstalled);

        BigDecimal plannedArea;
        if (location.getPlannedAreaM2() != null) {
            plannedArea = location.getPlannedAreaM2();
        } else {
            plannedArea = BigDecimal.ZERO;
        }
        if (newInstalled.compareTo(plannedArea) >= 0) {
            location.setStatus(Constants.STATUS_COMPLETED);
        } else {
            location.setStatus(Constants.STATUS_IN_PROGRESS);
        }
        projectLocationRepository.save(location);
    }

    private void updateProjectActualCost(Project project, BigDecimal additionalCost) {
        if (additionalCost != null && additionalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentActual;
            if (project.getActualCost() != null) {
                currentActual = project.getActualCost();
            } else {
                currentActual = BigDecimal.ZERO;
            }
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
        BigDecimal effectivePlanned;
        if (plannedArea != null) {
            effectivePlanned = plannedArea;
        } else {
            effectivePlanned = BigDecimal.ZERO;
        }
        BigDecimal effectiveScrap;
        if (scrapPct != null) {
            effectiveScrap = scrapPct;
        } else {
            effectiveScrap = BigDecimal.ZERO;
        }
        BigDecimal effectiveStock;
        if (availableStock != null) {
            effectiveStock = availableStock;
        } else {
            effectiveStock = BigDecimal.ZERO;
        }
        BigDecimal effectiveInProduction;
        if (inProduction != null) {
            effectiveInProduction = inProduction;
        } else {
            effectiveInProduction = BigDecimal.ZERO;
        }

        BigDecimal scrapFactor = BigDecimal.ONE.add(effectiveScrap.divide(Constants.PERCENT_DIVISOR, Constants.CALCULATION_SCALE, RoundingMode.HALF_UP));
        BigDecimal grossRequirement = effectivePlanned.multiply(scrapFactor);
        BigDecimal currentCoverage = effectiveStock.add(effectiveInProduction);
        BigDecimal shortfall = grossRequirement.subtract(currentCoverage);

        if (shortfall.compareTo(BigDecimal.ZERO) > 0) {
            return shortfall.setScale(Constants.RESULT_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Same as {@code getConsumptionsByProject}, copied into a view inside the transaction.
     *
     * @param projectId see {@code getConsumptionsByProject}
     * @return the view
     */
    @Transactional(readOnly = true)
    public List<SiteViews.ConsumptionView> getConsumptionViewsByProject(Long projectId) {
        return getConsumptionsByProject(projectId).stream().map(siteMapper::toConsumption).toList();
    }

    /**
     * Locations of a project as views; status label and progress are computed here, not in the mapper.
     *
     * @param projectId project id
     * @return location views
     */
    @Transactional(readOnly = true)
    public List<SiteViews.LocationView> getLocationViewsByProject(Long projectId) {
        return getLocationsByProject(projectId).stream().map(location -> {
            SiteViews.LocationView view = siteMapper.toDto(location);
            view.setStatusLabel(location.getStatusLabel());
            view.setProgressPercentage(location.getProgressPercentage());
            return view;
        }).toList();
    }
}
