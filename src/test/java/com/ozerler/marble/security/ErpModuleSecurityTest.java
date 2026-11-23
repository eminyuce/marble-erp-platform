package com.ozerler.marble.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ErpModuleSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("authenticated users can open quarry, factory, workshop, site and cost screens")
    void authenticatedGetIsAllowed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/blocks")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/production")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/production/accept")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/production/polish")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/production/pallets")).andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/operations/factory/sized"));
        mockMvc.perform(MockMvcRequestBuilders.get("/production/tablet")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/workshop")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/workshop/receipts")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/projects")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/costs")).andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/quarry")).andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/blocks"));
        mockMvc.perform(MockMvcRequestBuilders.get("/factory")).andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/production"));
        mockMvc.perform(MockMvcRequestBuilders.get("/sites")).andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/projects"));
        mockMvc.perform(MockMvcRequestBuilders.get("/cost-analysis")).andExpect(MockMvcResultMatchers.status().is3xxRedirection()).andExpect(MockMvcResultMatchers.redirectedUrl("/costs"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("non-admin users cannot open master data definitions")
    void userCannotOpenDefinitions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/definitions")).andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/definitions/machines")).andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/definitions/machines/create")).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("admin can open master data definition lists")
    void adminCanOpenDefinitions() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/definitions"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/definitions/machines"));
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/definitions/machines")).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("operator cannot record finance expenses or quarry fuel")
    void operatorCannotWriteFinanceOrQuarry() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/costs/expenses").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("centerId", "1")
                        .param("expenseType", "DIESEL")
                        .param("businessUnit", "QUARRY")
                        .param("amount", "10"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.post("/expenses").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("businessUnit", "QUARRY")
                        .param("centerId", "1")
                        .param("expenseType", "DIESEL")
                        .param("amount", "10")
                        .param("entryDate", "2026-09-16"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get("/reports")).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("a plain user cannot create an invoice")
    void userCannotCreateInvoice() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/invoices/new").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("invoiceDate", "2026-11-23")
                        .param("invoiceType", "SALES")
                        .param("department", "FACTORY"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "QUARRY_CHIEF")
    @DisplayName("quarry chief cannot open factory tablet planning writes")
    void quarryChiefCannotPlanFactory() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/production/tablet/plan").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("workOrderId", "1")
                        .param("processType", "GANGSAW_CUTTING"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}
