package com.ozerler.marble.service;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @Mock
    private ProductionOrderRepository productionOrderRepository;
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
                productionOrderRepository, null, null, slabRepository, scrapLogRepository, barcodeService, null, null);
    }

    @Test
    @DisplayName("getOrdersPaged maps database slab aggregates instead of loading child collections")
    void getOrdersPagedUsesDatabaseAggregates() {
        ProductionOrder order = sampleOrder(7L, "PO-2026-001");
        Mockito.when(
                        productionOrderRepository.searchOrders(
                                ArgumentMatchers.any(), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        Mockito.when(productionOrderRepository.aggregateSlabMetrics(List.of(7L)))
                .thenReturn(List.of(aggregate(7L, 12L, new BigDecimal("48.5000"))));

        TabulatorResponse<ProductionOrderDto> response = productionService.getOrdersPaged(1, 10, null, null, null);

        Assertions.assertThat(response.getData()).hasSize(1);
        ProductionOrderDto dto = response.getData().getFirst();
        Assertions.assertThat(dto.getOrderNo()).isEqualTo("PO-2026-001");
        Assertions.assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        Assertions.assertThat(dto.getStatusLabel()).isEqualTo("Tamamlandı");
        Assertions.assertThat(dto.getSlabCount()).isEqualTo(12);
        Assertions.assertThat(dto.getTotalSlabAreaM2()).isEqualByComparingTo("48.5000");
        Assertions.assertThat(order.getSlabs()).isEmpty();
    }

    @Test
    @DisplayName("getOrdersPaged skips aggregate query when the page is empty")
    void getOrdersPagedEmptyPageSkipsAggregateQuery() {
        Mockito.when(
                        productionOrderRepository.searchOrders(
                                ArgumentMatchers.any(), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty());

        TabulatorResponse<ProductionOrderDto> response = productionService.getOrdersPaged(1, 10, null, null, null);

        Assertions.assertThat(response.getData()).isEmpty();
        Mockito.verify(productionOrderRepository, Mockito.never())
                .aggregateSlabMetrics(ArgumentMatchers.anyCollection());
    }

    @Test
    @DisplayName("getOrdersPaged retries with createdDate when the grid sort field is not persistable")
    void getOrdersPagedUnknownSortFieldFallsBackToCreatedDate() {
        ProductionOrder order = sampleOrder(7L, "PO-2026-001");
        Mockito.when(
                        productionOrderRepository.searchOrders(
                                ArgumentMatchers.any(), ArgumentMatchers.any(Pageable.class)))
                .thenAnswer(
                        invocation -> {
                            Pageable pageable = invocation.getArgument(1);
                            boolean unknown =
                                    pageable.getSort().stream()
                                            .anyMatch(
                                                    sortOrder ->
                                                            "notAColumn"
                                                                    .equals(
                                                                            sortOrder
                                                                                    .getProperty()));
                            if (unknown) {
                                throw new InvalidDataAccessApiUsageException(
                                        "No property 'notAColumn' found");
                            }
                            return new PageImpl<>(List.of(order));
                        });
        Mockito.when(productionOrderRepository.aggregateSlabMetrics(List.of(7L)))
                .thenReturn(List.of(aggregate(7L, 12L, new BigDecimal("48.5000"))));

        TabulatorResponse<ProductionOrderDto> response =
                productionService.getOrdersPaged(1, 10, null, "notAColumn", "asc");

        Assertions.assertThat(response.getData()).hasSize(1);
        Assertions.assertThat(response.getData().getFirst().getOrderNo()).isEqualTo("PO-2026-001");
    }

    @Test
    @DisplayName("getOrderWithDetails returns the order with child collections initialized")
    void getOrderWithDetailsReturnsOrder() {
        ProductionOrder order = sampleOrder(7L, "PO-2026-001");
        Mockito.when(productionOrderRepository.findWithDetailsById(7L)).thenReturn(Optional.of(order));

        ProductionOrder result = productionService.getOrderWithDetails(7L);

        Assertions.assertThat(result.getOrderNo()).isEqualTo("PO-2026-001");
        Assertions.assertThat(result.getSlabs()).isEmpty();
        Assertions.assertThat(result.getScrapLogs()).isEmpty();
        Mockito.verify(productionOrderRepository).findWithDetailsById(7L);
    }

    @Test
    @DisplayName("getOrderWithDetails throws when the production order is missing")
    void getOrderWithDetailsMissingOrderThrows() {
        Mockito.when(productionOrderRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> productionService.getOrderWithDetails(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getOrderWithDetails rejects a null id")
    void getOrderWithDetailsNullIdThrows() {
        Assertions.assertThatThrownBy(() -> productionService.getOrderWithDetails(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("getSlabWithDetails returns the slab")
    void getSlabWithDetailsReturnsSlab() {
        Slab slab = Slab.builder().id(4L).slabCode("SLB-001").build();
        Mockito.when(slabRepository.findWithDetailsById(4L)).thenReturn(Optional.of(slab));

        Slab result = productionService.getSlabWithDetails(4L);

        Assertions.assertThat(result.getSlabCode()).isEqualTo("SLB-001");
        Mockito.verify(slabRepository).findWithDetailsById(4L);
    }

    @Test
    @DisplayName("updateSlab writes identity, dimensions, finish, and status then saves")
    void updateSlabUpdatesFieldsRecalculatesAreaAndSaves() {
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
        Mockito.when(slabRepository.findById(4L)).thenReturn(Optional.of(existing));
        Mockito.when(slabRepository.save(ArgumentMatchers.any(Slab.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

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

        Assertions.assertThat(updated.getSlabCode()).isEqualTo("SLB-2026-00851");
        Assertions.assertThat(updated.getWidthCm()).isEqualByComparingTo("175.00");
        Assertions.assertThat(updated.getLengthCm()).isEqualByComparingTo("285.00");
        Assertions.assertThat(updated.getSurfaceAreaM2()).isEqualByComparingTo("4.9875");
        Assertions.assertThat(updated.getSurfaceFinish()).isEqualTo(SurfaceFinish.POLISHED);
        Assertions.assertThat(updated.getQualityGrade()).isEqualTo(QualityGrade.A);
        Assertions.assertThat(updated.getGlossLevel()).isEqualTo(88);
        Assertions.assertThat(updated.getCostPerM2()).isEqualByComparingTo("1185.57");
        Assertions.assertThat(updated.getStatus()).isEqualTo(SlabStatus.RESERVED);
        Mockito.verify(slabRepository).save(existing);
    }

    @Test
    @DisplayName("updateSlab throws when the slab does not exist")
    void updateSlabMissingSlabThrows() {
        Mockito.when(slabRepository.findById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> productionService.updateSlab(
                99L, "SLB-1", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                SurfaceFinish.RAW, QualityGrade.A, 0, BigDecimal.ONE, SlabStatus.AVAILABLE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("getSlabWithDetails throws when the slab is missing")
    void getSlabWithDetailsMissingSlabThrows() {
        Mockito.when(slabRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> productionService.getSlabWithDetails(99L))
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
