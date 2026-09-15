package com.ozerler.marble.service;

import com.ozerler.marble.dto.CutOrderDto;
import com.ozerler.marble.dto.OrderChildAggregate;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.CutItem;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.repository.*;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
                projectRepository, projectLocationRepository, scrapLogRepository, null
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
        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getStatusLabel()).isEqualTo("Tamamlandı");
        assertThat(dto.getItemCount()).isEqualTo(4);
        assertThat(dto.getTotalAreaM2()).isEqualByComparingTo("6.2500");
        assertThat(order.getItems()).isEmpty();
    }

    @Test
    @DisplayName("getCutOrderWithDetails returns the cut order")
    void getCutOrderWithDetails_ReturnsOrder() {
        CutOrder order = CutOrder.builder().id(3L).cutOrderNo("CO-2026-003").status("COMPLETED").build();
        when(cutOrderRepository.findWithDetailsById(3L)).thenReturn(Optional.of(order));

        CutOrder result = workshopCutService.getCutOrderWithDetails(3L);

        assertThat(result.getCutOrderNo()).isEqualTo("CO-2026-003");
        verify(cutOrderRepository).findWithDetailsById(3L);
    }

    @Test
    @DisplayName("getCutOrderWithDetails throws when the cut order is missing")
    void getCutOrderWithDetails_MissingOrder_Throws() {
        when(cutOrderRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopCutService.getCutOrderWithDetails(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateCutOrder updates header fields and existing item metadata")
    void updateCutOrder_UpdatesHeaderAndItems() {
        Project existingProject = Project.builder().id(1L).name("Eski").build();
        Project newProject = Project.builder().id(2L).name("Villa").build();
        CutOrder order = CutOrder.builder()
                .id(5L)
                .cutOrderNo("CUT-2026-005")
                .project(existingProject)
                .machineName("Köprü-01")
                .operatorName("Ali")
                .status("COMPLETED")
                .notes("Eski not")
                .build();
        CutItem item = CutItem.builder()
                .id(11L)
                .itemCode("ITM-1")
                .cutOrder(order)
                .edgeFinish("HAM")
                .targetLocation("Depo")
                .build();
        when(cutOrderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(newProject));
        when(cutItemRepository.findByCutOrderId(5L)).thenReturn(List.of(item));
        when(cutOrderRepository.save(order)).thenReturn(order);

        CutOrder result = workshopCutService.updateCutOrder(
                5L, 2L, "Köprü-02", "Ayşe", "PAHLI", "Lobi", "Yeni not");

        assertThat(result.getProject()).isEqualTo(newProject);
        assertThat(result.getMachineName()).isEqualTo("Köprü-02");
        assertThat(result.getOperatorName()).isEqualTo("Ayşe");
        assertThat(result.getNotes()).isEqualTo("Yeni not");
        assertThat(item.getEdgeFinish()).isEqualTo("PAHLI");
        assertThat(item.getTargetLocation()).isEqualTo("Lobi");
        verify(cutItemRepository).saveAll(List.of(item));
        verify(cutOrderRepository).save(order);
    }

    @Test
    @DisplayName("updateCutOrder clears the project and skips item writes when metadata is blank")
    void updateCutOrder_ClearsProjectWithoutTouchingItems() {
        CutOrder order = CutOrder.builder()
                .id(8L)
                .cutOrderNo("CUT-2026-008")
                .project(Project.builder().id(3L).name("Villa").build())
                .machineName("Köprü-01")
                .operatorName("Ali")
                .status("COMPLETED")
                .build();
        when(cutOrderRepository.findById(8L)).thenReturn(Optional.of(order));
        when(cutOrderRepository.save(order)).thenReturn(order);

        CutOrder result = workshopCutService.updateCutOrder(8L, null, "Köprü-03", "Can", "  ", "", "Not");

        assertThat(result.getProject()).isNull();
        assertThat(result.getMachineName()).isEqualTo("Köprü-03");
        assertThat(result.getOperatorName()).isEqualTo("Can");
        verify(cutItemRepository, never()).findByCutOrderId(8L);
        verify(cutItemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("updateCutOrder throws when the cut order is missing")
    void updateCutOrder_MissingOrder_Throws() {
        when(cutOrderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopCutService.updateCutOrder(
                99L, null, "Köprü-01", "Ali", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
