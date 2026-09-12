package com.ozerler.marble.service;

import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SiteConsumptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProjectSiteServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectLocationRepository projectLocationRepository;
    @Mock
    private SiteConsumptionRepository siteConsumptionRepository;

    private ProjectSiteService projectSiteService;

    @BeforeEach
    void setUp() {
        projectSiteService = new ProjectSiteService(projectRepository, projectLocationRepository, siteConsumptionRepository, null);
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

        assertThat(shortfall).isEqualTo(new BigDecimal("40.00"));
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

        assertThat(shortfall).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateProductionRequirement handles null inputs safely by treating them as ZERO")
    void calculateProductionRequirement_NullInputs_SafeHandling() {
        BigDecimal shortfall = projectSiteService.calculateProductionRequirement(null, null, null, null);

        assertThat(shortfall).isEqualTo(BigDecimal.ZERO);
    }
}
