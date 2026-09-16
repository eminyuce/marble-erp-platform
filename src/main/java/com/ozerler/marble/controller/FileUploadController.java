package com.ozerler.marble.controller;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.FileStorageDto;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController extends AbstractController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody BackEndResponse uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "entityType", required = false, defaultValue = "BLOCK") String entityType,
            @RequestParam(value = "entityId", required = false) Long entityId) {

        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Uploading file {} for entityType={} entityId={}", file.getOriginalFilename(), entityType, entityId);
            FileStorage saved = fileStorageService.storeFile(file, entityType, entityId);
            FileStorageDto dto = FileStorageDto.fromEntity(saved);

            HttpHeaders responseHeaders = new HttpHeaders();
            ResponseEntity<FileStorageDto> resp = new ResponseEntity<>(dto, responseHeaders, HttpStatus.OK);

            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("File upload successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("File upload failed for {}", file.getOriginalFilename(), e);
            ber = buildFatalResponse(ber, serviceStatus, status, "uploadFile: " + e.getMessage(), Constants.ERR_FATAL);
        }

        return ber;
    }

    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody BackEndResponse uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "entityType", required = false, defaultValue = "BLOCK") String entityType,
            @RequestParam(value = "entityId", required = false) Long entityId) {

        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Uploading {} files for entityType={} entityId={}", files.size(), entityType, entityId);
            List<FileStorageDto> uploadedList = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    FileStorage saved = fileStorageService.storeFile(file, entityType, entityId);
                    uploadedList.add(FileStorageDto.fromEntity(saved));
                }
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            ResponseEntity<List<FileStorageDto>> resp = new ResponseEntity<>(uploadedList, responseHeaders, HttpStatus.OK);

            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Multi-file upload successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("Multi-file upload failed", e);
            ber = buildFatalResponse(ber, serviceStatus, status, "uploadMultipleFiles: " + e.getMessage(), Constants.ERR_FATAL);
        }

        return ber;
    }

    @DeleteMapping("/{id}")
    public @ResponseBody BackEndResponse deleteFileById(@PathVariable("id") Long id) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Deleting file storage record with id {}", id);
            boolean deleted = fileStorageService.deleteFileRecord(id);
            if (deleted) {
                ResponseEntity<Void> resp = ResponseEntity.ok().build();
                ber.setResponse(resp);
                serviceStatus.setHttpStatus(HttpStatus.OK);
                status.setMessage("File deleted successfully");
                serviceStatus.setStatus(status);
                ber.setServiceStatus(serviceStatus);
            } else {
                serviceStatus.setHttpStatus(HttpStatus.NOT_FOUND);
                status.setErrorCode(Constants.ERR_NOT_FOUND);
                status.setMessage("File not found to delete");
                status.addError("Not found");
                serviceStatus.setStatus(status);
                ber.setServiceStatus(serviceStatus);
            }
        } catch (Exception e) {
            log.error("A serious error occurred in deleteFileById {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "deleteFileById", Constants.ERR_FATAL);
        }

        return ber;
    }

    @DeleteMapping
    public @ResponseBody BackEndResponse revertUpload(@RequestBody String filePayload) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            String payload = filePayload != null ? filePayload.trim() : "";
            log.info("Reverting file upload with payload {}", payload);
            boolean deleted = false;

            // Check if payload is numeric ID
            if (payload.matches("^\\d+$")) {
                deleted = fileStorageService.deleteFileRecord(Long.parseLong(payload));
            } else {
                deleted = fileStorageService.deleteFile(payload);
            }

            if (deleted) {
                ResponseEntity<Void> resp = ResponseEntity.ok().build();
                ber.setResponse(resp);
                serviceStatus.setHttpStatus(HttpStatus.OK);
                status.setMessage("File deleted successfully");
                serviceStatus.setStatus(status);
                ber.setServiceStatus(serviceStatus);
            } else {
                serviceStatus.setHttpStatus(HttpStatus.NOT_FOUND);
                status.setErrorCode(Constants.ERR_NOT_FOUND);
                status.setMessage("File not found to revert");
                status.addError("Not found");
                serviceStatus.setStatus(status);
                ber.setServiceStatus(serviceStatus);
            }
        } catch (Exception e) {
            log.error("A serious error occurred in revertUpload", e);
            ber = buildFatalResponse(ber, serviceStatus, status, "revertUpload", Constants.ERR_FATAL);
        }

        return ber;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") Long id) {
        FileStorage fileStorage = fileStorageService.getFileById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı: " + id));

        Resource resource = fileStorageService.loadAsResource(id);

        String originalName = fileStorage.getOriginalName();
        String encodedFilename = URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");

        String mime = fileStorage.getMimeType();
        if (mime == null || mime.isBlank()) {
            mime = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }
}
