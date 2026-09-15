package com.ozerler.marble.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class DefinitionGridApiTest {

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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("definition list APIs return Tabulator JSON envelopes")
    void definitionApisReturnTabulatorJson() throws Exception {
        assertTabulatorEnvelope("/admin/definitions/suppliers/api/data");
        assertTabulatorEnvelope("/admin/definitions/customers/api/data");
        assertTabulatorEnvelope("/admin/definitions/machines/api/data");
        assertTabulatorEnvelope("/admin/definitions/stock-locations/api/data");
        assertTabulatorEnvelope("/admin/definitions/quarries/api/data");
        assertTabulatorEnvelope("/admin/definitions/cost-centers/api/data");
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("non-admin users cannot read definition grid APIs")
    void userCannotReadDefinitionApis() throws Exception {
        mockMvc.perform(get("/admin/definitions/suppliers/api/data").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private void assertTabulatorEnvelope(String path) throws Exception {
        mockMvc.perform(get(path).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.last_page").isNumber())
                .andExpect(jsonPath("$.total").isNumber());
    }
}
