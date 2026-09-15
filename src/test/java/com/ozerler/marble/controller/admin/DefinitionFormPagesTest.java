package com.ozerler.marble.controller.admin;

import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.MachineType;
import com.ozerler.marble.service.MasterDataService;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class DefinitionFormPagesTest {

    private static final List<String> LIST_PAGES = List.of(
            "src/main/resources/templates/admin/definitions/machines.html",
            "src/main/resources/templates/admin/definitions/stock-locations.html",
            "src/main/resources/templates/admin/definitions/quarries.html",
            "src/main/resources/templates/admin/definitions/customers.html",
            "src/main/resources/templates/admin/definitions/suppliers.html",
            "src/main/resources/templates/admin/definitions/cost-centers.html"
    );

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MasterDataService masterDataService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("definition lists open dedicated create/edit pages instead of Alpine modals")
    void listPagesLinkToFormPagesInsteadOfModals() throws Exception {
        for (String page : LIST_PAGES) {
            String source = Files.readString(Path.of(page));
            assertThat(source)
                    .as("%s must not open a create/edit modal", page)
                    .doesNotContain("openCreateModal")
                    .doesNotContain("openEditModal")
                    .doesNotContain("x-show=\"modalOpen\"")
                    .contains("/create")
                    .contains("/edit");
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("admin can open definition create and edit form pages")
    void adminCanOpenCreateAndEditForms() throws Exception {
        mockMvc.perform(get("/admin/definitions/machines/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/definitions/machine-form"));
        Machine saved = masterDataService.saveMachine(Machine.builder()
                .code("TST-FORM-01")
                .name("Form test makinesi")
                .businessUnit(BusinessUnit.FACTORY)
                .machineType(MachineType.GANGSAW)
                .active(true)
                .build());
        mockMvc.perform(get("/admin/definitions/machines/" + saved.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/definitions/machine-form"));
        mockMvc.perform(get("/admin/definitions/customers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/definitions/customer-form"));
        mockMvc.perform(get("/admin/definitions/suppliers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/definitions/supplier-form"));
        mockMvc.perform(get("/admin/definitions/stock-locations/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/definitions/stock-location-form"));
        mockMvc.perform(get("/admin/definitions/quarries/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/definitions/quarry-form"));
        mockMvc.perform(get("/admin/definitions/cost-centers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/definitions/cost-center-form"));
        mockMvc.perform(get("/admin/definitions/machines/999999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/definitions/machines"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("non-admin users cannot open definition form pages")
    void userCannotOpenDefinitionForms() throws Exception {
        mockMvc.perform(get("/admin/definitions/machines/create")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/definitions/machines/1/edit")).andExpect(status().isForbidden());
    }
}
