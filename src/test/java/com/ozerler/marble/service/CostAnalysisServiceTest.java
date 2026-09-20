package com.ozerler.marble.service;

import com.ozerler.marble.dto.CostAnalysisDto;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.FactoryOperationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.WorkshopOperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        when(costTransactionRepository.sumByUnitAndPeriod(eq(BusinessUnit.FACTORY), eq("2026-09")))
                .thenReturn(new BigDecimal("1000"));
        when(costTransactionRepository.sumByUnitAndPeriod(eq(BusinessUnit.FACTORY), eq("2026-08")))
                .thenReturn(BigDecimal.ZERO);
        when(factoryOperationRepository.aggregateQuantitiesByProcessType()).thenReturn(List.of());
        when(costTransactionRepository.sumByCategoryForUnitAndPeriod(eq(BusinessUnit.FACTORY), eq("2026-09")))
                .thenReturn(List.of());
        when(costTransactionRepository.findByBusinessUnitAndExpensePeriod(eq(BusinessUnit.FACTORY), eq("2026-09")))
                .thenReturn(List.of());
        when(blockRepository.sumExtractionCostMovedTo(eq(StockLocationType.FACTORY_BLOCK_YARD),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("400"));

        CostAnalysisDto withBlocks = service.analyze(BusinessUnit.FACTORY, "2026-09");

        when(blockRepository.sumExtractionCostMovedTo(eq(StockLocationType.FACTORY_BLOCK_YARD),
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);
        CostAnalysisDto withoutBlocks = service.analyze(BusinessUnit.FACTORY, "2026-09");

        assertThat(withBlocks.getIncomingBlockCost()).isEqualByComparingTo("400");
        assertThat(withBlocks.getTotalExpense()).isEqualByComparingTo("1400");
        assertThat(withoutBlocks.getTotalExpense()).isEqualByComparingTo("1000");
        assertThat(withBlocks.getTotalExpense()).isNotEqualByComparingTo(withoutBlocks.getTotalExpense());
    }
}
