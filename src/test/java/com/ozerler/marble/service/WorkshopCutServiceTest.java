package com.ozerler.marble.service;

import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.OrderChildAggregate;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopCutServiceTest {

    @Mock
    private CutOrderRepository cutOrderRepository;
    @Mock
    private CutItemRepository cutItemRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectLocationRepository projectLocationRepository;
    @Mock
    private ScrapLogRepository scrapLogRepository;

    private WorkshopCutService workshopCutService;

    @BeforeEach
    void setUp() {
        workshopCutService = new WorkshopCutService(
                cutOrderRepository, cutItemRepository, slabRepository,
                projectRepository, projectLocationRepository, scrapLogRepository
        );
    }

    @Test
    @DisplayName("createCutOrder throws IllegalArgumentException when piecesCount <= 0")
    void createCutOrder_ZeroPieces_ThrowsException() {
        assertThatThrownBy(() -> workshopCutService.createCutOrder(
                1L, 1L, 1L, "BridgeSaw-01", "Ahmet",
                0, new BigDecimal("60"), new BigDecimal("120"),
                "PAHLI", "Banyo", "Notlar"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Parça adedi");
    }

    @Test
    @DisplayName("createCutOrder throws IllegalArgumentException when target dimensions are invalid")
    void createCutOrder_InvalidDimensions_ThrowsException() {
        assertThatThrownBy(() -> workshopCutService.createCutOrder(
                1L, 1L, 1L, "BridgeSaw-01", "Ahmet",
                5, BigDecimal.ZERO, new BigDecimal("120"),
                "PAHLI", "Banyo", "Notlar"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Hedef genişlik");
    }

    @Test
    @DisplayName("getCutOrderById throws NullPointerException when ID is null")
    void getCutOrderById_NullId_ThrowsException() {
        assertThatThrownBy(() -> workshopCutService.getCutOrderById(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ID");
    }

    @Test
    @DisplayName("getCutOrdersPaged maps database item aggregates instead of loading child collections")
    void getCutOrdersPaged_UsesDatabaseAggregates() {
        CutOrder order = CutOrder.builder()
                .id(9L)
                .cutOrderNo("CO-2026-009")
                .machineName("Köprü-01")
                .status("COMPLETED")
                .build();
        when(cutOrderRepository.searchCutOrders(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(cutOrderRepository.aggregateItemMetrics(List.of(9L)))
                .thenReturn(List.of(new OrderChildAggregate() {
                    @Override
                    public Long getParentId() {
                        return 9L;
                    }

                    @Override
                    public Long getItemCount() {
                        return 4L;
                    }

                    @Override
                    public BigDecimal getTotalArea() {
                        return new BigDecimal("6.2500");
                    }
                }));

        TabulatorResponse<CutOrderDto> response = workshopCutService.getCutOrdersPaged(1, 10, null, null, null);

        assertThat(response.getData()).hasSize(1);
        CutOrderDto dto = response.getData().getFirst();
        assertThat(dto.getCutOrderNo()).isEqualTo("CO-2026-009");
        assertThat(dto.getItemCount()).isEqualTo(4);
        assertThat(dto.getTotalAreaM2()).isEqualByComparingTo("6.2500");
        assertThat(order.getItems()).isEmpty();
    }
}
