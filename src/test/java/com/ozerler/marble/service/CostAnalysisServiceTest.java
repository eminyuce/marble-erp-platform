package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.ozerler.marble.dto.CostAnalysisDto;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.FactoryOperationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.WorkshopOperationRepository;

@ExtendWith(MockitoExtension.class)
class CostAnalysisServiceTest {

    @Mock
    private CostTransactionRepository costTransactionRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private FactoryOperationRepository factoryOperationRepository;
    @Mock
    private WorkshopOperationRepository workshopOperationRepository;
    @Mock
    private ProjectRepository projectRepository;

    private CostAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new CostAnalysisService(costTransactionRepository, blockRepository,
                factoryOperationRepository, workshopOperationRepository, projectRepository);
    }

    @Test
    @DisplayName("factory m2 cost includes incoming quarry block extraction cost")
    void factoryAnalysisIncludesIncomingBlockCost() {
        Mockito.when(
                        costTransactionRepository.sumByUnitAndPeriod(
                                ArgumentMatchers.eq(BusinessUnit.FACTORY),
                                ArgumentMatchers.eq("2026-09")))
                .thenReturn(new BigDecimal("1000"));
        Mockito.when(
                        costTransactionRepository.sumByUnitAndPeriod(
                                ArgumentMatchers.eq(BusinessUnit.FACTORY),
                                ArgumentMatchers.eq("2026-08")))
                .thenReturn(BigDecimal.ZERO);
        Mockito.when(factoryOperationRepository.aggregateQuantitiesByProcessType()).thenReturn(List.of());
        Mockito.when(
                        costTransactionRepository.sumByCategoryForUnitAndPeriod(
                                ArgumentMatchers.eq(BusinessUnit.FACTORY),
                                ArgumentMatchers.eq("2026-09")))
                .thenReturn(List.of());
        Mockito.when(
                        costTransactionRepository.findByBusinessUnitAndExpensePeriod(
                                ArgumentMatchers.eq(BusinessUnit.FACTORY),
                                ArgumentMatchers.eq("2026-09")))
                .thenReturn(List.of());
        Mockito.when(blockRepository.sumExtractionCostMovedTo(ArgumentMatchers.eq(StockLocationType.FACTORY_BLOCK_YARD),
                ArgumentMatchers.any(LocalDateTime.class), ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("400"));

        CostAnalysisDto withBlocks = service.analyze(BusinessUnit.FACTORY, "2026-09");

        Mockito.when(blockRepository.sumExtractionCostMovedTo(ArgumentMatchers.eq(StockLocationType.FACTORY_BLOCK_YARD),
                ArgumentMatchers.any(LocalDateTime.class), ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        CostAnalysisDto withoutBlocks = service.analyze(BusinessUnit.FACTORY, "2026-09");

        Assertions.assertThat(withBlocks.getIncomingBlockCost()).isEqualByComparingTo("400");
        Assertions.assertThat(withBlocks.getTotalExpense()).isEqualByComparingTo("1400");
        Assertions.assertThat(withoutBlocks.getTotalExpense()).isEqualByComparingTo("1000");
        Assertions.assertThat(withBlocks.getTotalExpense()).isNotEqualByComparingTo(withoutBlocks.getTotalExpense());
    }
}
