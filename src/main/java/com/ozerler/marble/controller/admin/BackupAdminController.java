package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.BackupItemDto;
import com.ozerler.marble.dto.BackupOperationStatus;
import com.ozerler.marble.service.DatabaseBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<List<BackupItemDto>> getBackupsApi() {
        return ResponseEntity.ok(backupService.listBackups());
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

    @GetMapping("/download/{fileName}")
    public String downloadBackup(@PathVariable String fileName) {
        String downloadUrl = backupService.generatePresignedDownloadUrl(fileName);
        return "redirect:" + downloadUrl;
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
