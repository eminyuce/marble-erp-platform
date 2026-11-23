package com.ozerler.marble.controller.admin;

import org.springframework.test.web.servlet.MvcResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import com.ozerler.marble.service.DatabaseBackupService;

@ExtendWith(MockitoExtension.class)
class BackupAdminControllerDownloadTest {

    @Mock
    private DatabaseBackupService backupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BackupAdminController(backupService)).build();
    }

    @Test
    @DisplayName("reachable RustFS redirects to the exact presigned URL")
    void downloadRedirectsToPresignedUrl() throws Exception {
        String location = "https://files.example.com/erp-files/backups/backup_test.sql.gz"
                + "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=key%2F20261019%2Fus-east-1%2Fs3%2Faws4_request"
                + "&X-Amz-Signature=abc";
        Mockito.when(backupService.browserPresignedUrl(ArgumentMatchers.eq("backup_test.sql.gz"), ArgumentMatchers.eq("localhost")))
                .thenReturn(Optional.of(location));

        var started = mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/download/backup_test.sql.gz")).andReturn();
        MvcResult result;
        if (started.getRequest().isAsyncStarted()) {
            result = mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(started)).andReturn();
        } else {
            result = started;
        }

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(302);
        Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(location);
        Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");

        Mockito.verify(backupService, Mockito.never()).openDownload(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("internal RustFS streams the backup through the application")
    void downloadStreamsWhenPresignedUrlIsUnavailable() throws Exception {
        byte[] payload = new byte[] {1, 2, 3, 4};
        Mockito.when(backupService.browserPresignedUrl(ArgumentMatchers.eq("backup_test.sql.gz"), ArgumentMatchers.eq("localhost")))
                .thenReturn(Optional.empty());
        Mockito.when(backupService.openDownload("backup_test.sql.gz"))
                .thenReturn(new DatabaseBackupService.BackupContent(
                        "backup_test.sql.gz", payload.length, "application/gzip", new ByteArrayInputStream(payload)));
        Mockito.doAnswer(invocation -> {
            try (InputStream input = invocation.getArgument(0)) {
                OutputStream output = invocation.getArgument(1);
                input.transferTo(output);
            }
            return null;
        }).when(backupService).writeBackup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq("backup_test.sql.gz"));

        var started = mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/download/backup_test.sql.gz")).andReturn();
        MvcResult result;
        if (started.getRequest().isAsyncStarted()) {
            result = mockMvc.perform(MockMvcRequestBuilders.asyncDispatch(started)).andReturn();
        } else {
            result = started;
        }

        Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("backup_test.sql.gz");
        Assertions.assertThat(result.getResponse().getContentAsByteArray()).containsExactly(payload);
    }

    @Test
    @DisplayName("missing backup returns 404")
    void downloadMissingFile() throws Exception {
        Mockito.when(backupService.browserPresignedUrl(ArgumentMatchers.eq("backup_missing.sql.gz"), ArgumentMatchers.eq("localhost")))
                .thenThrow(new StoredFileNotFoundException("missing"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/download/backup_missing.sql.gz"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Yedek dosyası bulunamadı."));
    }

    @Test
    @DisplayName("storage outage returns 502 and does not expose the internal endpoint")
    void downloadStorageFailure() throws Exception {
        Mockito.when(backupService.browserPresignedUrl(ArgumentMatchers.eq("backup_test.sql.gz"), ArgumentMatchers.eq("localhost")))
                .thenReturn(Optional.empty());
        Mockito.when(backupService.openDownload("backup_test.sql.gz"))
                .thenThrow(new StorageException("Unable to download the requested file"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings/backups/download/backup_test.sql.gz"))
                .andExpect(MockMvcResultMatchers.status().isBadGateway())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("rustfs"))));
    }
}
