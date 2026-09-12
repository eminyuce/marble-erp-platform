package com.ozerler.marble.controller;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController extends AbstractController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody BackEndResponse uploadFile(@RequestParam("file") MultipartFile file) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Uploading file {}", file.getOriginalFilename());
            String fileUrl = fileStorageService.storeFile(file);

            HttpHeaders responseHeaders = new HttpHeaders();
            ResponseEntity<String> resp = new ResponseEntity<>(fileUrl, responseHeaders, HttpStatus.OK);

            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("File upload successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("File upload failed", e);
            ber = buildFatalResponse(ber, serviceStatus, status, "uploadFile", Constants.ERR_FATAL);
        }

        return ber;
    }

    @DeleteMapping
    public @ResponseBody BackEndResponse revertUpload(@RequestBody String fileUrl) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Reverting file upload for url {}", fileUrl);
            boolean deleted = fileStorageService.deleteFile(fileUrl.trim());
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
}
