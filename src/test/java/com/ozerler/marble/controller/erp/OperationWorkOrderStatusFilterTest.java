package com.ozerler.marble.controller.erp;

import java.time.LocalDate;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.view.WorkOrderView;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import com.ozerler.marble.service.FormLookupService;
import com.ozerler.marble.service.OperationWorkOrderService;

@ExtendWith(MockitoExtension.class)
class OperationWorkOrderStatusFilterTest {

    @Mock
    private FormLookupService formLookup;

    @Mock
    private OperationWorkOrderService workOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new OperationWorkOrderController(formLookup, workOrderService)).build();
    }

    @Test
    @DisplayName("GET /work-orders?status=DRAFT returns the list and ignores the unknown status")
    void indexIgnoresDraftStatus() throws Exception {
        stubEmptyList();

        mockMvc.perform(MockMvcRequestBuilders.get("/work-orders").param("status", "DRAFT"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("selectedStatus", ""))
                .andExpect(MockMvcResultMatchers.model().attribute("statuses",
                        Matchers.arrayContaining(OperationWorkOrderStatus.values())));

        Mockito.verify(workOrderService).searchOrderRows(
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.eq(0),
                Mockito.eq(20));
    }

    @Test
    @DisplayName("GET /work-orders?status=READY_FOR_SHIPMENT returns the list and ignores the unknown status")
    void indexIgnoresReadyForShipmentStatus() throws Exception {
        stubEmptyList();

        mockMvc.perform(MockMvcRequestBuilders.get("/work-orders").param("status", "READY_FOR_SHIPMENT"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("selectedStatus", ""));

        Mockito.verify(workOrderService).searchOrderRows(
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.eq(0),
                Mockito.eq(20));
    }

    @Test
    @DisplayName("GET /work-orders?status=IN_PRODUCTION still filters by a valid status")
    void indexAcceptsValidStatus() throws Exception {
        stubEmptyList();

        mockMvc.perform(MockMvcRequestBuilders.get("/work-orders").param("status", "IN_PRODUCTION"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.model().attribute("selectedStatus", "IN_PRODUCTION"));

        Mockito.verify(workOrderService).searchOrderRows(
                Mockito.isNull(),
                Mockito.eq(OperationWorkOrderStatus.IN_PRODUCTION),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.eq(0),
                Mockito.eq(20));
    }

    @Test
    @DisplayName("GET /work-orders/api/data ignores DRAFT and READY_FOR_SHIPMENT status params")
    void apiDataIgnoresUnknownStatuses() throws Exception {
        Mockito.when(workOrderService.orderRows(
                        Mockito.any(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.eq(1),
                        Mockito.eq(25)))
                .thenReturn(TabulatorResponse.of(List.of(), 1, 0));

        mockMvc.perform(MockMvcRequestBuilders.get("/work-orders/api/data").param("status", "DRAFT"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/work-orders/api/data").param("status", "READY_FOR_SHIPMENT"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        Mockito.verify(workOrderService, Mockito.times(2)).orderRows(
                Mockito.any(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.eq(1),
                Mockito.eq(25));
    }

    private void stubEmptyList() {
        Mockito.when(formLookup.customers()).thenReturn(List.of());
        Mockito.when(workOrderService.searchOrderRows(
                        ArgumentMatchers.nullable(BusinessUnit.class),
                        ArgumentMatchers.nullable(OperationWorkOrderStatus.class),
                        ArgumentMatchers.nullable(Long.class),
                        ArgumentMatchers.nullable(LocalDate.class),
                        ArgumentMatchers.nullable(LocalDate.class),
                        ArgumentMatchers.nullable(String.class),
                        Mockito.anyInt(),
                        Mockito.anyInt()))
                .thenReturn(new PageImpl<>(List.<WorkOrderView>of()));
    }
}
