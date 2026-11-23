package com.ozerler.marble.controller;


import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.PresignedDownload;
import com.ozerler.marble.exception.FileAccessDeniedException;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.FileStorageService;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @Test
    @DisplayName("uploadFile should return success response with FileStorageDto")
    void uploadFileSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "image-bytes".getBytes());
        FileStorage storage = FileStorage.builder()
                .id(1L)
                .fileName("uuid-photo.jpg")
                .originalName("photo.jpg")
                .mimeType("image/jpeg")
                .fileSize(11L)
                .filePath("/uploads/uuid-photo.jpg")
                .entityType("BLOCK")
                .build();

        Mockito.when(fileStorageService.storeFileView(Mockito.eq(file), Mockito.eq("BLOCK"), Mockito.isNull())).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));

        BackEndResponse response = fileUploadController.uploadFile(file, "BLOCK", null);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
    }

    @Test
    @DisplayName("uploadMultipleFiles should return list of FileStorageDtos")
    void uploadMultipleFilesSuccess() throws IOException {
        MockMultipartFile f1 = new MockMultipartFile("files", "img1.png", "image/png", "img1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "doc1.pdf", "application/pdf", "pdf1".getBytes());

        FileStorage s1 = FileStorage.builder().id(10L).fileName("s1.png").originalName("img1.png").mimeType("image/png").build();
        FileStorage s2 = FileStorage.builder().id(11L).fileName("s2.pdf").originalName("doc1.pdf").mimeType("application/pdf").build();

        Mockito.when(fileStorageService.storeFileView(Mockito.eq(f1), Mockito.eq("BLOCK"), Mockito.eq(100L))).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(s1));
        Mockito.when(fileStorageService.storeFileView(Mockito.eq(f2), Mockito.eq("BLOCK"), Mockito.eq(100L))).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(s2));

        BackEndResponse response = fileUploadController.uploadMultipleFiles(List.of(f1, f2), "BLOCK", 100L);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getResponse().getBody()).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).hasSize(2);
    }

    @Test
    @DisplayName("deleteFileById should delete file and return OK status")
    void deleteFileByIdSuccess() {
        Mockito.when(fileStorageService.deleteFileRecord(25L)).thenReturn(true);

        BackEndResponse response = fileUploadController.deleteFileById(25L);

        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Mockito.verify(fileStorageService).deleteFileRecord(25L);
    }

    @Test
    @DisplayName("deleteFileById should return 404 when file is not found")
    void deleteFileByIdNotFound() {
        Mockito.when(fileStorageService.deleteFileRecord(999L)).thenReturn(false);

        BackEndResponse response = fileUploadController.deleteFileById(999L);

        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("revertUpload with numeric ID should delegate to deleteFileRecord")
    void revertUploadNumericId() {
        Mockito.when(fileStorageService.deleteFileRecord(50L)).thenReturn(true);

        BackEndResponse response = fileUploadController.revertUpload("50");

        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Mockito.verify(fileStorageService).deleteFileRecord(50L);
    }

    @Test
    @DisplayName("revertUpload with URL should delegate to deleteFile")
    void revertUploadFileUrl() {
        Mockito.when(fileStorageService.deleteFile("/uploads/test.jpg")).thenReturn(true);

        BackEndResponse response = fileUploadController.revertUpload("/uploads/test.jpg");

        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Mockito.verify(fileStorageService).deleteFile("/uploads/test.jpg");
    }

    @Test
    @DisplayName("downloadFile should return ResponseEntity with Resource and attachment header")
    void downloadFileSuccess() {
        FileStorage storage = FileStorage.builder()
                .id(5L)
                .fileName("uuid-rep.pdf")
                .originalName("Rapor 2026.pdf")
                .mimeType("application/pdf")
                .build();

        ByteArrayResource resource = new ByteArrayResource("pdf-content".getBytes());

        Mockito.when(fileStorageService.requireAccessibleFileView(5L)).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));
        Mockito.when(fileStorageService.loadAsResource(5L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(5L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentDisposition().toString()).contains("Rapor 2026.pdf");
        Assertions.assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve DOCX filename with Turkish characters")
    void downloadFileDocxPreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(6L)
                .fileName("blocks_12345678.docx")
                .originalName("Mermer Analiz Raporu (Şantiye & Ocak).docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();

        ByteArrayResource resource = new ByteArrayResource("fake-docx-data".getBytes());

        Mockito.when(fileStorageService.requireAccessibleFileView(6L)).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));
        Mockito.when(fileStorageService.loadAsResource(6L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(6L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("Mermer Analiz Raporu (Şantiye & Ocak).docx");
        Assertions.assertThat(response.getHeaders().getContentDisposition().getType())
                .isEqualTo("attachment");
        Assertions.assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve TXT filename")
    void downloadFileTxtPreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(7L)
                .fileName("blocks_98765432.txt")
                .originalName("jeoloji_notlari.txt")
                .mimeType("text/plain")
                .build();

        ByteArrayResource resource = new ByteArrayResource("notlar...".getBytes());

        Mockito.when(fileStorageService.requireAccessibleFileView(7L)).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));
        Mockito.when(fileStorageService.loadAsResource(7L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(7L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("jeoloji_notlari.txt");
        Assertions.assertThat(response.getHeaders().getContentType().toString())
                .contains("text/plain");
        Assertions.assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("viewFile should open Markdown inline as readable text")
    void viewFileMarkdownOpensInlineAsText() {
        FileStorage storage = FileStorage.builder()
                .id(11L)
                .fileName("blocks_11111111.md")
                .originalName("jeoloji_notu.md")
                .mimeType("text/markdown")
                .build();

        ByteArrayResource resource = new ByteArrayResource("# Ocak notu".getBytes());

        Mockito.when(fileStorageService.requireAccessibleFileView(11L)).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));
        Mockito.when(fileStorageService.loadAsResource(11L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.viewFile(11L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
        Assertions.assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("jeoloji_notu.md");
        Assertions.assertThat(response.getHeaders().getContentType().toString()).contains("text/plain");
        Assertions.assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve XLSX filename and spreadsheet MIME type")
    void downloadFileXlsxPreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(8L)
                .fileName("blocks_88888888.xlsx")
                .originalName("Blok_Analiz_Raporu_2026.xlsx")
                .mimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .build();

        ByteArrayResource resource = new ByteArrayResource("fake-xlsx-bytes".getBytes());

        Mockito.when(fileStorageService.requireAccessibleFileView(8L)).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));
        Mockito.when(fileStorageService.loadAsResource(8L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(8L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("Blok_Analiz_Raporu_2026.xlsx");
        Assertions.assertThat(response.getHeaders().getContentType().toString())
                .contains("spreadsheetml.sheet");
        Assertions.assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve CSV filename and text/csv MIME type")
    void downloadFileCsvPreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(9L)
                .fileName("blocks_99999999.csv")
                .originalName("blok_verileri.csv")
                .mimeType("text/csv")
                .build();

        ByteArrayResource resource = new ByteArrayResource("id,code\n11,BLK-11".getBytes());

        Mockito.when(fileStorageService.requireAccessibleFileView(9L)).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));
        Mockito.when(fileStorageService.loadAsResource(9L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(9L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("blok_verileri.csv");
        Assertions.assertThat(response.getHeaders().getContentType().toString())
                .contains("text/csv");
        Assertions.assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("createDownloadUrl should return the presigned URL and its expiry")
    void createDownloadUrlSuccess() {
        Instant expiresAt = Instant.parse("2026-11-23T12:05:00Z");
        Mockito.when(fileStorageService.createPresignedDownload(12L))
                .thenReturn(new PresignedDownload(
                        "http://localhost:9000/erp-files/documents/block/1/a.pdf?X-Amz-Expires=300",
                        300,
                        expiresAt));

        BackEndResponse response = fileUploadController.createDownloadUrl(12L);

        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body =
                (java.util.Map<String, Object>) response.getResponse().getBody();
        Assertions.assertThat(body).containsEntry("url",
                "http://localhost:9000/erp-files/documents/block/1/a.pdf?X-Amz-Expires=300");
        Assertions.assertThat(body).containsEntry("expiresInSeconds", 300L);
        Assertions.assertThat(body).containsEntry("expiresAt", "2026-11-23T12:05:00Z");
    }

    @Test
    @DisplayName("createDownloadUrl returns an image link with the 30 minute expiry")
    void createDownloadUrlImageExpiry() {
        Instant expiresAt = Instant.parse("2026-11-23T12:30:00Z");
        Mockito.when(fileStorageService.createPresignedDownload(4L))
                .thenReturn(new PresignedDownload(
                        "http://localhost:9000/erp-files/images/block/1/a.jpg?X-Amz-Expires=1800",
                        1800,
                        expiresAt));

        BackEndResponse response = fileUploadController.createDownloadUrl(4L);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body =
                (java.util.Map<String, Object>) response.getResponse().getBody();
        Assertions.assertThat(body).containsEntry("expiresInSeconds", 1800L);
        Assertions.assertThat(body.get("url").toString()).contains("X-Amz-Expires=1800");
    }

    @Test
    @DisplayName("createDownloadUrl does not hide an authorization failure")
    void createDownloadUrlUnauthorizedPropagates() {
        Mockito.when(fileStorageService.createPresignedDownload(3L))
                .thenThrow(new FileAccessDeniedException("denied"));

        Assertions.assertThatThrownBy(() -> fileUploadController.createDownloadUrl(3L))
                .isInstanceOf(FileAccessDeniedException.class);
    }

    @Test
    @DisplayName("viewFile should return inline content disposition")
    void viewFileSuccess() {
        FileStorage storage = FileStorage.builder()
                .id(5L)
                .fileName("uuid-photo.jpg")
                .originalName("photo.jpg")
                .mimeType("image/jpeg")
                .fileSize(11L)
                .build();
        ByteArrayResource resource = new ByteArrayResource("image".getBytes());
        Mockito.when(fileStorageService.requireAccessibleFileView(5L)).thenReturn(com.ozerler.marble.dto.FileStorageDto.fromEntity(storage));
        Mockito.when(fileStorageService.loadAsResource(5L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.viewFile(5L);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
    }
}
