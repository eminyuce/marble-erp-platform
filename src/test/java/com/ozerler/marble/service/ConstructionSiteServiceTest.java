package com.ozerler.marble.service;

import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.model.ConstructionSiteStonePlan;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.ProjectLocation;
import com.ozerler.marble.model.SiteInstallation;
import com.ozerler.marble.model.SiteSupplyAllocation;
import com.ozerler.marble.repository.ConstructionSiteStonePlanRepository;
import com.ozerler.marble.repository.CostTransactionRepository;
import com.ozerler.marble.repository.CustomerRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.repository.MaterialLotRepository;
import com.ozerler.marble.repository.PalletRepository;
import com.ozerler.marble.repository.ProjectLocationRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.SiteConsumptionRepository;
import com.ozerler.marble.repository.SiteInstallationRepository;
import com.ozerler.marble.repository.SiteSupplyAllocationRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockReservationRepository;

@ExtendWith(MockitoExtension.class)
class ConstructionSiteServiceTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectLocationRepository projectLocationRepository;
    @Mock
    private ConstructionSiteStonePlanRepository stonePlanRepository;
    @Mock
    private SiteSupplyAllocationRepository allocationRepository;
    @Mock
    private SiteInstallationRepository installationRepository;
    @Mock
    private SiteConsumptionRepository consumptionRepository;
    @Mock
    private CostTransactionRepository costTransactionRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StockReservationRepository stockReservationRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private MaterialLotRepository materialLotRepository;
    @Mock
    private PalletRepository palletRepository;
    @Mock
    private ProjectSiteService projectSiteService;
    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private ConstructionSiteService constructionSiteService;

    @Test
    @DisplayName("deleteProject throws IllegalStateException when project has associated invoices")
    void deleteProject_WithAssociatedInvoices_ThrowsException() {
        Long projectId = 100L;
        Project project = Project.builder().id(projectId).projectCode("PRJ-100").build();
        Mockito.when(projectSiteService.getProjectById(projectId)).thenReturn(project);
        Mockito.when(invoiceRepository.existsByProjectId(projectId)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> constructionSiteService.deleteProject(projectId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fatura kayıtları bulunmaktadır");

        Mockito.verify(projectRepository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    @DisplayName("deleteProject cleans up all related records and deletes project successfully")
    void deleteProject_NoInvoices_Success() {
        Long projectId = 200L;
        Project project = Project.builder().id(projectId).projectCode("PRJ-200").build();
        Mockito.when(projectSiteService.getProjectById(projectId)).thenReturn(project);
        Mockito.when(invoiceRepository.existsByProjectId(projectId)).thenReturn(false);

        SiteInstallation installation = SiteInstallation.builder().id(10L).project(project).build();
        Mockito.when(installationRepository.findByProjectIdOrderByInstalledOnDesc(projectId))
                .thenReturn(List.of(installation));

        ConstructionSiteStonePlan plan = ConstructionSiteStonePlan.builder().id(20L).project(project).build();
        Mockito.when(stonePlanRepository.findByProjectIdOrderByIdAsc(projectId))
                .thenReturn(List.of(plan));

        SiteSupplyAllocation allocation = SiteSupplyAllocation.builder().id(30L).stonePlan(plan).build();
        Mockito.when(allocationRepository.findByStonePlanId(20L))
                .thenReturn(List.of(allocation));

        ProjectLocation parentLoc = ProjectLocation.builder().id(40L).project(project).build();
        ProjectLocation childLoc = ProjectLocation.builder().id(41L).project(project).parent(parentLoc).build();
        Mockito.when(projectLocationRepository.findByProjectId(projectId))
                .thenReturn(List.of(parentLoc, childLoc));

        constructionSiteService.deleteProject(projectId);

        Mockito.verify(installationRepository).deleteAll(List.of(installation));
        Mockito.verify(allocationRepository).deleteAll(List.of(allocation));
        Mockito.verify(stonePlanRepository).deleteAll(List.of(plan));
        Mockito.verify(stockReservationRepository).deleteAll(Mockito.anyList());
        Mockito.verify(consumptionRepository).deleteAll(Mockito.anyList());
        Mockito.verify(costTransactionRepository).deleteAll(Mockito.anyList());
        Mockito.verify(projectLocationRepository).saveAll(Mockito.anyList());
        Mockito.verify(projectLocationRepository).deleteAll(Mockito.anyList());
        Mockito.verify(projectRepository).delete(project);
        Assertions.assertThat(childLoc.getParent()).isNull();
    }
}
