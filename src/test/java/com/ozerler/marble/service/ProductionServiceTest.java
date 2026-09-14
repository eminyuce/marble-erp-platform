package com.ozerler.marble.service;

import com.ozerler.marble.dto.OrderChildAggregate;
import com.ozerler.marble.dto.ProductionOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.enums.ProcessType;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.SlabStatus;
import com.ozerler.marble.model.enums.SurfaceFinish;
import jakarta.persistence.EntityNotFoundException;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private ScrapLogRepository scrapLogRepository;
    @Mock
    private BarcodeService barcodeService;

    private ProductionService productionService;

    @BeforeEach
    void setUp() {
        productionService = new ProductionService(
                productionOrderRepository, blockRepository, slabRepository, scrapLogRepository, barcodeService, null);
    }

    @Test
    @DisplayName("getOrdersPaged maps database slab aggregates instead of loading child collections")
    void getOrdersPaged_UsesDatabaseAggregates() {
        ProductionOrder order = sampleOrder(7L, "PO-2026-001");
        when(productionOrderRepository.searchOrders(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(productionOrderRepository.aggregateSlabMetrics(List.of(7L)))
                .thenReturn(List.of(aggregate(7L, 12L, new BigDecimal("48.5000"))));

        TabulatorResponse<ProductionOrderDto> response = productionService.getOrdersPaged(1, 10, null, null, null);

        assertThat(response.getData()).hasSize(1);
        ProductionOrderDto dto = response.getData().getFirst();
        assertThat(dto.getOrderNo()).isEqualTo("PO-2026-001");
        assertThat(dto.getSlabCount()).isEqualTo(12);
        assertThat(dto.getTotalSlabAreaM2()).isEqualByComparingTo("48.5000");
        assertThat(order.getSlabs()).isEmpty();
    }

    @Test
    @DisplayName("getOrdersPaged skips aggregate query when the page is empty")
    void getOrdersPaged_EmptyPage_SkipsAggregateQuery() {
        when(productionOrderRepository.searchOrders(any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        TabulatorResponse<ProductionOrderDto> response = productionService.getOrdersPaged(1, 10, null, null, null);

        assertThat(response.getData()).isEmpty();
        verify(productionOrderRepository, never()).aggregateSlabMetrics(anyCollection());
    }

    @Test
    @DisplayName("getOrderWithDetails returns the order with child collections initialized")
    void getOrderWithDetails_ReturnsOrder() {
        ProductionOrder order = sampleOrder(7L, "PO-2026-001");
        when(productionOrderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(order));

        ProductionOrder result = productionService.getOrderWithDetails(7L);

        assertThat(result.getOrderNo()).isEqualTo("PO-2026-001");
        assertThat(result.getSlabs()).isEmpty();
        assertThat(result.getScrapLogs()).isEmpty();
        verify(productionOrderRepository).findWithDetailsById(7L);
    }

    @Test
    @DisplayName("getOrderWithDetails throws when the production order is missing")
    void getOrderWithDetails_MissingOrder_Throws() {
        when(productionOrderRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionService.getOrderWithDetails(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getOrderWithDetails rejects a null id")
    void getOrderWithDetails_NullId_Throws() {
        assertThatThrownBy(() -> productionService.getOrderWithDetails(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getSlabWithDetails returns the slab")
    void getSlabWithDetails_ReturnsSlab() {
        Slab slab = Slab.builder().id(4L).slabCode("SLB-001").build();
        when(slabRepository.findWithDetailsById(4L)).thenReturn(Optional.of(slab));

        Slab result = productionService.getSlabWithDetails(4L);

        assertThat(result.getSlabCode()).isEqualTo("SLB-001");
        verify(slabRepository).findWithDetailsById(4L);
    }

    @Test
    @DisplayName("updateSlab writes identity, dimensions, finish, and status then saves")
    void updateSlab_UpdatesFieldsRecalculatesAreaAndSaves() {
        Slab existing = Slab.builder()
                .id(4L)
                .slabCode("SLB-OLD")
                .thicknessCm(new BigDecimal("2.00"))
                .widthCm(new BigDecimal("100.00"))
                .lengthCm(new BigDecimal("100.00"))
                .surfaceAreaM2(new BigDecimal("1.0000"))
                .surfaceFinish(SurfaceFinish.RAW)
                .qualityGrade(QualityGrade.B)
                .glossLevel(0)
                .costPerM2(new BigDecimal("100.00"))
                .status(SlabStatus.AVAILABLE)
                .build();
        when(slabRepository.findById(4L)).thenReturn(Optional.of(existing));
        when(slabRepository.save(any(Slab.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Slab updated = productionService.updateSlab(
                4L,
                "SLB-2026-00851",
                new BigDecimal("2.00"),
                new BigDecimal("175.00"),
                new BigDecimal("285.00"),
                SurfaceFinish.POLISHED,
                QualityGrade.A,
                88,
                new BigDecimal("1185.57"),
                SlabStatus.RESERVED
        );

        assertThat(updated.getSlabCode()).isEqualTo("SLB-2026-00851");
        assertThat(updated.getWidthCm()).isEqualByComparingTo("175.00");
        assertThat(updated.getLengthCm()).isEqualByComparingTo("285.00");
        assertThat(updated.getSurfaceAreaM2()).isEqualByComparingTo("4.9875");
        assertThat(updated.getSurfaceFinish()).isEqualTo(SurfaceFinish.POLISHED);
        assertThat(updated.getQualityGrade()).isEqualTo(QualityGrade.A);
        assertThat(updated.getGlossLevel()).isEqualTo(88);
        assertThat(updated.getCostPerM2()).isEqualByComparingTo("1185.57");
        assertThat(updated.getStatus()).isEqualTo(SlabStatus.RESERVED);
        verify(slabRepository).save(existing);
    }

    @Test
    @DisplayName("updateSlab throws when the slab does not exist")
    void updateSlab_MissingSlab_Throws() {
        when(slabRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionService.updateSlab(
                99L, "SLB-1", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                SurfaceFinish.RAW, QualityGrade.A, 0, BigDecimal.ONE, SlabStatus.AVAILABLE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("getSlabWithDetails throws when the slab is missing")
    void getSlabWithDetails_MissingSlab_Throws() {
        when(slabRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productionService.getSlabWithDetails(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private static ProductionOrder sampleOrder(Long id, String orderNo) {
        Block block = Block.builder()
                .id(3L)
                .blockCode("BLK-001")
                .stoneType("Crema Marfil")
                .build();
        return ProductionOrder.builder()
                .id(id)
                .orderNo(orderNo)
                .block(block)
                .machineName("Katrak-01")
                .processType(ProcessType.GANGSAW)
                .startTime(LocalDateTime.of(2026, 3, 1, 8, 0))
                .status("COMPLETED")
                .build();
    }

    private static OrderChildAggregate aggregate(Long parentId, Long itemCount, BigDecimal totalArea) {
        return new OrderChildAggregate() {
            @Override
            public Long getParentId() {
                return parentId;
            }

            @Override
            public Long getItemCount() {
                return itemCount;
            }

            @Override
            public BigDecimal getTotalArea() {
                return totalArea;
            }
        };
    }
}
