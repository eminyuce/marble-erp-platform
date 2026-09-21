package com.ozerler.marble.service;

import com.ozerler.marble.domain.SiteProfitAndLoss;
import com.ozerler.marble.model.ConstructionSiteStonePlan;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.MaterialLot;
import com.ozerler.marble.model.Pallet;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.SiteInstallation;
import com.ozerler.marble.model.SiteSupplyAllocation;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockReservation;
import com.ozerler.marble.model.enums.ExpenseCategory;
import com.ozerler.marble.model.enums.MaterialLotStatus;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SupplyRoute;
import com.ozerler.marble.model.enums.SurfaceFinish;
import com.ozerler.marble.repository.ConstructionSiteStonePlanRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SiteConsumptionRepository;
import com.ozerler.marble.repository.SiteInstallationRepository;
import com.ozerler.marble.repository.SiteSupplyAllocationRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockReservationRepository;
import com.ozerler.marble.util.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConstructionSiteService {

    private final ProjectRepository projectRepository;
    private final ProjectLocationRepository projectLocationRepository;
    private final ConstructionSiteStonePlanRepository stonePlanRepository;
    private final SiteSupplyAllocationRepository allocationRepository;
    private final SiteInstallationRepository installationRepository;
    private final SiteConsumptionRepository consumptionRepository;
    private final CostTransactionRepository costTransactionRepository;
    private final CustomerRepository customerRepository;
    private final StockReservationRepository stockReservationRepository;
    private final SlabRepository slabRepository;
    private final MaterialLotRepository materialLotRepository;
    private final PalletRepository palletRepository;
    private final ProjectSiteService projectSiteService;

    @Transactional
    public Project updateProject(Long id, String projectCode, String name, String customerName, Long customerId,
                                 BigDecimal contractValue, BigDecimal estimatedCost,
                                 LocalDate startDate, LocalDate deliveryDate, String notes, ProjectStatus status) {
        Project project = projectSiteService.getProjectById(id);
        project.setProjectCode(projectCode.trim());
        project.setName(name.trim());
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.customer.not_found", customerId)));
            project.setCustomer(customer);
            project.setCustomerName(customer.getCompanyName());
        } else if (customerName != null) {
            project.setCustomerName(customerName.trim());
        }
        project.setContractValue(contractValue != null ? contractValue : BigDecimal.ZERO);
        project.setEstimatedCost(estimatedCost != null ? estimatedCost : BigDecimal.ZERO);
        project.setStartDate(startDate);
        project.setDeliveryDate(deliveryDate);
        project.setNotes(notes);
        if (status != null) {
            project.setStatus(status);
        }
        Project saved = projectRepository.save(project);
        recalculateActualCost(saved.getId());
        return saved;
    }

    @Transactional
    public ConstructionSiteStonePlan addStonePlan(Long projectId, Long locationId, String stoneType,
                                                  SurfaceFinish surfaceFinish, BigDecimal widthCm, BigDecimal lengthCm,
                                                  BigDecimal plannedAreaM2, BigDecimal scrapPercent, SupplyRoute supplyRoute,
                                                  String notes) {
        Project project = projectSiteService.getProjectById(projectId);
        ProjectLocation location = projectLocationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.location.not_found", locationId)));
        BigDecimal planned = plannedAreaM2 != null ? plannedAreaM2 : BigDecimal.ZERO;
        BigDecimal scrap = scrapPercent != null ? scrapPercent : BigDecimal.ZERO;
        BigDecimal required = projectSiteService.calculateProductionRequirement(planned, scrap, BigDecimal.ZERO, BigDecimal.ZERO);
        return stonePlanRepository.save(ConstructionSiteStonePlan.builder()
                .project(project)
                .location(location)
                .stoneType(stoneType)
                .surfaceFinish(surfaceFinish)
                .widthCm(widthCm)
                .lengthCm(lengthCm)
                .plannedAreaM2(planned)
                .scrapPercent(scrap)
                .requiredAreaM2(required)
                .supplyRoute(supplyRoute != null ? supplyRoute : SupplyRoute.INTERNAL_PRODUCTION)
                .notes(notes)
                .build());
    }

    @Transactional
    public SiteSupplyAllocation allocateReservation(Long planId, Long slabId, BigDecimal areaM2) {
        ConstructionSiteStonePlan plan = stonePlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.site.plan.not_found", planId)));
        Slab slab = slabRepository.findById(slabId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.slab.not_found", slabId)));
        List<StockReservation> active = stockReservationRepository.findByStatus("ACTIVE").stream()
                .filter(reservation -> reservation.getSlab() != null && slabId.equals(reservation.getSlab().getId()))
                .toList();
        if (!active.isEmpty()) {
            throw new IllegalArgumentException(MessageUtils.getMessage("error.reservation.active_exists"));
        }
        StockReservation reservation = stockReservationRepository.save(StockReservation.builder()
                .slab(slab)
                .project(plan.getProject())
                .reservedAreaM2(areaM2 != null ? areaM2 : slab.getSurfaceAreaM2())
                .status("ACTIVE")
                .build());
        slab.setStatus(SlabStatus.RESERVED);
        materialLotRepository.findBySlabId(slabId).ifPresent(lot -> lot.setStatus(MaterialLotStatus.RESERVED));
        return allocationRepository.save(SiteSupplyAllocation.builder()
                .stonePlan(plan)
                .stockReservation(reservation)
                .allocatedAreaM2(reservation.getReservedAreaM2())
                .build());
    }

    @Transactional
    public SiteInstallation recordInstallation(Long projectId, Long locationId, Long materialLotId, Long palletId,
                                               BigDecimal installedAreaM2, BigDecimal wasteAreaM2,
                                               LocalDate installedOn, String crewName, String notes) {
        Project project = projectSiteService.getProjectById(projectId);
        ProjectLocation location = projectLocationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException(MessageUtils.getMessage("error.location.not_found", locationId)));
        Objects.requireNonNull(installedAreaM2, MessageUtils.getMessage("error.site.install.area.required"));
        MaterialLot lot = materialLotId != null ? materialLotRepository.findById(materialLotId).orElse(null) : null;
        Pallet pallet = palletId != null ? palletRepository.findById(palletId).orElse(null) : null;
        SiteInstallation installation = installationRepository.save(SiteInstallation.builder()
                .project(project)
                .location(location)
                .materialLot(lot)
                .pallet(pallet)
                .installedAreaM2(installedAreaM2)
                .wasteAreaM2(wasteAreaM2 != null ? wasteAreaM2 : BigDecimal.ZERO)
                .installedOn(installedOn != null ? installedOn : LocalDate.now())
                .crewName(crewName)
                .notes(notes)
                .build());
        BigDecimal installed = location.getInstalledAreaM2() != null ? location.getInstalledAreaM2() : BigDecimal.ZERO;
        location.setInstalledAreaM2(installed.add(installedAreaM2));
        if (lot != null) {
            lot.setStatus(MaterialLotStatus.INSTALLED);
        }
        recalculateActualCost(projectId);
        return installation;
    }

    @Transactional
    public BigDecimal recalculateActualCost(Long projectId) {
        Project project = projectSiteService.getProjectById(projectId);
        BigDecimal consumptions = consumptionRepository.getTotalConsumptionByProject(projectId);
        BigDecimal expenses = costTransactionRepository.sumByProjectId(projectId);
        BigDecimal total = (consumptions != null ? consumptions : BigDecimal.ZERO)
                .add(expenses != null ? expenses : BigDecimal.ZERO);
        project.setActualCost(total);
        projectRepository.save(project);
        return total;
    }

    @Transactional(readOnly = true)
    public List<ConstructionSiteStonePlan> plansFor(Long projectId) {
        return stonePlanRepository.findByProjectIdOrderByIdAsc(projectId);
    }

    @Transactional(readOnly = true)
    public List<SiteInstallation> installationsFor(Long projectId) {
        return installationRepository.findByProjectIdOrderByInstalledOnDesc(projectId);
    }

    @Transactional(readOnly = true)
    public SiteProfitAndLoss.Result profitAndLoss(Long projectId) {
        Project project = projectSiteService.getProjectById(projectId);
        Map<ExpenseCategory, BigDecimal> categories = new java.util.EnumMap<>(ExpenseCategory.class);
        for (Object[] row : costTransactionRepository.sumCategoriesByProjectId(projectId)) {
            if (row[0] instanceof ExpenseCategory category) {
                categories.put(category, (BigDecimal) row[1]);
            }
        }
        return SiteProfitAndLoss.calculate(
                categories.getOrDefault(ExpenseCategory.MATERIAL, BigDecimal.ZERO),
                categories.getOrDefault(ExpenseCategory.LABOR, BigDecimal.ZERO),
                categories.getOrDefault(ExpenseCategory.TAX, BigDecimal.ZERO),
                categories.getOrDefault(ExpenseCategory.CONSUMABLE, BigDecimal.ZERO),
                categories.getOrDefault(ExpenseCategory.TRANSPORTATION, BigDecimal.ZERO),
                categories.getOrDefault(ExpenseCategory.OTHER, BigDecimal.ZERO)
                        .add(categories.getOrDefault(ExpenseCategory.MAINTENANCE, BigDecimal.ZERO)),
                project.getContractValue());
    }

    public List<Customer> customers() {
        return customerRepository.findAllByOrderByCompanyNameAsc();
    }
}
