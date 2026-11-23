package com.ozerler.marble.service;


import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
                quarryRepository, null, new TaskExecutorAdapter(task -> task.run()));
    }

    @Test
    @DisplayName("search returns empty without querying repositories for short terms")
    void searchShortQueryDoesNotHitRepositories() {
        GlobalSearchResponse response = globalSearchService.search("a");

        Assertions.assertThat(response.getTotal()).isZero();
        Assertions.assertThat(response.getResults()).isEmpty();
        Mockito.verify(blockRepository, Mockito.never()).searchByBlockCode(Mockito.any(), Mockito.any());
        Mockito.verify(projectRepository, Mockito.never()).searchByCodeOrName(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("search fans out across all entity repositories and merges hits")
    void searchMergesHitsFromAllSources() {
        stubEmptySearches();
        Mockito.when(blockRepository.searchByBlockCode(Mockito.eq("blok"), Mockito.any(Pageable.class)))
                .thenReturn(List.of(Block.builder().blockCode("BLK-100").stoneType("Beyaz").build()));
        Mockito.when(projectRepository.searchByCodeOrName(Mockito.eq("blok"), Mockito.any(Pageable.class)))
                .thenReturn(List.of(Project.builder()
                        .id(4L)
                        .projectCode("PRJ-4")
                        .name("Blok Villa")
                        .customerName("Acme")
                        .build()));

        GlobalSearchResponse response = globalSearchService.search("blok");

        Assertions.assertThat(response.getTotal()).isEqualTo(2);
        Assertions.assertThat(response.getResults()).extracting(hit -> hit.getType())
                .containsExactlyInAnyOrder("BLOCK", "PROJECT");
        Mockito.verify(slabRepository).searchBySlabCode(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(productionOrderRepository).searchByOrderNo(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(cutOrderRepository).searchByCutOrderNo(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(salesOrderRepository).searchByOrderNo(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(purchaseOrderRepository).searchByPoNumber(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(userRepository)
                .searchActiveUsers(
                        Mockito.eq("blok"),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.any(Pageable.class));
        Mockito.verify(customerRepository).searchByCodeOrName(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(supplierRepository).searchByCodeOrName(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(costCenterRepository).searchByCodeOrName(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(cutItemRepository).searchByItemCode(Mockito.eq("blok"), Mockito.any(Pageable.class));
        Mockito.verify(quarryRepository).searchByCodeOrName(Mockito.eq("blok"), Mockito.any(Pageable.class));
    }

    @Test
    @DisplayName("search unwraps repository failures from worker threads")
    void searchPropagatesRepositoryFailure() {
        stubEmptySearches();
        Mockito.when(blockRepository.searchByBlockCode(Mockito.eq("xx"), Mockito.any(Pageable.class)))
                .thenThrow(new IllegalStateException("blok araması başarısız"));

        Assertions.assertThatThrownBy(() -> globalSearchService.search("xx"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("blok araması başarısız");
    }

    private void stubEmptySearches() {
        Mockito.when(blockRepository.searchByBlockCode(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(projectRepository.searchByCodeOrName(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(slabRepository.searchBySlabCode(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(productionOrderRepository.searchByOrderNo(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(cutOrderRepository.searchByCutOrderNo(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(salesOrderRepository.searchByOrderNo(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(purchaseOrderRepository.searchByPoNumber(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(
                        userRepository.searchActiveUsers(
                                Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.any()))
                .thenReturn(Page.empty());
        Mockito.when(customerRepository.searchByCodeOrName(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(supplierRepository.searchByCodeOrName(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(costCenterRepository.searchByCodeOrName(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(cutItemRepository.searchByItemCode(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(quarryRepository.searchByCodeOrName(Mockito.any(), Mockito.any())).thenReturn(List.of());
    }
}
