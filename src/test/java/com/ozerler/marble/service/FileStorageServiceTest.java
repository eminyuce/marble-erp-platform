package com.ozerler.marble.service;

import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.repository.FileStorageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileStorageRepository fileStorageRepository;

    @Mock
    private org.springframework.context.MessageSource messageSource;

    private FileStorageService fileStorageService;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(fileStorageRepository, messageSource);
        ReflectionTestUtils.setField(fileStorageService, "mediaDir", tempUploadDir.toString());
        fileStorageService.init();
    }

    @Test
    @DisplayName("storeFile should save image file physically in images/ with blocks_ prefix and persist FileStorage entity")
    void storeFile_Image_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "block_front.jpg", "image/jpeg", "fake-image-content".getBytes());

        when(fileStorageRepository.save(any(FileStorage.class))).thenAnswer(invocation -> {
            FileStorage fs = invocation.getArgument(0);
            fs.setId(1L);
            return fs;
        });

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 100L);

        assertThat(saved).isNotNull();
        assertThat(saved.getOriginalName()).isEqualTo("block_front.jpg");
        assertThat(saved.getEntityType()).isEqualTo("BLOCK");
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getFileName()).startsWith("blocks_");
        assertThat(saved.getFilePath()).startsWith("/media/images/");
        assertThat(saved.isImage()).isTrue();
        assertThat(saved.isPdf()).isFalse();
        assertThat(saved.isWord()).isFalse();

        Path storedFile = tempUploadDir.resolve("images").resolve(saved.getFileName());
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo("fake-image-content".getBytes());
    }

    @Test
    @DisplayName("storeFile should save PDF document in documents/ with blocks_ prefix")
    void storeFile_Pdf_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "geology_report.pdf", "application/pdf", "fake-pdf-content".getBytes());

        when(fileStorageRepository.save(any(FileStorage.class))).thenAnswer(invocation -> {
            FileStorage fs = invocation.getArgument(0);
            fs.setId(2L);
            return fs;
        });

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", 100L);

        assertThat(saved).isNotNull();
        assertThat(saved.getOriginalName()).isEqualTo("geology_report.pdf");
        assertThat(saved.getFileName()).startsWith("blocks_");
        assertThat(saved.getFilePath()).startsWith("/media/documents/");
        assertThat(saved.isPdf()).isTrue();
        assertThat(saved.isImage()).isFalse();
        assertThat(saved.isWord()).isFalse();

        Path storedFile = tempUploadDir.resolve("documents").resolve(saved.getFileName());
        assertThat(Files.exists(storedFile)).isTrue();
    }

    @Test
    @DisplayName("storeFile should save DOCX document successfully in documents/")
    void storeFile_Docx_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "analysis.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "fake-docx-content".getBytes());

        when(fileStorageRepository.save(any(FileStorage.class))).thenAnswer(invocation -> {
            FileStorage fs = invocation.getArgument(0);
            fs.setId(3L);
            return fs;
        });

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", null);

        assertThat(saved).isNotNull();
        assertThat(saved.getOriginalName()).isEqualTo("analysis.docx");
        assertThat(saved.getFileName()).startsWith("blocks_");
        assertThat(saved.getFilePath()).startsWith("/media/documents/");
        assertThat(saved.isWord()).isTrue();
        assertThat(saved.isPdf()).isFalse();
        assertThat(saved.isImage()).isFalse();

        Path storedFile = tempUploadDir.resolve("documents").resolve(saved.getFileName());
        assertThat(Files.exists(storedFile)).isTrue();
    }

    @Test
    @DisplayName("storeFile should save TXT document successfully in documents/")
    void storeFile_Txt_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "geology_notes.txt",
                "text/plain",
                "ocak jeolojik gozlemleri: kalsit damari var".getBytes());

        when(fileStorageRepository.save(any(FileStorage.class))).thenAnswer(invocation -> {
            FileStorage fs = invocation.getArgument(0);
            fs.setId(4L);
            return fs;
        });

        FileStorage saved = fileStorageService.storeFile(file, "BLOCK", null);

        assertThat(saved).isNotNull();
        assertThat(saved.getOriginalName()).isEqualTo("geology_notes.txt");
        assertThat(saved.getFileName()).startsWith("blocks_");
        assertThat(saved.getFilePath()).startsWith("/media/documents/");
        assertThat(saved.isText()).isTrue();
        assertThat(saved.isDocument()).isTrue();
        assertThat(saved.isWord()).isFalse();
        assertThat(saved.isPdf()).isFalse();
        assertThat(saved.isImage()).isFalse();

        Path storedFile = tempUploadDir.resolve("documents").resolve(saved.getFileName());
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readString(storedFile)).isEqualTo("ocak jeolojik gozlemleri: kalsit damari var");
    }

    @Test
    @DisplayName("storeFile should reject empty file")
    void storeFile_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> fileStorageService.storeFile(emptyFile, "BLOCK", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("storeFile should reject path traversal")
    void storeFile_PathTraversal_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "../evil.jpg", "image/jpeg", "content".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("storeFile should reject unsupported file extensions (e.g. .exe, .sh)")
    void storeFile_UnsupportedExtension_ThrowsException() {
        MockMultipartFile badFile = new MockMultipartFile(
                "file", "malware.exe", "application/octet-stream", "bad".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(badFile, "BLOCK", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("attachFilesToEntity should update pending files with entityType and entityId")
    void attachFilesToEntity_UpdatesFiles() {
        FileStorage f1 = FileStorage.builder().id(10L).fileName("blocks_f1.jpg").entityType("BLOCK").build();
        FileStorage f2 = FileStorage.builder().id(11L).fileName("blocks_f2.pdf").entityType("BLOCK").build();

        when(fileStorageRepository.findByIdInAndDeletedFalse(List.of(10L, 11L)))
                .thenReturn(List.of(f1, f2));

        fileStorageService.attachFilesToEntity(List.of(10L, 11L), "BLOCK", 500L);

        assertThat(f1.getEntityId()).isEqualTo(500L);
        assertThat(f2.getEntityId()).isEqualTo(500L);
        verify(fileStorageRepository).saveAll(List.of(f1, f2));
    }

    @Test
    @DisplayName("deleteFileRecord should remove DB record and delete physical file from disk")
    void deleteFileRecord_Success() throws IOException {
        Path physicalFile = tempUploadDir.resolve("images").resolve("blocks_to_delete.png");
        Files.writeString(physicalFile, "delete me");
        assertThat(Files.exists(physicalFile)).isTrue();

        FileStorage fs = FileStorage.builder()
                .id(42L)
                .fileName("blocks_to_delete.png")
                .filePath("images/blocks_to_delete.png")
                .originalName("photo.png")
                .build();

        when(fileStorageRepository.findById(42L)).thenReturn(Optional.of(fs));

        boolean deleted = fileStorageService.deleteFileRecord(42L);

        assertThat(deleted).isTrue();
        assertThat(Files.exists(physicalFile)).isFalse();
        verify(fileStorageRepository).delete(fs);
    }

    @Test
    @DisplayName("deleteAllFilesForEntity should remove all physical files and DB records for that entity")
    void deleteAllFilesForEntity_Success() throws IOException {
        Path p1 = tempUploadDir.resolve("images").resolve("blocks_entity_f1.jpg");
        Path p2 = tempUploadDir.resolve("documents").resolve("blocks_entity_f2.pdf");
        Files.writeString(p1, "p1");
        Files.writeString(p2, "p2");

        FileStorage f1 = FileStorage.builder().id(1L).fileName("blocks_entity_f1.jpg").filePath("images/blocks_entity_f1.jpg").entityType("BLOCK").entityId(99L).build();
        FileStorage f2 = FileStorage.builder().id(2L).fileName("blocks_entity_f2.pdf").filePath("documents/blocks_entity_f2.pdf").entityType("BLOCK").entityId(99L).build();

        when(fileStorageRepository.findByEntityTypeAndEntityId("BLOCK", 99L))
                .thenReturn(List.of(f1, f2));

        fileStorageService.deleteAllFilesForEntity("BLOCK", 99L);

        assertThat(Files.exists(p1)).isFalse();
        assertThat(Files.exists(p2)).isFalse();
        verify(fileStorageRepository).deleteAll(List.of(f1, f2));
    }

    @Test
    @DisplayName("loadAsResource should load readable Resource from disk")
    void loadAsResource_Success() throws IOException {
        Path physicalFile = tempUploadDir.resolve("documents").resolve("blocks_doc.pdf");
        Files.writeString(physicalFile, "pdf-data");

        FileStorage fs = FileStorage.builder()
                .id(15L)
                .fileName("blocks_doc.pdf")
                .filePath("documents/blocks_doc.pdf")
                .originalName("blocks_doc.pdf")
                .build();

        when(fileStorageRepository.findByIdAndDeletedFalse(15L)).thenReturn(Optional.of(fs));

        Resource resource = fileStorageService.loadAsResource(15L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("checkStorageHealth should report UP when media images and documents folders are readable and writable")
    void checkStorageHealth_Success() {
        var health = fileStorageService.checkStorageHealth();

        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo("UP");
    }

    @Test
    @DisplayName("checkStorageHealth should report DOWN when images location is null or inaccessible")
    void checkStorageHealth_WhenLocationNull_ReportsDown() {
        ReflectionTestUtils.setField(fileStorageService, "imagesLocation", null);

        var health = fileStorageService.checkStorageHealth();

        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo("DOWN");
        assertThat(health.getError()).contains("media/images").contains("klasör yolu");
    }
}

