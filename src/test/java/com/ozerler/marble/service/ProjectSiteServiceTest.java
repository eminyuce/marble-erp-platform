package com.ozerler.marble.service;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void calculateProductionRequirement_StandardInputs_ReturnsShortfall() {
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
    void calculateProductionRequirement_ExcessCoverage_ReturnsZero() {
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
    void calculateProductionRequirement_NullInputs_SafeHandling() {
        BigDecimal shortfall = projectSiteService.calculateProductionRequirement(null, null, null, null);

        Assertions.assertThat(shortfall).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getProjectsPaged with customerId passes filter to repository")
    void getProjectsPaged_WithCustomerId_FiltersCorrectly() {
        com.ozerler.marble.model.Customer customer = com.ozerler.marble.model.Customer.builder()
                .id(10L)
                .companyName("Akdeniz İnşaat")
                .customerCode("AKD-01")
                .build();

        com.ozerler.marble.model.Project project = com.ozerler.marble.model.Project.builder()
                .id(1L)
                .projectCode("PRJ-001")
                .name("Akdeniz Plaza")
                .customer(customer)
                .customerName(customer.getCompanyName())
                .status(com.ozerler.marble.model.enums.ProjectStatus.ACTIVE)
                .build();

        org.springframework.data.domain.Page<com.ozerler.marble.model.Project> page =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(project));

        org.mockito.Mockito.when(projectRepository.searchProjectsFiltered(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(page);

        com.ozerler.marble.dto.TabulatorResponse<com.ozerler.marble.dto.ProjectDto> response =
                projectSiteService.getProjectsPaged(1, 10, null, "ACTIVE", 10L, null, null);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).hasSize(1);
        Assertions.assertThat(response.getData().get(0).getCustomerId()).isEqualTo(10L);
        Assertions.assertThat(response.getData().get(0).getCustomerCode()).isEqualTo("AKD-01");
    }
}
