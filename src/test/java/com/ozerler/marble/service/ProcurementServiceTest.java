package com.ozerler.marble.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.PurchaseOrderStatus;
import com.ozerler.marble.model.enums.SupplierType;
import com.ozerler.marble.repository.CostCenterRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.PurchaseOrderRepository;
import com.ozerler.marble.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private CostCenterRepository costCenterRepository;

    private ProcurementService procurementService;

    @BeforeEach
    void setUp() {
        procurementService = new ProcurementService(
                purchaseOrderRepository, null, null, null, supplierRepository, projectRepository, null,
                expenseService, costCenterRepository);
    }

    @Test
    @DisplayName("purchase orders require a business unit")
    void createPurchaseOrderMissingUnitThrows() {
        Assertions.assertThatThrownBy(() -> procurementService.createPurchaseOrder(
                "SIP-1", 1L, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        Mockito.verify(purchaseOrderRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("site purchases require a project")
    void createPurchaseOrderSiteWithoutProjectThrows() {
        Assertions.assertThatThrownBy(() -> procurementService.createPurchaseOrder(
                "SIP-1", 1L, null, BusinessUnit.SITE, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        Mockito.verify(purchaseOrderRepository, Mockito.never()).save(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("confirming a purchase posts the total to the unit cost center")
    void confirmOrderPostsExpenseToBusinessUnit() {
        Supplier supplier =
                Supplier.builder()
                        .id(1L)
                        .companyName("Tedarik")
                        .supplierType(SupplierType.CONSUMABLE)
                        .build();
        Project project = Project.builder().id(3L).name("Şantiye A").build();
        PurchaseOrder order = PurchaseOrder.builder()
                .id(10L)
                .poNumber("SIP-10")
                .supplier(supplier)
                .project(project)
                .businessUnit(BusinessUnit.SITE)
                .status(PurchaseOrderStatus.DRAFT)
                .totalAmount(new BigDecimal("2500"))
                .build();
        CostCenter siteCenter =
                CostCenter.builder()
                        .id(5L)
                        .code("CC-005")
                        .name("Şantiyeler")
                        .businessUnit(BusinessUnit.SITE)
                        .build();

        Mockito.when(purchaseOrderRepository.findWithDetailsById(10L)).thenReturn(Optional.of(order));
        Mockito.when(costCenterRepository.findFirstByBusinessUnitOrderByCodeAsc(BusinessUnit.SITE))
                .thenReturn(Optional.of(siteCenter));
        Mockito.when(purchaseOrderRepository.save(ArgumentMatchers.any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        procurementService.updateStatus(10L, PurchaseOrderStatus.CONFIRMED);

        ArgumentCaptor<ExpenseService.ExpenseDraft> expense =
                ArgumentCaptor.forClass(ExpenseService.ExpenseDraft.class);
        Mockito.verify(expenseService).recordExpense(expense.capture());
        Assertions.assertThat(expense.getValue().businessUnit()).isEqualTo(BusinessUnit.SITE);
        Assertions.assertThat(expense.getValue().centerId()).isEqualTo(5L);
        Assertions.assertThat(expense.getValue().amount()).isEqualByComparingTo("2500");
        Assertions.assertThat(expense.getValue().project()).isEqualTo(project);
        Assertions.assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.CONFIRMED);
    }
}
