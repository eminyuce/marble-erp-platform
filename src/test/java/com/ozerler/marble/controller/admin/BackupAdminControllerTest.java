package com.ozerler.marble.controller.admin;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.service.DatabaseBackupService;

@SpringBootTest
@ActiveProfiles("test")
class BackupAdminControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DatabaseBackupService backupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN can access backup API list and status")
    void adminCanAccessBackupApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/api/list"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("application/json"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/api/status"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.state").exists());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN can trigger backup")
    void adminCanTriggerBackup() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/admin/settings/backups/trigger")
                                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Restore rejects invalid confirm code")
    void restoreRejectsInvalidConfirmCode() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/settings/backups/restore")
                        .param("fileName", "backup_test.sql.gz")
                        .param("confirmCode", "wrong_code")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    @DisplayName("Regular user without ROLE_ADMIN is forbidden from backup management")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/api/list"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/admin/settings/backups/trigger")
                                .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/download/backup_missing.sql.gz"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN downloads a stored backup as a gzip stream")
    void adminDownloadsStoredBackup() throws Exception {
        awaitIdle();
        String fileName = backupService.createBackupAsync().get(60, TimeUnit.SECONDS);

        MvcResult started =
                mockMvc.perform(
                                MockMvcRequestBuilders.get(
                                        "/admin/settings/backups/download/" + fileName))
                        .andReturn();
        MvcResult result;
        if (started.getRequest().isAsyncStarted()) {
            result = mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(started)).andReturn();
        } else {
            result = started;
        }

        Assertions.assertThat(result.getResponse().getStatus())
                .withFailMessage(
                        "status=%s body=%s",
                        result.getResponse().getStatus(), result.getResponse().getContentAsString())
                .isEqualTo(200);
        Assertions.assertThat(result.getResponse().getContentType()).contains("application/gzip");
        Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains(fileName);
        Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");

        byte[] body = result.getResponse().getContentAsByteArray();
        Assertions.assertThat(body).startsWith((byte) 0x1f, (byte) 0x8b);
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
            String text = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            Assertions.assertThat(text).contains("Özerler Mermer ERP Database Backup");
        }
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Missing backup download returns 404")
    void missingBackupReturnsNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/download/backup_missing.sql.gz"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Unsafe backup file name is rejected")
    void unsafeBackupFileNameIsRejected() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/download/not-a-backup.txt"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false));
    }

    private void awaitIdle() throws InterruptedException {
        for (int attempt = 0; attempt < 200; attempt++) {
            if (backupService.getStatus().getState() != BackupOperationStatus.State.RUNNING) {
                return;
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Backup operation did not finish");
    }
}
