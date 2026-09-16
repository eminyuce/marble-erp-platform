package com.ozerler.marble.validation;

import com.ozerler.marble.config.ObjectStorageProperties;
import com.ozerler.marble.exception.FileValidationException;
import com.ozerler.marble.support.TestFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadValidatorTest {

    private FileUploadValidator validator;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setMaxFileSizeBytes(1024);
        validator = new FileUploadValidator(properties, new SvgContentValidator());
    }

    @Test
    @DisplayName("valid PNG is accepted")
    void validPng() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", TestFiles.png());
        FileUploadValidator.ValidatedUpload result = validator.validate(file);
        assertThat(result.cleanExtension()).isEqualTo("png");
        assertThat(result.mimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("valid WebP is accepted")
    void validWebp() {
        MockMultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", TestFiles.webp());
        assertThat(validator.validate(file).cleanExtension()).isEqualTo("webp");
    }

    @Test
    @DisplayName("oversized file is rejected")
    void oversizedFile() {
        byte[] big = new byte[2048];
        System.arraycopy(TestFiles.jpeg(), 0, big, 0, TestFiles.jpeg().length);
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", big);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    @DisplayName("empty file is rejected")
    void emptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("invalid extension is rejected")
    void invalidExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", "MZ".getBytes());
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileValidationException.class);
    }

    @Test
    @DisplayName("MIME type that does not match the extension is rejected")
    void mimeMismatch() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "application/pdf", TestFiles.jpeg());
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileValidationException.class)
                .hasMessageContaining("content type");
    }

    @Test
    @DisplayName("malicious filename with path traversal is rejected")
    void maliciousFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "..\\evil.jpg", "image/jpeg", TestFiles.jpeg());
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(FileValidationException.class);
    }
}
