package com.ozerler.marble.controller.admin;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class BackupAdminControllerTest {

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
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN can access backup API list and status")
    void adminCanAccessBackupApi() throws Exception {
        mockMvc.perform(get("/admin/settings/backups/api/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        mockMvc.perform(get("/admin/settings/backups/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").exists());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN can trigger backup")
    void adminCanTriggerBackup() throws Exception {
        mockMvc.perform(post("/admin/settings/backups/trigger").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Restore rejects invalid confirm code")
    void restoreRejectsInvalidConfirmCode() throws Exception {
        mockMvc.perform(post("/admin/settings/backups/restore")
                        .param("fileName", "backup_test.sql.gz")
                        .param("confirmCode", "wrong_code")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    @DisplayName("Regular user without ROLE_ADMIN is forbidden from backup management")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/settings/backups/api/list"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/settings/backups/trigger").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
