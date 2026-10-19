package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.service.DatabaseBackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        mockMvc.perform(get("/admin/settings/backups/download/backup_missing.sql.gz"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN downloads a stored backup as a gzip stream")
    void adminDownloadsStoredBackup() throws Exception {
        awaitIdle();
        String fileName = backupService.createBackupAsync().get(60, TimeUnit.SECONDS);

        MvcResult started = mockMvc.perform(get("/admin/settings/backups/download/" + fileName)).andReturn();
        MvcResult result = started.getRequest().isAsyncStarted()
                ? mockMvc.perform(asyncDispatch(started)).andReturn()
                : started;

        assertThat(result.getResponse().getStatus())
                .withFailMessage("status=%s body=%s", result.getResponse().getStatus(), result.getResponse().getContentAsString())
                .isEqualTo(200);
        assertThat(result.getResponse().getContentType()).contains("application/gzip");
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains(fileName);
        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");

        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body).startsWith((byte) 0x1f, (byte) 0x8b);
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
            String text = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(text).contains("Özerler Mermer ERP Database Backup");
        }
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Missing backup download returns 404")
    void missingBackupReturnsNotFound() throws Exception {
        mockMvc.perform(get("/admin/settings/backups/download/backup_missing.sql.gz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Unsafe backup file name is rejected")
    void unsafeBackupFileNameIsRejected() throws Exception {
        mockMvc.perform(get("/admin/settings/backups/download/not-a-backup.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
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
