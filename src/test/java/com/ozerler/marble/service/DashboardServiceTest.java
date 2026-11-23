package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.dto.DashboardKpiDto;
import com.ozerler.marble.model.enums.ProjectStatus;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CheckRecordRepository;
import com.ozerler.marble.repository.CollectionRecordRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.repository.OperationWorkOrderRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.repository.UserRepository;

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
    @Mock
    private StockItemRepository stockItemRepository;
    @Mock
    private OperationWorkOrderRepository workOrderRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CollectionRecordRepository collectionRepository;
    @Mock
    private CheckRecordRepository checkRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getDashboardKpis aggregates all metrics and converts block weight to tons")
    void getDashboardKpis_CalculatesCorrectly() {
        Mockito.when(slabRepository.getTotalInventoryAreaM2()).thenReturn(new BigDecimal("1540.50"));
        Mockito.when(slabRepository.getTotalAreaByStatus(SlabStatus.AVAILABLE)).thenReturn(new BigDecimal("1200.00"));
        Mockito.when(slabRepository.getTotalAreaByStatus(SlabStatus.RESERVED)).thenReturn(new BigDecimal("340.50"));
        Mockito.when(blockRepository.getCountFactoryStock()).thenReturn(18L);
        Mockito.when(blockRepository.getTotalFactoryStockWeightKg()).thenReturn(36000.0);
        Mockito.when(projectRepository.countByStatus(ProjectStatus.ACTIVE)).thenReturn(4L);
        Mockito.when(scrapLogRepository.getTotalScrapCostImpact()).thenReturn(new BigDecimal("28500.00"));
        Mockito.when(scrapLogRepository.getScrapSummaryByReason()).thenReturn(List.<Object[]>of(new Object[]{"FR_01", 3L, new BigDecimal("12.5")}));
        Mockito.when(userRepository.countByDeletedFalse()).thenReturn(12L);
        Mockito.when(costTransactionRepository.getCostDistributionByCenter()).thenReturn(List.<Object[]>of(new Object[]{"KATRAK", new BigDecimal("45000")}));

        DashboardKpiDto result = dashboardService.getDashboardKpis();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getTotalSlabArea()).isEqualTo(new BigDecimal("1540.50"));
        Assertions.assertThat(result.getAvailableSlabArea()).isEqualTo(new BigDecimal("1200.00"));
        Assertions.assertThat(result.getReservedSlabArea()).isEqualTo(new BigDecimal("340.50"));
        Assertions.assertThat(result.getFactoryBlockCount()).isEqualTo(18L);
        Assertions.assertThat(result.getFactoryBlockWeightTon()).isEqualTo(36.0);
        Assertions.assertThat(result.getActiveProjectsCount()).isEqualTo(4L);
        Assertions.assertThat(result.getTotalScrapImpact()).isEqualTo(new BigDecimal("28500.00"));
        Assertions.assertThat(result.getTotalUsers()).isEqualTo(12L);
        Assertions.assertThat(result.getScrapSummary()).hasSize(1);
        Assertions.assertThat(result.getScrapSummary().get(0).getCount()).isEqualTo(3L);
        Assertions.assertThat(result.getScrapSummary().get(0).getTotalWeightKg()).isEqualTo(new BigDecimal("12.5"));
        Assertions.assertThat(result.getCostDistribution()).hasSize(1);
        Assertions.assertThat(result.getCostDistribution().get(0).getCostCenterName()).isEqualTo("KATRAK");
        Assertions.assertThat(result.getCostDistribution().get(0).getTotalAmount()).isEqualTo(new BigDecimal("45000"));
    }
}
