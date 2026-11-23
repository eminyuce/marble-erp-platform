package com.ozerler.marble.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.config.PresignedUrlProperties;
import com.ozerler.marble.dto.DependencyHealth;
import com.ozerler.marble.dto.FileStorageDto;
import com.ozerler.marble.dto.PresignedDownload;
import com.ozerler.marble.exception.FileAccessDeniedException;
import com.ozerler.marble.exception.FileValidationException;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import com.ozerler.marble.storage.FileObjectKeyFactory;
import com.ozerler.marble.storage.ObjectStorageService;
import com.ozerler.marble.support.TestFiles;
import com.ozerler.marble.validation.FileUploadValidator;
import com.ozerler.marble.validation.SvgContentValidator;

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
                new PresignedUrlProperties(),
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

        Assertions.assertThat(saved.getOriginalName()).isEqualTo("block_front.jpg");
        Assertions.assertThat(saved.getEntityType()).isEqualTo("BLOCK");
        Assertions.assertThat(saved.getEntityId()).isEqualTo(100L);
        Assertions.assertThat(saved.getObjectKey()).startsWith("images/block/100/");
        Assertions.assertThat(saved.getObjectKey()).endsWith(".jpg");
        Assertions.assertThat(saved.getFilePath()).isEqualTo("/api/upload/view/1");
        Assertions.assertThat(saved.getChecksum()).isNotBlank();
        Assertions.assertThat(saved.isImage()).isTrue();
        Mockito.verify(objectStorageService).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any(InputStream.class), ArgumentMatchers.anyLong(), ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("storeFile should upload a valid SVG under the svg/ object-key prefix")
    void storeFile_Svg_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.svg", "image/svg+xml", TestFiles.svg());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 12L);

        Assertions.assertThat(saved.getObjectKey()).startsWith("svg/block/12/");
        Assertions.assertThat(saved.getObjectKey()).endsWith(".svg");
        Assertions.assertThat(saved.isImage()).isTrue();
        Assertions.assertThat(saved.getMimeType()).contains("svg");
    }

    @Test
    @DisplayName("storeFile should reject SVG documents that contain script")
    void storeFile_MaliciousSvg_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.svg", "image/svg+xml", TestFiles.maliciousSvg());

        Assertions.assertThatThrownBy(() -> fileStorageService.storeFile(file, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
        Mockito.verify(objectStorageService, Mockito.never()).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.anyLong(), ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("storeFile should save PDF document in documents/ object-key prefix")
    void storeFile_Pdf_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "geology_report.pdf", "application/pdf", TestFiles.pdf());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 100L);

        Assertions.assertThat(saved.getFilePath()).startsWith("/api/upload/view/");
        Assertions.assertThat(saved.getObjectKey()).startsWith("documents/block/100/");
        Assertions.assertThat(saved.isPdf()).isTrue();
        Assertions.assertThat(saved.isImage()).isFalse();
    }

    @Test
    @DisplayName("storeFile should save DOCX document successfully")
    void storeFile_Docx_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "analysis.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                TestFiles.docx());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", null);

        Assertions.assertThat(saved.getObjectKey()).startsWith("documents/block/pending/");
        Assertions.assertThat(saved.isWord()).isTrue();
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

        Assertions.assertThat(saved.isText()).isTrue();
        Assertions.assertThat(saved.isMarkdown()).isFalse();
        Assertions.assertThat(saved.isDocument()).isTrue();
        Assertions.assertThat(saved.getObjectKey()).contains("documents/");
    }

    @Test
    @DisplayName("storeFile should save Markdown document successfully")
    void storeFile_Markdown_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "jeoloji_notu.md",
                "text/markdown",
                "# Ocak notu\nKalsit damari var.".getBytes());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 4L);

        Assertions.assertThat(saved.isMarkdown()).isTrue();
        Assertions.assertThat(saved.isText()).isFalse();
        Assertions.assertThat(saved.isDocument()).isTrue();
        Assertions.assertThat(saved.getMimeType()).isEqualTo("text/markdown");
        Assertions.assertThat(saved.getObjectKey()).startsWith("documents/block/4/");
        Assertions.assertThat(saved.getObjectKey()).endsWith(".md");
    }

    @Test
    @DisplayName("storeFile should save XLSX document successfully")
    void storeFile_Xlsx_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "block_density_analysis.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                TestFiles.xlsx());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 11L);

        Assertions.assertThat(saved.isExcel()).isTrue();
        Assertions.assertThat(saved.getObjectKey()).startsWith("documents/block/11/");
    }

    @Test
    @DisplayName("storeFile should save XLS document successfully")
    void storeFile_Xls_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "quarry_measurements.xls",
                "application/octet-stream",
                TestFiles.oleCompound());
        stubSuccessfulUpload();

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 11L);

        Assertions.assertThat(saved.isExcel()).isTrue();
        Assertions.assertThat(saved.isDocument()).isTrue();
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

        Assertions.assertThat(saved.isCsv()).isTrue();
        Assertions.assertThat(saved.getOriginalName()).isEqualTo("spectrometry_data.csv");
    }

    @Test
    @DisplayName("storeFile should allow two uploads with the same original filename")
    void storeFile_DuplicateOriginalName_Succeeds() throws IOException {
        MockMultipartFile first = new MockMultipartFile("file", "photo.jpg", "image/jpeg", TestFiles.jpeg());
        MockMultipartFile second = new MockMultipartFile("file", "photo.jpg", "image/jpeg", TestFiles.jpeg());
        stubSuccessfulUpload();

        FileStorage one = fileStorageService.storeFile(first, "BLOCK", 1L);
        FileStorage two = fileStorageService.storeFile(second, "BLOCK", 1L);

        Assertions.assertThat(one.getOriginalName()).isEqualTo(two.getOriginalName());
        Assertions.assertThat(one.getObjectKey()).isNotEqualTo(two.getObjectKey());
    }

    @Test
    @DisplayName("storeFile should reject empty file")
    void storeFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        Assertions.assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("storeFile should reject path traversal")
    void storeFile_PathTraversal_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "../evil.jpg", "image/jpeg", TestFiles.jpeg());

        Assertions.assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("storeFile should reject unsupported file extensions (e.g. .exe, .sh)")
    void storeFile_UnsupportedExtension_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "bad".getBytes());

        Assertions.assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("storeFile should reject JPEG extension with non-image content")
    void storeFile_MimeAndMagicMismatch_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "disguised.jpg", "image/jpeg", "MZ executable".getBytes());

        Assertions.assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("attachFilesToEntity should update pending files with entityType and entityId")
    void attachFilesToEntity_UpdatesFiles() {
        FileStorage f1 = FileStorage.builder().id(10L).fileName("f1.jpg").entityType("BLOCK").build();
        FileStorage f2 = FileStorage.builder().id(11L).fileName("f2.pdf").entityType("BLOCK").build();

        Mockito.when(fileStorageRepository.findByIdInAndDeletedFalse(List.of(10L, 11L)))
                .thenReturn(List.of(f1, f2));

        fileStorageService.attachFilesToEntity(List.of(10L, 11L), "BLOCK", 500L);

        Assertions.assertThat(f1.getEntityId()).isEqualTo(500L);
        Assertions.assertThat(f2.getEntityId()).isEqualTo(500L);
        Mockito.verify(fileStorageRepository).saveAll(List.of(f1, f2));
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

        Mockito.when(fileStorageRepository.findByIdAndDeletedFalse(42L)).thenReturn(Optional.of(fs));

        boolean deleted = fileStorageService.deleteFileRecord(42L);

        Assertions.assertThat(deleted).isTrue();
        Assertions.assertThat(fs.isDeleted()).isTrue();
        Assertions.assertThat(fs.getDeletedAt()).isNotNull();
        Mockito.verify(objectStorageService).delete("images/block/1/uuid.png");
        Mockito.verify(fileStorageRepository).save(fs);
    }

    @Test
    @DisplayName("deleteAllFilesForEntity should soft-delete linked files and remove objects")
    void deleteAllFilesForEntity_Success() {
        FileStorage f1 = FileStorage.builder().id(1L).fileName("f1.jpg")
                .objectKey("images/block/99/f1.jpg").entityType("BLOCK").entityId(99L).build();
        FileStorage f2 = FileStorage.builder().id(2L).fileName("f2.pdf")
                .objectKey("documents/block/99/f2.pdf").entityType("BLOCK").entityId(99L).build();

        Mockito.when(fileStorageRepository.findByEntityTypeAndEntityId("BLOCK", 99L))
                .thenReturn(List.of(f1, f2));

        fileStorageService.deleteAllFilesForEntity("BLOCK", 99L);

        Assertions.assertThat(f1.isDeleted()).isTrue();
        Assertions.assertThat(f2.isDeleted()).isTrue();
        Mockito.verify(objectStorageService).delete("images/block/99/f1.jpg");
        Mockito.verify(objectStorageService).delete("documents/block/99/f2.pdf");
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

        Mockito.when(fileStorageRepository.findByIdAndDeletedFalse(15L)).thenReturn(Optional.of(fs));
        Mockito.when(objectStorageService.download("documents/block/1/doc.pdf"))
                .thenReturn(new ByteArrayInputStream("pdf-data".getBytes()));

        Resource resource = fileStorageService.loadAsResource(15L);

        Assertions.assertThat(resource).isNotNull();
        Assertions.assertThat(resource.getFilename()).isEqualTo("blocks_doc.pdf");
        Assertions.assertThat(resource.contentLength()).isEqualTo(8L);
        Assertions.assertThat(resource.getInputStream()).hasContent("pdf-data");
    }

    @Test
    @DisplayName("createPresignedDownload authorizes then signs a document for 5 minutes")
    void createPresignedDownload_DocumentUsesShortExpiry() {
        FileStorage fs = FileStorage.builder()
                .id(8L)
                .objectKey("documents/block/1/a.pdf")
                .originalName("a.pdf")
                .mimeType("application/pdf")
                .entityType("BLOCK")
                .entityId(1L)
                .build();
        Mockito.when(fileStorageRepository.findByIdAndDeletedFalse(8L)).thenReturn(Optional.of(fs));
        Mockito.when(objectStorageService.presignedDocumentUrl("documents/block/1/a.pdf"))
                .thenReturn("http://localhost:9000/erp-files/documents/block/1/a.pdf?X-Amz-Expires=300");

        PresignedDownload download = fileStorageService.createPresignedDownload(8L);

        Assertions.assertThat(download.url()).contains("erp-files");
        Assertions.assertThat(download.expiresInSeconds()).isEqualTo(300);
        Assertions.assertThat(download.expiresAt()).isAfter(java.time.Instant.now());
        Mockito.verify(objectStorageService, Mockito.never()).presignedImageUrl(ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("createPresignedDownload signs an image for 30 minutes")
    void createPresignedDownload_ImageUsesLongerExpiry() {
        FileStorage fs = FileStorage.builder()
                .id(9L)
                .objectKey("images/block/1/a.jpg")
                .originalName("a.jpg")
                .mimeType("image/jpeg")
                .entityType("BLOCK")
                .entityId(1L)
                .build();
        Mockito.when(fileStorageRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.of(fs));
        Mockito.when(objectStorageService.presignedImageUrl("images/block/1/a.jpg"))
                .thenReturn("http://localhost:9000/erp-files/images/block/1/a.jpg?X-Amz-Expires=1800");

        PresignedDownload download = fileStorageService.createPresignedDownload(9L);

        Assertions.assertThat(download.expiresInSeconds()).isEqualTo(1800);
        Mockito.verify(objectStorageService, Mockito.never()).presignedDocumentUrl(ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("access is checked before a presigned URL is created")
    void createPresignedDownload_UnauthorizedUser_DoesNotSign() {
        FileStorage fs = FileStorage.builder()
                .id(3L)
                .objectKey("images/block/pending/a.jpg")
                .originalName("a.jpg")
                .mimeType("image/jpeg")
                .entityType("BLOCK")
                .build();
        fs.setAddUserId("owner@example.com");
        Mockito.when(fileStorageRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(fs));
        authenticate("other@example.com", "ROLE_USER");

        Assertions.assertThatThrownBy(() -> fileStorageService.createPresignedDownload(3L))
                .isInstanceOf(FileAccessDeniedException.class);
        Mockito.verify(objectStorageService, Mockito.never()).presignedImageUrl(ArgumentMatchers.anyString());
        Mockito.verify(objectStorageService, Mockito.never()).presignedDocumentUrl(ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("toView attaches a fresh image URL and does not persist it")
    void toView_Image_SetsPresignedUrlOnlyOnTheDto() {
        FileStorage fs = FileStorage.builder()
                .id(4L)
                .objectKey("images/block/1/a.jpg")
                .originalName("a.jpg")
                .mimeType("image/jpeg")
                .fileName("a.jpg")
                .filePath("/api/upload/view/4")
                .fileSize(10L)
                .entityType("BLOCK")
                .entityId(1L)
                .build();
        Mockito.when(objectStorageService.presignedImageUrl("images/block/1/a.jpg"))
                .thenReturn("http://localhost:9000/erp-files/images/block/1/a.jpg?X-Amz-Signature=hidden");

        FileStorageDto view = fileStorageService.toView(fs);

        Assertions.assertThat(view.getPresignedImageUrl()).contains("X-Amz-Signature");
        Assertions.assertThat(fs.getFilePath()).isEqualTo("/api/upload/view/4");
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
        Mockito.when(fileStorageRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(fs));
        authenticate("other@example.com", "ROLE_USER");

        Assertions.assertThatThrownBy(() -> fileStorageService.requireAccessibleFile(3L))
                .isInstanceOf(FileAccessDeniedException.class);
    }

    @Test
    @DisplayName("checkStorageHealth should report UP when object storage is healthy")
    void checkStorageHealth_Success() {
        Mockito.when(objectStorageService.checkHealth()).thenReturn(DependencyHealth.up());

        var health = fileStorageService.checkStorageHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("checkStorageHealth should report DOWN when object storage is unavailable")
    void checkStorageHealth_WhenStorageDown_ReportsDown() {
        Mockito.when(objectStorageService.checkHealth())
                .thenReturn(DependencyHealth.down("RustFS erişilemiyor"));

        var health = fileStorageService.checkStorageHealth();

        Assertions.assertThat(health.getStatus()).isEqualTo("DOWN");
        Assertions.assertThat(health.getError()).contains("RustFS");
    }

    private void stubSuccessfulUpload() {
        Mockito.when(objectStorageService.getBucketName()).thenReturn("erp-files");
        Mockito.doAnswer(invocation -> {
            InputStream in = invocation.getArgument(1);
            in.readAllBytes();
            return null;
        }).when(objectStorageService).upload(ArgumentMatchers.anyString(), ArgumentMatchers.any(InputStream.class), ArgumentMatchers.anyLong(), ArgumentMatchers.anyString());
        Mockito.when(fileStorageRepository.save(ArgumentMatchers.any(FileStorage.class))).thenAnswer(invocation -> {
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
