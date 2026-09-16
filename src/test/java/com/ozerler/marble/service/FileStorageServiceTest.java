package com.ozerler.marble.service;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.exception.FileAccessDeniedException;
import com.ozerler.marble.exception.FileValidationException;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import com.ozerler.marble.storage.FileObjectKeyFactory;
import com.ozerler.marble.storage.ObjectStorageService;
import com.ozerler.marble.support.TestFiles;
import com.ozerler.marble.validation.FileUploadValidator;
import com.ozerler.marble.validation.SvgContentValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileStorageRepository fileStorageRepository;

    @Mock
    private org.springframework.context.MessageSource messageSource;

    @Mock
    private ObjectStorageService objectStorageService;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        FileUploadValidator validator = new FileUploadValidator(properties, new SvgContentValidator());
        fileStorageService = new FileStorageService(
                fileStorageRepository,
                messageSource,
                objectStorageService,
                properties,
                validator,
                new FileObjectKeyFactory());
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("storeFile should upload image to object storage and persist metadata with view URL")
    void storeFile_Image_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "block_front.jpg", "image/jpeg", TestFiles.jpeg());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 100L);

        assertThat(saved.getOriginalName()).isEqualTo("block_front.jpg");
        assertThat(saved.getEntityType()).isEqualTo("BLOCK");
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getObjectKey()).startsWith("images/block/100/");
        assertThat(saved.getObjectKey()).endsWith(".jpg");
        assertThat(saved.getFilePath()).isEqualTo("/api/upload/view/1");
        assertThat(saved.getChecksum()).isNotBlank();
        assertThat(saved.isImage()).isTrue();
        verify(objectStorageService).upload(anyString(), any(InputStream.class), anyLong(), anyString());
    }

    @Test
    @DisplayName("storeFile should upload a valid SVG under the svg/ object-key prefix")
    void storeFile_Svg_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.svg", "image/svg+xml", TestFiles.svg());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 12L);

        assertThat(saved.getObjectKey()).startsWith("svg/block/12/");
        assertThat(saved.getObjectKey()).endsWith(".svg");
        assertThat(saved.isImage()).isTrue();
        assertThat(saved.getMimeType()).contains("svg");
    }

    @Test
    @DisplayName("storeFile should reject SVG documents that contain script")
    void storeFile_MaliciousSvg_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.svg", "image/svg+xml", TestFiles.maliciousSvg());

        assertThatThrownBy(() -> fileStorageService.storeFile(file, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
        verify(objectStorageService, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("storeFile should save PDF document in documents/ object-key prefix")
    void storeFile_Pdf_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "geology_report.pdf", "application/pdf", TestFiles.pdf());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 100L);

        assertThat(saved.getFilePath()).startsWith("/api/upload/view/");
        assertThat(saved.getObjectKey()).startsWith("documents/block/100/");
        assertThat(saved.isPdf()).isTrue();
        assertThat(saved.isImage()).isFalse();
    }

    @Test
    @DisplayName("storeFile should save DOCX document successfully")
    void storeFile_Docx_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "analysis.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                TestFiles.zipOffice());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", null);

        assertThat(saved.getObjectKey()).startsWith("documents/block/pending/");
        assertThat(saved.isWord()).isTrue();
    }

    @Test
    @DisplayName("storeFile should save TXT document successfully")
    void storeFile_Txt_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "geology_notes.txt",
                "text/plain",
                "ocak jeolojik gozlemleri: kalsit damari var".getBytes());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", null);

        assertThat(saved.isText()).isTrue();
        assertThat(saved.isDocument()).isTrue();
        assertThat(saved.getObjectKey()).contains("documents/");
    }

    @Test
    @DisplayName("storeFile should save XLSX document successfully")
    void storeFile_Xlsx_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "block_density_analysis.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                TestFiles.zipOffice());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 11L);

        assertThat(saved.isExcel()).isTrue();
        assertThat(saved.getObjectKey()).startsWith("documents/block/11/");
    }

    @Test
    @DisplayName("storeFile should save XLS document successfully")
    void storeFile_Xls_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "quarry_measurements.xls",
                "application/vnd.ms-excel",
                "fake-xls-content".getBytes());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 11L);

        assertThat(saved.isExcel()).isTrue();
        assertThat(saved.isDocument()).isTrue();
    }

    @Test
    @DisplayName("storeFile should save CSV document successfully")
    void storeFile_Csv_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "spectrometry_data.csv",
                "text/csv",
                "sample_id,density,hardness\n1,2.71,3.5".getBytes());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 11L);

        assertThat(saved.isCsv()).isTrue();
        assertThat(saved.getOriginalName()).isEqualTo("spectrometry_data.csv");
    }

    @Test
    @DisplayName("storeFile should allow two uploads with the same original filename")
    void storeFile_DuplicateOriginalName_Succeeds() throws IOException {
        MockMultipartFile first = new MockMultipartFile("file", "photo.jpg", "image/jpeg", TestFiles.jpeg());
        MockMultipartFile second = new MockMultipartFile("file", "photo.jpg", "image/jpeg", TestFiles.jpeg());
        stubSuccessfulUpload();

        FileStorage one = fileStorageService.storeFile(first, "BLOCK", 1L);
        FileStorage two = fileStorageService.storeFile(second, "BLOCK", 1L);

        assertThat(one.getOriginalName()).isEqualTo(two.getOriginalName());
        assertThat(one.getObjectKey()).isNotEqualTo(two.getObjectKey());
    }

    @Test
    @DisplayName("storeFile should reject empty file")
    void storeFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("storeFile should reject path traversal")
    void storeFile_PathTraversal_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "../evil.jpg", "image/jpeg", TestFiles.jpeg());

        assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("storeFile should reject unsupported file extensions (e.g. .exe, .sh)")
    void storeFile_UnsupportedExtension_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "bad".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("storeFile should reject JPEG extension with non-image content")
    void storeFile_MimeAndMagicMismatch_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "disguised.jpg", "image/jpeg", "MZ executable".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("attachFilesToEntity should update pending files with entityType and entityId")
    void attachFilesToEntity_UpdatesFiles() {
        FileStorage f1 = FileStorage.builder().id(10L).fileName("f1.jpg").entityType("BLOCK").build();
        FileStorage f2 = FileStorage.builder().id(11L).fileName("f2.pdf").entityType("BLOCK").build();

        when(fileStorageRepository.findByIdInAndDeletedFalse(List.of(10L, 11L)))
                .thenReturn(List.of(f1, f2));

        fileStorageService.attachFilesToEntity(List.of(10L, 11L), "BLOCK", 500L);

        assertThat(f1.getEntityId()).isEqualTo(500L);
        assertThat(f2.getEntityId()).isEqualTo(500L);
        verify(fileStorageRepository).saveAll(List.of(f1, f2));
    }

    @Test
    @DisplayName("deleteFileRecord should soft-delete metadata and remove the object")
    void deleteFileRecord_Success() {
        FileStorage fs = FileStorage.builder()
                .id(42L)
                .fileName("uuid.png")
                .objectKey("images/block/1/uuid.png")
                .filePath("/api/upload/view/42")
                .originalName("photo.png")
                .entityType("BLOCK")
                .entityId(1L)
                .build();

        when(fileStorageRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(fs));

        boolean deleted = fileStorageService.deleteFileRecord(42L);

        assertThat(deleted).isTrue();
        assertThat(fs.isDeleted()).isTrue();
        assertThat(fs.getDeletedAt()).isNotNull();
        verify(objectStorageService).delete("images/block/1/uuid.png");
        verify(fileStorageRepository).save(fs);
    }

    @Test
    @DisplayName("deleteAllFilesForEntity should soft-delete linked files and remove objects")
    void deleteAllFilesForEntity_Success() {
        FileStorage f1 = FileStorage.builder().id(1L).fileName("f1.jpg")
                .objectKey("images/block/99/f1.jpg").entityType("BLOCK").entityId(99L).build();
        FileStorage f2 = FileStorage.builder().id(2L).fileName("f2.pdf")
                .objectKey("documents/block/99/f2.pdf").entityType("BLOCK").entityId(99L).build();

        when(fileStorageRepository.findByEntityTypeAndEntityId("BLOCK", 99L))
                .thenReturn(List.of(f1, f2));

        fileStorageService.deleteAllFilesForEntity("BLOCK", 99L);

        assertThat(f1.isDeleted()).isTrue();
        assertThat(f2.isDeleted()).isTrue();
        verify(objectStorageService).delete("images/block/99/f1.jpg");
        verify(objectStorageService).delete("documents/block/99/f2.pdf");
    }

    @Test
    @DisplayName("loadAsResource should stream the object from storage")
    void loadAsResource_Success() throws IOException {
        FileStorage fs = FileStorage.builder()
                .id(15L)
                .fileName("doc.pdf")
                .objectKey("documents/block/1/doc.pdf")
                .originalName("blocks_doc.pdf")
                .fileSize(8L)
                .build();

        when(fileStorageRepository.findByIdAndDeletedFalse(15L)).thenReturn(Optional.of(fs));
        when(objectStorageService.download("documents/block/1/doc.pdf"))
                .thenReturn(new ByteArrayInputStream("pdf-data".getBytes()));

        Resource resource = fileStorageService.loadAsResource(15L);

        assertThat(resource).isNotNull();
        assertThat(resource.getFilename()).isEqualTo("blocks_doc.pdf");
        assertThat(resource.contentLength()).isEqualTo(8L);
        assertThat(resource.getInputStream()).hasContent("pdf-data");
    }

    @Test
    @DisplayName("createPresignedDownloadUrl should authorize then ask object storage for a URL")
    void createPresignedDownloadUrl_Success() {
        FileStorage fs = FileStorage.builder()
                .id(8L)
                .objectKey("documents/block/1/a.pdf")
                .entityType("BLOCK")
                .entityId(1L)
                .build();
        when(fileStorageRepository.findByIdAndDeletedFalse(8L)).thenReturn(Optional.of(fs));
        when(objectStorageService.createPresignedUrl("documents/block/1/a.pdf", Duration.ofMinutes(15)))
                .thenReturn("http://localhost:9000/erp-files/documents/block/1/a.pdf?X-Amz-Expires=900");

        String url = fileStorageService.createPresignedDownloadUrl(8L);

        assertThat(url).contains("erp-files");
    }

    @Test
    @DisplayName("unattached file cannot be downloaded by a different authenticated user")
    void requireAccessibleFile_UnauthorizedUser_Denied() {
        FileStorage fs = FileStorage.builder()
                .id(3L)
                .objectKey("images/block/pending/a.jpg")
                .entityType("BLOCK")
                .build();
        fs.setAddUserId("owner@example.com");
        when(fileStorageRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(fs));
        authenticate("other@example.com", "ROLE_USER");

        assertThatThrownBy(() -> fileStorageService.requireAccessibleFile(3L))
                .isInstanceOf(FileAccessDeniedException.class);
    }

    @Test
    @DisplayName("checkStorageHealth should report UP when object storage is healthy")
    void checkStorageHealth_Success() {
        when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());

        var health = fileStorageService.checkStorageHealth();

        assertThat(health.getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("checkStorageHealth should report DOWN when object storage is unavailable")
    void checkStorageHealth_WhenStorageDown_ReportsDown() {
        when(objectStorageService.checkHealth())
                .thenReturn(DependencyHealth.down("Nesne depolama (MinIO) erişilemiyor"));

        var health = fileStorageService.checkStorageHealth();

        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getError()).contains("MinIO");
    }

    private void stubSuccessfulUpload() {
        when(objectStorageService.getBucketName()).thenReturn("erp-files");
        doAnswer(invocation -> {
            InputStream in = invocation.getArgument(1);
            in.readAllBytes();
            return null;
        }).when(objectStorageService).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        when(fileStorageRepository.save(any(FileStorage.class))).thenAnswer(invocation -> {
            FileStorage fs = invocation.getArgument(0);
            if (fs.getId() == null) {
                fs.setId(1L);
            }
            return fs;
        });
    }

    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        "n/a",
                        List.of(new SimpleGrantedAuthority(role))));
    }
}
