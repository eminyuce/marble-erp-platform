package com.ozerler.marble.service;

import com.ozerler.marble.dto.GlobalSearchResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.CutItemRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.ProductionOrderRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.PurchaseOrderRepository;
import com.ozerler.marble.repository.QuarryRepository;
import com.ozerler.marble.repository.SalesOrderRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.SupplierRepository;
import com.ozerler.marble.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private CutOrderRepository cutOrderRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private CostCenterRepository costCenterRepository;
    @Mock
    private CutItemRepository cutItemRepository;
    @Mock
    private QuarryRepository quarryRepository;

    private GlobalSearchService globalSearchService;

    @BeforeEach
    void setUp() {
        globalSearchService = new GlobalSearchService(
                blockRepository, projectRepository, slabRepository, productionOrderRepository,
                cutOrderRepository, salesOrderRepository, purchaseOrderRepository, userRepository,
                customerRepository, supplierRepository, costCenterRepository, cutItemRepository,
                quarryRepository, null, new TaskExecutorAdapter(Runnable::run));
    }

    @Test
    @DisplayName("search returns empty without querying repositories for short terms")
    void search_ShortQuery_DoesNotHitRepositories() {
        GlobalSearchResponse response = globalSearchService.search("a");

        assertThat(response.getTotal()).isZero();
        assertThat(response.getResults()).isEmpty();
        verify(blockRepository, never()).searchByBlockCode(any(), any());
        verify(projectRepository, never()).searchByCodeOrName(any(), any());
    }

    @Test
    @DisplayName("search fans out across all entity repositories and merges hits")
    void search_MergesHitsFromAllSources() {
        stubEmptySearches();
        when(blockRepository.searchByBlockCode(eq("blok"), any(Pageable.class)))
                .thenReturn(List.of(Block.builder().blockCode("BLK-100").stoneType("Beyaz").build()));
        when(projectRepository.searchByCodeOrName(eq("blok"), any(Pageable.class)))
                .thenReturn(List.of(Project.builder()
                        .id(4L)
                        .projectCode("PRJ-4")
                        .name("Blok Villa")
                        .customerName("Acme")
                        .build()));

        GlobalSearchResponse response = globalSearchService.search("blok");

        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getResults()).extracting(hit -> hit.getType())
                .containsExactlyInAnyOrder("BLOCK", "PROJECT");
        verify(slabRepository).searchBySlabCode(eq("blok"), any(Pageable.class));
        verify(productionOrderRepository).searchByOrderNo(eq("blok"), any(Pageable.class));
        verify(cutOrderRepository).searchByCutOrderNo(eq("blok"), any(Pageable.class));
        verify(salesOrderRepository).searchByOrderNo(eq("blok"), any(Pageable.class));
        verify(purchaseOrderRepository).searchByPoNumber(eq("blok"), any(Pageable.class));
        verify(userRepository).searchActiveUsers(eq("blok"), isNull(), isNull(), any(Pageable.class));
        verify(customerRepository).searchByCodeOrName(eq("blok"), any(Pageable.class));
        verify(supplierRepository).searchByCodeOrName(eq("blok"), any(Pageable.class));
        verify(costCenterRepository).searchByCodeOrName(eq("blok"), any(Pageable.class));
        verify(cutItemRepository).searchByItemCode(eq("blok"), any(Pageable.class));
        verify(quarryRepository).searchByCodeOrName(eq("blok"), any(Pageable.class));
    }

    @Test
    @DisplayName("search unwraps repository failures from worker threads")
    void search_PropagatesRepositoryFailure() {
        stubEmptySearches();
        when(blockRepository.searchByBlockCode(eq("xx"), any(Pageable.class)))
                .thenThrow(new IllegalStateException("blok araması başarısız"));

        assertThatThrownBy(() -> globalSearchService.search("xx"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("blok araması başarısız");
    }

    private void stubEmptySearches() {
        when(blockRepository.searchByBlockCode(any(), any())).thenReturn(List.of());
        when(projectRepository.searchByCodeOrName(any(), any())).thenReturn(List.of());
        when(slabRepository.searchBySlabCode(any(), any())).thenReturn(List.of());
        when(productionOrderRepository.searchByOrderNo(any(), any())).thenReturn(List.of());
        when(cutOrderRepository.searchByCutOrderNo(any(), any())).thenReturn(List.of());
        when(salesOrderRepository.searchByOrderNo(any(), any())).thenReturn(List.of());
        when(purchaseOrderRepository.searchByPoNumber(any(), any())).thenReturn(List.of());
        when(userRepository.searchActiveUsers(any(), isNull(), isNull(), any())).thenReturn(Page.empty());
        when(customerRepository.searchByCodeOrName(any(), any())).thenReturn(List.of());
        when(supplierRepository.searchByCodeOrName(any(), any())).thenReturn(List.of());
        when(costCenterRepository.searchByCodeOrName(any(), any())).thenReturn(List.of());
        when(cutItemRepository.searchByItemCode(any(), any())).thenReturn(List.of());
        when(quarryRepository.searchByCodeOrName(any(), any())).thenReturn(List.of());
    }
}
