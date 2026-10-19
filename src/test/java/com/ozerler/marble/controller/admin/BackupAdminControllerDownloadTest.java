package com.ozerler.marble.controller.admin;

import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import com.ozerler.marble.service.DatabaseBackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        when(backupService.browserPresignedUrl(eq("backup_test.sql.gz"), eq("localhost")))
                .thenReturn(Optional.of(location));

        var started = mockMvc.perform(get("/admin/settings/backups/download/backup_test.sql.gz")).andReturn();
        var result = started.getRequest().isAsyncStarted()
                ? mockMvc.perform(asyncDispatch(started)).andReturn()
                : started;

        assertThat(result.getResponse().getStatus()).isEqualTo(302);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo(location);
        assertThat(result.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");

        verify(backupService, never()).openDownload(any());
    }

    @Test
    @DisplayName("internal RustFS streams the backup through the application")
    void downloadStreamsWhenPresignedUrlIsUnavailable() throws Exception {
        byte[] payload = new byte[] {1, 2, 3, 4};
        when(backupService.browserPresignedUrl(eq("backup_test.sql.gz"), eq("localhost")))
                .thenReturn(Optional.empty());
        when(backupService.openDownload("backup_test.sql.gz"))
                .thenReturn(new DatabaseBackupService.BackupContent(
                        "backup_test.sql.gz", payload.length, "application/gzip", new ByteArrayInputStream(payload)));
        doAnswer(invocation -> {
            try (InputStream input = invocation.getArgument(0)) {
                OutputStream output = invocation.getArgument(1);
                input.transferTo(output);
            }
            return null;
        }).when(backupService).writeBackup(any(), any(), eq("backup_test.sql.gz"));

        var started = mockMvc.perform(get("/admin/settings/backups/download/backup_test.sql.gz")).andReturn();
        var result = started.getRequest().isAsyncStarted()
                ? mockMvc.perform(asyncDispatch(started)).andReturn()
                : started;

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("backup_test.sql.gz");
        assertThat(result.getResponse().getContentAsByteArray()).containsExactly(payload);
    }

    @Test
    @DisplayName("missing backup returns 404")
    void downloadMissingFile() throws Exception {
        when(backupService.browserPresignedUrl(eq("backup_missing.sql.gz"), eq("localhost")))
                .thenThrow(new StoredFileNotFoundException("missing"));

        mockMvc.perform(get("/admin/settings/backups/download/backup_missing.sql.gz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Yedek dosyası bulunamadı."));
    }

    @Test
    @DisplayName("storage outage returns 502 and does not expose the internal endpoint")
    void downloadStorageFailure() throws Exception {
        when(backupService.browserPresignedUrl(eq("backup_test.sql.gz"), eq("localhost")))
                .thenReturn(Optional.empty());
        when(backupService.openDownload("backup_test.sql.gz"))
                .thenThrow(new StorageException("Unable to download the requested file"));

        mockMvc.perform(get("/admin/settings/backups/download/backup_test.sql.gz"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("rustfs"))));
    }
}
