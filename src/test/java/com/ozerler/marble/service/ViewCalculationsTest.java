package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.ozerler.marble.dto.view.FuelPeriodSummary;
import com.ozerler.marble.dto.view.SiteViews;
import com.ozerler.marble.mapper.SiteMapper;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.MachineFuelEntryRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;

/**
 * The figures that used to be computed in controllers or entity getters are now produced by services.
 */
class ViewCalculationsTest {

    @Test
    @DisplayName("total monthly budget sums the cost centers and skips missing budgets")
    void totalMonthlyBudget() {
        CostCenterRepository repository = Mockito.mock(CostCenterRepository.class);
        Mockito.when(repository.findAll(ArgumentMatchers.any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                CostCenter.builder().code("A").name("A").monthlyBudget(new BigDecimal("100.00")).build(),
                CostCenter.builder().code("B").name("B").build(),
                CostCenter.builder().code("C").name("C").monthlyBudget(new BigDecimal("50.50")).build())));
        CostAccountingService service = new CostAccountingService(repository, null, null, null);

        Assertions.assertThat(service.totalMonthlyBudget()).isEqualByComparingTo("150.50");
    }

    @Test
    @DisplayName("fuel period summary adds litres and amounts and counts distinct machines")
    void fuelPeriodSummary() {
        MachineFuelEntryRepository repository = Mockito.mock(MachineFuelEntryRepository.class);
        Machine first = Machine.builder().id(1L).name("A").build();
        Machine second = Machine.builder().id(2L).name("B").build();
        Mockito.when(repository.findByEntryDateBetweenOrderByEntryDateDesc(
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1)))
                .thenReturn(List.of(
                        MachineFuelEntry.builder().machine(first).litres(new BigDecimal("10")).totalAmount(new BigDecimal("400")).build(),
                        MachineFuelEntry.builder().machine(first).litres(new BigDecimal("5")).build(),
                        MachineFuelEntry.builder().machine(second).totalAmount(new BigDecimal("20")).build()));
        MachineFuelService service = new MachineFuelService(repository, null, null, null, null, null);

        FuelPeriodSummary summary = service.periodSummary("2026-09");

        Assertions.assertThat(summary.totalLitres()).isEqualByComparingTo("15");
        Assertions.assertThat(summary.totalAmount()).isEqualByComparingTo("420");
        Assertions.assertThat(summary.entryCount()).isEqualTo(3);
        Assertions.assertThat(summary.machineCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("project location views get status label and progress from the service")
    void locationViewsCarryCalculatedFields() {
        ProjectLocationRepository repository = Mockito.mock(ProjectLocationRepository.class);
        ProjectLocation location = ProjectLocation.builder().locationName("Lobi").status("COMPLETED")
                .plannedAreaM2(new BigDecimal("10")).installedAreaM2(new BigDecimal("4")).build();
        Mockito.when(repository.findByProjectId(3L)).thenReturn(List.of(location));
        SiteMapper mapper = Mockito.mock(SiteMapper.class);
        Mockito.when(mapper.toDto(location)).thenReturn(new SiteViews.LocationView());
        ProjectSiteService service = new ProjectSiteService(null, mapper, repository, null, null, null);

        SiteViews.LocationView view = service.getLocationViewsByProject(3L).get(0);

        Assertions.assertThat(view.getProgressPercentage()).isEqualByComparingTo("40.0");
        Assertions.assertThat(view.getStatusLabel()).isEqualTo(location.getStatusLabel());
    }
}
