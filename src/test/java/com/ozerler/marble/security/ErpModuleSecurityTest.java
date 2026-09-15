package com.ozerler.marble.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("authenticated users can open quarry, factory, workshop, site and cost screens")
    void authenticatedGetIsAllowed() throws Exception {
        mockMvc.perform(get("/blocks")).andExpect(status().isOk());
        mockMvc.perform(get("/production")).andExpect(status().isOk());
        mockMvc.perform(get("/production/polish")).andExpect(status().isOk());
        mockMvc.perform(get("/production/pallets")).andExpect(status().isOk());
        mockMvc.perform(get("/production/tablet")).andExpect(status().isOk());
        mockMvc.perform(get("/workshop")).andExpect(status().isOk());
        mockMvc.perform(get("/projects")).andExpect(status().isOk());
        mockMvc.perform(get("/costs")).andExpect(status().isOk());
        mockMvc.perform(get("/quarry")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/blocks"));
        mockMvc.perform(get("/factory")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/production"));
        mockMvc.perform(get("/sites")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/projects"));
        mockMvc.perform(get("/cost-analysis")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/costs"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("non-admin users cannot open master data definitions")
    void userCannotOpenDefinitions() throws Exception {
        mockMvc.perform(get("/admin/definitions")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/definitions/machines")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/definitions/machines/create")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("admin can open master data definition lists")
    void adminCanOpenDefinitions() throws Exception {
        mockMvc.perform(get("/admin/definitions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/definitions/machines"));
        mockMvc.perform(get("/admin/definitions/machines")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("operator cannot record finance expenses or quarry fuel")
    void operatorCannotWriteFinanceOrQuarry() throws Exception {
        mockMvc.perform(post("/costs/expenses").with(csrf())
                        .param("centerId", "1")
                        .param("expenseType", "DIESEL")
                        .param("businessUnit", "QUARRY")
                        .param("amount", "10"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/blocks/fuel").with(csrf())
                        .param("machineId", "1")
                        .param("litres", "10")
                        .param("pricePerLitre", "40"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/reports")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "QUARRY_CHIEF")
    @DisplayName("quarry chief cannot open factory tablet planning writes")
    void quarryChiefCannotPlanFactory() throws Exception {
        mockMvc.perform(post("/production/tablet/plan").with(csrf())
                        .param("workOrderId", "1")
                        .param("processType", "GANGSAW_CUTTING"))
                .andExpect(status().isForbidden());
    }
}
