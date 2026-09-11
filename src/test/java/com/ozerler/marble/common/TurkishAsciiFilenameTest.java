package com.ozerler.marble.common;

import com.ozerler.marble.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TurkishAsciiFilenameTest {

    private static final Pattern ASCII_FILENAME = Pattern.compile("^[a-z0-9._-]+$");

    @Test
    @DisplayName("Turkish letters are transliterated to ASCII without leftover diacritics")
    void transliteratesTurkishLetters() {
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("kullanıcılar.csv")).isEqualTo("kullanicilar.csv");
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("şantiye_projeleri")).isEqualTo("santiye_projeleri");
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("ocak_blokları")).isEqualTo("ocak_bloklari");
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("plaka_stoğu")).isEqualTo("plaka_stogu");
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("üretim_emirleri")).isEqualTo("uretim_emirleri");
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("İmalat")).isEqualTo("imalat");
    }

    @Test
    @DisplayName("Every report type yields an ASCII Turkish export stem")
    void reportTypeStemsAreAsciiTurkish() {
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            String stem = TurkishAsciiFilename.toAsciiTurkishFilename(type.getExportFilenameStem());
            assertThat(stem).matches(ASCII_FILENAME);
            assertThat(stem.toLowerCase(Locale.ROOT)).isEqualTo(stem);
            assertThat(stem).doesNotContain("report", "export", "users", "blocks", "workshop", "slabs");
        }

        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename(
                ReportService.ReportType.QUARRY_BLOCKS.getExportFilenameStem())).isEqualTo("ocak_bloklari");
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename(
                ReportService.ReportType.COST_ACCOUNTING.getExportFilenameStem())).isEqualTo("maliyet_raporu");
        assertThat(TurkishAsciiFilename.toAsciiTurkishFilename(
                ReportService.ReportType.SLABS_INVENTORY.getExportFilenameStem())).isEqualTo("plaka_stogu");
    }
}
