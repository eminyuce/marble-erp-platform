package com.ozerler.marble.service;

import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.OperationWorkOrderItem;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import com.ozerler.marble.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationWorkOrderLineItemsTest {

    @Mock
    private OperationWorkOrderRepository workOrderRepository;
    @Mock
    private WorkOrderStatusHistoryRepository historyRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private StockItemRepository stockItemRepository;
    @Mock
    private SlabRepository slabRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private StockMovementService stockMovementService;
    @Mock
    private OperationDefinitionRepository definitionRepository;

    private OperationWorkOrderService workOrderService;

    @BeforeEach
    void setUp() {
        workOrderService = new OperationWorkOrderService(
                workOrderRepository,
                historyRepository,
                customerRepository,
                stockItemRepository,
                slabRepository,
                blockRepository,
                stockMovementService,
                definitionRepository
        );
    }

    @Test
    @DisplayName("OperationWorkOrderItem automatically computes m2 and mt metrics")
    void testItemMetricCalculation() {
        OperationWorkOrderItem item = OperationWorkOrderItem.builder()
                .widthCm(new BigDecimal("60"))
                .lengthCm(new BigDecimal("120"))
                .thicknessCm(new BigDecimal("2.0"))
                .pieceCount(10)
                .surfaceOperation("Cilalı")
                .surfaceUnit("m2")
                .edgeOperation("Pah")
                .edgeUnit("m.t.")
                .build();

        item.computeMetrics();

        // 60 * 120 * 10 / 10000 = 7.2000 m2
        assertThat(item.getCalculatedM2()).isEqualByComparingTo("7.2000");
        // 120 * 10 / 100 = 12.0000 mt
        assertThat(item.getCalculatedMt()).isEqualByComparingTo("12.0000");
        assertThat(item.getSurfaceOperation()).isEqualTo("Cilalı");
        assertThat(item.getSurfaceUnit()).isEqualTo("m2");
        assertThat(item.getEdgeOperation()).isEqualTo("Pah");
        assertThat(item.getEdgeUnit()).isEqualTo("m.t.");
    }

    @Test
    @DisplayName("createWorkOrder persists line items and links them to work order")
    void testCreateWorkOrderWithItems() {
        Customer customer = Customer.builder().id(5L).companyName("Test Müşteri").build();
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        when(workOrderRepository.save(any(OperationWorkOrder.class))).thenAnswer(inv -> {
            OperationWorkOrder wo = inv.getArgument(0);
            wo.setId(99L);
            return wo;
        });

        OperationWorkOrderItem item1 = OperationWorkOrderItem.builder()
                .widthCm(new BigDecimal("30"))
                .lengthCm(new BigDecimal("60"))
                .thicknessCm(new BigDecimal("2.0"))
                .pieceCount(20)
                .surfaceOperation("Honlu")
                .surfaceUnit("m2")
                .edgeOperation("Düz")
                .edgeUnit("m.t.")
                .build();

        OperationWorkOrderService.WorkOrderCreateForm form = new OperationWorkOrderService.WorkOrderCreateForm(
                "FWO-2026-TEST",
                5L,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                BusinessUnit.FACTORY,
                "Ustabaşı",
                "Muğla Beyazı",
                "1. Kalite",
                new BigDecimal("2.0"),
                new BigDecimal("30"),
                new BigDecimal("60"),
                new BigDecimal("3.6"),
                "m2",
                "Honlu",
                "Düz",
                null, null, null, null,
                "Detaylı iş emri",
                List.of(item1)
        );

        OperationWorkOrder created = workOrderService.createWorkOrder(form);

        assertThat(created).isNotNull();
        assertThat(created.getItems()).hasSize(1);
        assertThat(created.getItems().get(0).getWorkOrder()).isEqualTo(created);
        assertThat(created.getItems().get(0).getSurfaceOperation()).isEqualTo("Honlu");
        assertThat(created.getItems().get(0).getEdgeUnit()).isEqualTo("m.t.");

        ArgumentCaptor<OperationWorkOrder> captor = ArgumentCaptor.forClass(OperationWorkOrder.class);
        verify(workOrderRepository).save(captor.capture());
        assertThat(captor.getValue().getItems()).isNotEmpty();
    }

    @Test
    @DisplayName("updateStatus successfully approves work order and records history")
    void testUpdateStatusToApproved() {
        OperationWorkOrder existing = OperationWorkOrder.builder()
                .id(10L)
                .orderNo("WO-APP-001")
                .status(OperationWorkOrderStatus.NEW)
                .build();

        when(workOrderRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(existing));
        when(workOrderRepository.save(any(OperationWorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        OperationWorkOrder updated = workOrderService.updateStatus(10L, OperationWorkOrderStatus.APPROVED, "Müşteri onayladı");

        assertThat(updated.getStatus()).isEqualTo(OperationWorkOrderStatus.APPROVED);
        verify(historyRepository).save(any());
        verify(workOrderRepository).save(existing);
    }

    @Test
    @DisplayName("getOrderById fetches order and safely initializes proxies")
    void testGetOrderByIdWithDetails() {
        Customer c = Customer.builder().id(1L).companyName("Acme Mermer").build();
        OperationWorkOrder order = OperationWorkOrder.builder()
                .id(20L)
                .orderNo("WO-DET-001")
                .customer(c)
                .status(OperationWorkOrderStatus.APPROVED)
                .build();

        when(workOrderRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(order));

        OperationWorkOrder fetched = workOrderService.getOrderById(20L);

        assertThat(fetched).isNotNull();
        assertThat(fetched.getCustomerDisplayName()).isEqualTo("Acme Mermer");
        verify(workOrderRepository).findByIdWithDetails(20L);
    }
}
