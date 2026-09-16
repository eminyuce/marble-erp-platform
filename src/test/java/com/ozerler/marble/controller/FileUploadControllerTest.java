package com.ozerler.marble.controller;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.FileStorageDto;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @Test
    @DisplayName("uploadFile should return success response with FileStorageDto")
    void uploadFile_Success() throws IOException {
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

        when(fileStorageService.storeFile(eq(file), eq("BLOCK"), isNull())).thenReturn(storage);

        BackEndResponse response = fileUploadController.uploadFile(file, "BLOCK", null);

        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
    }

    @Test
    @DisplayName("uploadMultipleFiles should return list of FileStorageDtos")
    void uploadMultipleFiles_Success() throws IOException {
        MockMultipartFile f1 = new MockMultipartFile("files", "img1.png", "image/png", "img1".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "doc1.pdf", "application/pdf", "pdf1".getBytes());

        FileStorage s1 = FileStorage.builder().id(10L).fileName("s1.png").originalName("img1.png").mimeType("image/png").build();
        FileStorage s2 = FileStorage.builder().id(11L).fileName("s2.pdf").originalName("doc1.pdf").mimeType("application/pdf").build();

        when(fileStorageService.storeFile(eq(f1), eq("BLOCK"), eq(100L))).thenReturn(s1);
        when(fileStorageService.storeFile(eq(f2), eq("BLOCK"), eq(100L))).thenReturn(s2);

        BackEndResponse response = fileUploadController.uploadMultipleFiles(List.of(f1, f2), "BLOCK", 100L);

        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getResponse().getBody()).asList().hasSize(2);
    }

    @Test
    @DisplayName("deleteFileById should delete file and return OK status")
    void deleteFileById_Success() {
        when(fileStorageService.deleteFileRecord(25L)).thenReturn(true);

        BackEndResponse response = fileUploadController.deleteFileById(25L);

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        verify(fileStorageService).deleteFileRecord(25L);
    }

    @Test
    @DisplayName("deleteFileById should return 404 when file is not found")
    void deleteFileById_NotFound() {
        when(fileStorageService.deleteFileRecord(999L)).thenReturn(false);

        BackEndResponse response = fileUploadController.deleteFileById(999L);

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("revertUpload with numeric ID should delegate to deleteFileRecord")
    void revertUpload_NumericId() {
        when(fileStorageService.deleteFileRecord(50L)).thenReturn(true);

        BackEndResponse response = fileUploadController.revertUpload("50");

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        verify(fileStorageService).deleteFileRecord(50L);
    }

    @Test
    @DisplayName("revertUpload with URL should delegate to deleteFile")
    void revertUpload_FileUrl() {
        when(fileStorageService.deleteFile("/uploads/test.jpg")).thenReturn(true);

        BackEndResponse response = fileUploadController.revertUpload("/uploads/test.jpg");

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        verify(fileStorageService).deleteFile("/uploads/test.jpg");
    }

    @Test
    @DisplayName("downloadFile should return ResponseEntity with Resource and attachment header")
    void downloadFile_Success() {
        FileStorage storage = FileStorage.builder()
                .id(5L)
                .fileName("uuid-rep.pdf")
                .originalName("Rapor 2026.pdf")
                .mimeType("application/pdf")
                .build();

        ByteArrayResource resource = new ByteArrayResource("pdf-content".getBytes());

        when(fileStorageService.requireAccessibleFile(5L)).thenReturn(storage);
        when(fileStorageService.loadAsResource(5L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().toString()).contains("Rapor 2026.pdf");
        assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve DOCX filename with Turkish characters")
    void downloadFile_Docx_PreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(6L)
                .fileName("blocks_12345678.docx")
                .originalName("Mermer Analiz Raporu (Şantiye & Ocak).docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .build();

        ByteArrayResource resource = new ByteArrayResource("fake-docx-data".getBytes());

        when(fileStorageService.requireAccessibleFile(6L)).thenReturn(storage);
        when(fileStorageService.loadAsResource(6L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(6L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("Mermer Analiz Raporu (Şantiye & Ocak).docx");
        assertThat(response.getHeaders().getContentDisposition().getType())
                .isEqualTo("attachment");
        assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve TXT filename")
    void downloadFile_Txt_PreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(7L)
                .fileName("blocks_98765432.txt")
                .originalName("jeoloji_notlari.txt")
                .mimeType("text/plain")
                .build();

        ByteArrayResource resource = new ByteArrayResource("notlar...".getBytes());

        when(fileStorageService.requireAccessibleFile(7L)).thenReturn(storage);
        when(fileStorageService.loadAsResource(7L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("jeoloji_notlari.txt");
        assertThat(response.getHeaders().getContentType().toString())
                .contains("text/plain");
        assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve XLSX filename and spreadsheet MIME type")
    void downloadFile_Xlsx_PreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(8L)
                .fileName("blocks_88888888.xlsx")
                .originalName("Blok_Analiz_Raporu_2026.xlsx")
                .mimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .build();

        ByteArrayResource resource = new ByteArrayResource("fake-xlsx-bytes".getBytes());

        when(fileStorageService.requireAccessibleFile(8L)).thenReturn(storage);
        when(fileStorageService.loadAsResource(8L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(8L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("Blok_Analiz_Raporu_2026.xlsx");
        assertThat(response.getHeaders().getContentType().toString())
                .contains("spreadsheetml.sheet");
        assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("downloadFile should preserve CSV filename and text/csv MIME type")
    void downloadFile_Csv_PreservesOriginalFilename() {
        FileStorage storage = FileStorage.builder()
                .id(9L)
                .fileName("blocks_99999999.csv")
                .originalName("blok_verileri.csv")
                .mimeType("text/csv")
                .build();

        ByteArrayResource resource = new ByteArrayResource("id,code\n11,BLK-11".getBytes());

        when(fileStorageService.requireAccessibleFile(9L)).thenReturn(storage);
        when(fileStorageService.loadAsResource(9L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.downloadFile(9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("blok_verileri.csv");
        assertThat(response.getHeaders().getContentType().toString())
                .contains("text/csv");
        assertThat(response.getBody()).isEqualTo(resource);
    }

    @Test
    @DisplayName("createDownloadUrl should return metadata and a presigned URL")
    void createDownloadUrl_Success() {
        when(fileStorageService.createPresignedDownloadUrl(12L))
                .thenReturn("http://localhost:9000/erp-files/documents/block/1/a.pdf?sig=1");
        when(fileStorageService.getPresignedUrlExpiry()).thenReturn(java.time.Duration.ofMinutes(15));

        BackEndResponse response = fileUploadController.createDownloadUrl(12L);

        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body =
                (java.util.Map<String, Object>) response.getResponse().getBody();
        assertThat(body).containsEntry("fileId", 12L);
        assertThat(body).containsEntry("url", "http://localhost:9000/erp-files/documents/block/1/a.pdf?sig=1");
    }

    @Test
    @DisplayName("viewFile should return inline content disposition")
    void viewFile_Success() {
        FileStorage storage = FileStorage.builder()
                .id(5L)
                .fileName("uuid-photo.jpg")
                .originalName("photo.jpg")
                .mimeType("image/jpeg")
                .fileSize(11L)
                .build();
        ByteArrayResource resource = new ByteArrayResource("image".getBytes());
        when(fileStorageService.requireAccessibleFile(5L)).thenReturn(storage);
        when(fileStorageService.loadAsResource(5L)).thenReturn(resource);

        ResponseEntity<Resource> response = fileUploadController.viewFile(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
    }
}
