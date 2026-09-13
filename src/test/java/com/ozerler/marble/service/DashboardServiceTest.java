package com.ozerler.marble.service;

import com.ozerler.marble.dto.DashboardKpiDto;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ScrapLogRepository scrapLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CostTransactionRepository costTransactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getDashboardKpis aggregates all metrics and converts block weight to tons")
    void getDashboardKpis_CalculatesCorrectly() {
        when(slabRepository.getTotalInventoryAreaM2()).thenReturn(new BigDecimal("1540.50"));
        when(slabRepository.getTotalAreaByStatus(SlabStatus.AVAILABLE)).thenReturn(new BigDecimal("1200.00"));
        when(slabRepository.getTotalAreaByStatus(SlabStatus.RESERVED)).thenReturn(new BigDecimal("340.50"));
        when(blockRepository.countByStatus(BlockStatus.FACTORY_STOCK)).thenReturn(18L);
        when(blockRepository.getTotalFactoryStockWeightKg()).thenReturn(36000.0);
        when(projectRepository.countByStatus(ProjectStatus.ACTIVE)).thenReturn(4L);
        when(scrapLogRepository.getTotalScrapCostImpact()).thenReturn(new BigDecimal("28500.00"));
        when(scrapLogRepository.getScrapSummaryByReason()).thenReturn(List.<Object[]>of(new Object[]{"FR_01", 3L, new BigDecimal("12.5")}));
        when(userRepository.countByDeletedFalse()).thenReturn(12L);
        when(costTransactionRepository.getCostDistributionByCenter()).thenReturn(List.<Object[]>of(new Object[]{"KATRAK", new BigDecimal("45000")}));

        DashboardKpiDto result = dashboardService.getDashboardKpis();

        assertThat(result).isNotNull();
        assertThat(result.getTotalSlabArea()).isEqualTo(new BigDecimal("1540.50"));
        assertThat(result.getAvailableSlabArea()).isEqualTo(new BigDecimal("1200.00"));
        assertThat(result.getReservedSlabArea()).isEqualTo(new BigDecimal("340.50"));
        assertThat(result.getFactoryBlockCount()).isEqualTo(18L);
        assertThat(result.getFactoryBlockWeightTon()).isEqualTo(36.0);
        assertThat(result.getActiveProjectsCount()).isEqualTo(4L);
        assertThat(result.getTotalScrapImpact()).isEqualTo(new BigDecimal("28500.00"));
        assertThat(result.getTotalUsers()).isEqualTo(12L);
        assertThat(result.getScrapSummary()).hasSize(1);
        assertThat(result.getScrapSummary().get(0).getCount()).isEqualTo(3L);
        assertThat(result.getScrapSummary().get(0).getTotalWeightKg()).isEqualTo(new BigDecimal("12.5"));
        assertThat(result.getCostDistribution()).hasSize(1);
        assertThat(result.getCostDistribution().get(0).getCostCenterName()).isEqualTo("KATRAK");
        assertThat(result.getCostDistribution().get(0).getTotalAmount()).isEqualTo(new BigDecimal("45000"));
    }
}
