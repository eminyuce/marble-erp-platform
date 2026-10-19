package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.BackupItemDto;
import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.exception.StorageException;
import com.ozerler.marble.exception.StoredFileNotFoundException;
import com.ozerler.marble.service.DatabaseBackupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/settings/backups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class BackupAdminController {

    private final DatabaseBackupService backupService;

    @GetMapping
    public String backupTabRedirect() {
        return "redirect:/admin/settings?tab=backups";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<BackupItemDto>> getBackupsApi(HttpServletRequest request) {
        return ResponseEntity.ok(backupService.listBackups(request.getServerName()));
    }

    @GetMapping("/api/status")
    @ResponseBody
    public ResponseEntity<BackupOperationStatus> getStatusApi() {
        return ResponseEntity.ok(backupService.getStatus());
    }

    @PostMapping("/trigger")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerBackup() {
        try {
            backupService.createBackupAsync();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Yedekleme işlemi başlatıldı. Tamamlandığında listede görünecektir."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Yedek başlatılamadı: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Yedek başlatılırken bir hata oluştu: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<StreamingResponseBody> downloadBackup(@PathVariable String fileName, HttpServletRequest request) {
        Optional<String> presigned = backupService.browserPresignedUrl(fileName, request.getServerName());
        if (presigned.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, presigned.get())
                    .cacheControl(CacheControl.noStore())
                    .body(output -> output.flush());
        }
        DatabaseBackupService.BackupContent content = backupService.openDownload(fileName);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, attachmentDisposition(content.fileName()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore());
        if (content.sizeBytes() >= 0) {
            response.contentLength(content.sizeBytes());
        }
        return response.body(output -> backupService.writeBackup(content.body(), output, content.fileName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> invalidBackupFileName(IllegalArgumentException ex) {
        return jsonError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(StoredFileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> missingBackup(StoredFileNotFoundException ex) {
        return jsonError(HttpStatus.NOT_FOUND, "Yedek dosyası bulunamadı.");
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> backupStorageFailure(StorageException ex) {
        log.error("Backup storage request failed: {}", ex.getMessage());
        return jsonError(HttpStatus.BAD_GATEWAY, "Yedek deposuna erişilemedi. Lütfen tekrar deneyin.");
    }

    private static String attachmentDisposition(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private static ResponseEntity<Map<String, Object>> jsonError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("success", false, "message", message));
    }

    @PostMapping("/restore")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> triggerRestore(@RequestParam("fileName") String fileName,
                                                              @RequestParam("confirmCode") String confirmCode) {
        if (confirmCode == null || !"ONAYLIYORUM".equalsIgnoreCase(confirmCode.trim())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Güvenlik onay kodu hatalı! Geri yükleme başlatılamadı. Lütfen 'ONAYLIYORUM' yazınız."
            ));
        }

        try {
            backupService.restoreBackupAsync(fileName);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", fileName + " yedeğini geri yükleme işlemi başlatıldı."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Geri yükleme başlatılamadı: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Geri yükleme başlatılırken hata oluştu: " + e.getMessage()
            ));
        }
    }
}
