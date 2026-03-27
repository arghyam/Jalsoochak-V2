package org.arghyam.jalsoochak.tenant.dto.internal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("LogoSource factory tests")
class LogoSourceTest {

    @Test
    @DisplayName("Should return FileSource when only a non-empty file is provided")
    void from_fileOnly_returnsFileSource() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        LogoSource result = LogoSource.from(file, null);

        assertInstanceOf(LogoSource.FileSource.class, result);
    }

    @Test
    @DisplayName("Should return UrlSource when only a URL is provided")
    void from_urlOnly_returnsUrlSource() {
        LogoSource result = LogoSource.from(null, "https://cdn.example.com/logo.png");

        assertInstanceOf(LogoSource.UrlSource.class, result);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when neither is provided")
    void from_neitherProvided_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> LogoSource.from(null, null));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file is empty and URL is blank")
    void from_emptyFileAndBlankUrl_throwsIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> LogoSource.from(emptyFile, "  "));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when both file and URL are provided")
    void from_bothProvided_throwsIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> LogoSource.from(file, "https://cdn.example.com/logo.png"));
    }

    @Test
    @DisplayName("Should treat empty file as absent — falls back to URL")
    void from_emptyFileWithUrl_returnsUrlSource() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);

        LogoSource result = LogoSource.from(emptyFile, "https://cdn.example.com/logo.png");

        assertInstanceOf(LogoSource.UrlSource.class, result);
    }

    @Test
    @DisplayName("Should accept uppercase HTTP scheme (case-insensitive)")
    void urlSource_uppercaseHttpScheme_isAccepted() {
        // Should not throw — equalsIgnoreCase must handle uppercase schemes
        LogoSource result = LogoSource.from(null, "HTTPS://cdn.example.com/logo.png");

        assertInstanceOf(LogoSource.UrlSource.class, result);
    }

    @Test
    @DisplayName("Should reject URL with no scheme (null scheme)")
    void urlSource_nullScheme_throwsIllegalArgumentException() {
        // A protocol-relative URL like "//cdn.example.com/logo.png" has a null scheme
        assertThrows(IllegalArgumentException.class,
                () -> LogoSource.from(null, "//cdn.example.com/logo.png"));
    }
}
