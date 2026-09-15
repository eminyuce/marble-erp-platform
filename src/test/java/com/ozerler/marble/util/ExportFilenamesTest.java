package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ExportFilenamesTest {

    private static final LocalDateTime SAMPLE_TIME = LocalDateTime.of(2026, 9, 15, 12, 21, 45);
    private static final Pattern EXPORT_FILENAME =
            Pattern.compile("^[a-z0-9_-]+_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.[a-z0-9]+$");

    @Test
    @DisplayName("Builds entity_YYYY-MM-DD_HH-mm-ss.ext using a 24-hour timestamp")
    void buildsTimestampedAsciiFilename() {
        String filename = ExportFilenames.build("bloklar", "xlsx", SAMPLE_TIME);

        assertThat(filename).isEqualTo("bloklar_2026-09-15_12-21-45.xlsx");
        assertThat(filename).doesNotContain(":");
        assertThat(filename).matches(EXPORT_FILENAME);
    }

    @ParameterizedTest
    @CsvSource({
            "bloklar, csv, bloklar_2026-09-15_12-21-45.csv",
            "uretim_emirleri, xlsx, uretim_emirleri_2026-09-15_12-21-45.xlsx",
            "plakalar, csv, plakalar_2026-09-15_12-21-45.csv",
            "kesim_emirleri, xlsx, kesim_emirleri_2026-09-15_12-21-45.xlsx",
            "projeler, csv, projeler_2026-09-15_12-21-45.csv",
            "satinalma, xlsx, satinalma_2026-09-15_12-21-45.xlsx",
            "satislar, csv, satislar_2026-09-15_12-21-45.csv",
            "kullanicilar, xlsx, kullanicilar_2026-09-15_12-21-45.xlsx",
            "ocak_bloklari, csv, ocak_bloklari_2026-09-15_12-21-45.csv",
            "faturalar, pdf, faturalar_2026-09-15_12-21-45.pdf"
    })
    @DisplayName("Preserves Turkish entity stems and the requested extension")
    void preservesEntityStemAndExtension(String entity, String extension, String expected) {
        assertThat(ExportFilenames.build(entity, extension, SAMPLE_TIME)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Transliterates Turkish letters and rejects spaces or special characters")
    void transliteratesTurkishAndStripsUnsupportedCharacters() {
        String filename = ExportFilenames.build("Şantiye Projeleri!", "XLSX", SAMPLE_TIME);

        assertThat(filename).isEqualTo("santiye_projeleri_2026-09-15_12-21-45.xlsx");
        assertThat(filename).doesNotContain(":", " ", "ş", "Ş", "!");
        assertThat(filename).matches(EXPORT_FILENAME);
    }

    @Test
    @DisplayName("Uses 24-hour clock including midnight and late evening")
    void usesTwentyFourHourClock() {
        assertThat(ExportFilenames.build("siparisler", "csv", LocalDateTime.of(2026, 1, 2, 0, 5, 9)))
                .isEqualTo("siparisler_2026-01-02_00-05-09.csv");
        assertThat(ExportFilenames.build("siparisler", "csv", LocalDateTime.of(2026, 1, 2, 23, 59, 59)))
                .isEqualTo("siparisler_2026-01-02_23-59-59.csv");
    }

    @Test
    @DisplayName("Normalizes a leading dot on the extension and strips one from the entity")
    void normalizesExistingExtensions() {
        assertThat(ExportFilenames.build("bloklar.csv", ".xlsx", SAMPLE_TIME))
                .isEqualTo("bloklar_2026-09-15_12-21-45.xlsx");
        assertThat(ExportFilenames.build("indirilen", ".pdf", SAMPLE_TIME))
                .isEqualTo("indirilen_2026-09-15_12-21-45.pdf");
    }

    @Test
    @DisplayName("Falls back to the Turkish default stem when the entity name is blank")
    void fallsBackToDefaultStem() {
        assertThat(ExportFilenames.build("   ", "csv", SAMPLE_TIME))
                .isEqualTo(Constants.FALLBACK_DOWNLOAD_FILENAME + "_2026-09-15_12-21-45.csv");
    }

    @Test
    @DisplayName("Every report type yields a valid timestamped Turkish filename")
    void reportTypeExportsFollowConvention() {
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            String csv = ExportFilenames.build(type.getExportFilenameStem(), "csv", SAMPLE_TIME);
            String xlsx = ExportFilenames.build(type.getExportFilenameStem(), "xlsx", SAMPLE_TIME);

            assertThat(csv).startsWith(type.getExportFilenameStem() + "_2026-09-15_12-21-45");
            assertThat(csv).endsWith(".csv");
            assertThat(xlsx).endsWith(".xlsx");
            assertThat(csv).matches(EXPORT_FILENAME);
            assertThat(xlsx).matches(EXPORT_FILENAME);
            assertThat(csv).doesNotContain(":");
        }
    }

    @Test
    @DisplayName("build without a timestamp uses the JVM default timezone and current time")
    void nowUsesSystemDefaultTimezone() {
        String filename = ExportFilenames.build("bloklar", "csv");

        assertThat(filename).matches(EXPORT_FILENAME);
        assertThat(filename).startsWith("bloklar_");
        assertThat(filename).endsWith(".csv");
        assertThat(filename).doesNotContain(":");

        String timestamp = filename.substring("bloklar_".length(), filename.length() - ".csv".length());
        LocalDateTime parsed = LocalDateTime.parse(
                timestamp,
                java.time.format.DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT_EXPORT_FILENAME));
        assertThat(parsed).isBetween(LocalDateTime.now().minusSeconds(2), LocalDateTime.now().plusSeconds(2));
    }
}
