package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.ozerler.marble.dto.ProjectDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SiteConsumptionRepository;

@ExtendWith(MockitoExtension.class)
class ProjectSiteServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectLocationRepository projectLocationRepository;
    @Mock
    private SiteConsumptionRepository siteConsumptionRepository;
    @Mock
    private CustomerRepository customerRepository;

    private ProjectSiteService projectSiteService;

    @BeforeEach
    void setUp() {
        projectSiteService = new ProjectSiteService(projectRepository, null, projectLocationRepository, siteConsumptionRepository, customerRepository, null);
    }

    @Test
    @DisplayName("calculateProductionRequirement calculates gross need with scrap and deducts coverage")
    void calculateProductionRequirementStandardInputsReturnsShortfall() {
        // Planned: 100 m2, Scrap: 10% -> Gross = 110 m2. Stock: 40, InProd: 30 -> Coverage = 70. Shortfall = 40.00
        BigDecimal plannedArea = new BigDecimal("100.00");
        BigDecimal scrapPct = new BigDecimal("10.00");
        BigDecimal availableStock = new BigDecimal("40.00");
        BigDecimal inProduction = new BigDecimal("30.00");

        BigDecimal shortfall = projectSiteService.calculateProductionRequirement(
                plannedArea, scrapPct, availableStock, inProduction
        );

        Assertions.assertThat(shortfall).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("calculateProductionRequirement returns ZERO when stock and in-production exceed gross need")
    void calculateProductionRequirementExcessCoverageReturnsZero() {
        BigDecimal plannedArea = new BigDecimal("100.00");
        BigDecimal scrapPct = new BigDecimal("5.00");
        BigDecimal availableStock = new BigDecimal("150.00");
        BigDecimal inProduction = BigDecimal.ZERO;

        BigDecimal shortfall = projectSiteService.calculateProductionRequirement(
                plannedArea, scrapPct, availableStock, inProduction
        );

        Assertions.assertThat(shortfall).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateProductionRequirement handles null inputs safely by treating them as ZERO")
    void calculateProductionRequirementNullInputsSafeHandling() {
        BigDecimal shortfall = projectSiteService.calculateProductionRequirement(null, null, null, null);

        Assertions.assertThat(shortfall).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getProjectsPaged with customerId passes filter to repository")
    void getProjectsPagedWithCustomerIdFiltersCorrectly() {
        Customer customer = Customer.builder()
                .id(10L)
                .companyName("Akdeniz İnşaat")
                .customerCode("AKD-01")
                .build();

        Project project = Project.builder()
                .id(1L)
                .projectCode("PRJ-001")
                .name("Akdeniz Plaza")
                .customer(customer)
                .customerName(customer.getCompanyName())
                .status(ProjectStatus.ACTIVE)
                .build();

        Page<Project> page =
                new PageImpl<>(List.of(project));

        Mockito.when(projectRepository.searchProjectsFiltered(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(10L),
                ArgumentMatchers.any()
        )).thenReturn(page);

        TabulatorResponse<ProjectDto> response =
                projectSiteService.getProjectsPaged(1, 10, null, "ACTIVE", 10L, null, null);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).hasSize(1);
        Assertions.assertThat(response.getData().get(0).getCustomerId()).isEqualTo(10L);
        Assertions.assertThat(response.getData().get(0).getCustomerCode()).isEqualTo("AKD-01");
    }
}
