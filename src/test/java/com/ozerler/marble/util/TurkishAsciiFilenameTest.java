package com.ozerler.marble.util;

import java.util.Locale;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.service.ReportService;

class TurkishAsciiFilenameTest {

    private static final Pattern ASCII_FILENAME = Pattern.compile("^[a-z0-9._-]+$");

    @Test
    @DisplayName("Turkish letters are transliterated to ASCII without leftover diacritics")
    void transliteratesTurkishLetters() {
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("kullanıcılar.csv")).isEqualTo("kullanicilar.csv");
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("şantiye_projeleri")).isEqualTo("santiye_projeleri");
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("ocak_blokları")).isEqualTo("ocak_bloklari");
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("plaka_stoğu")).isEqualTo("plaka_stogu");
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("üretim_emirleri")).isEqualTo("uretim_emirleri");
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename("İmalat")).isEqualTo("imalat");
    }

    @Test
    @DisplayName("Every report type yields an ASCII Turkish export stem")
    void reportTypeStemsAreAsciiTurkish() {
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            String stem = TurkishAsciiFilename.toAsciiTurkishFilename(type.getExportFilenameStem());
            Assertions.assertThat(stem).matches(ASCII_FILENAME);
            Assertions.assertThat(stem.toLowerCase(Locale.ROOT)).isEqualTo(stem);
            Assertions.assertThat(stem).doesNotContain("report", "export", "users", "blocks", "workshop", "slabs");
        }

        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename(
                ReportService.ReportType.QUARRY_BLOCKS.getExportFilenameStem())).isEqualTo("ocak_bloklari");
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename(
                ReportService.ReportType.COST_ACCOUNTING.getExportFilenameStem())).isEqualTo("maliyet_raporu");
        Assertions.assertThat(TurkishAsciiFilename.toAsciiTurkishFilename(
                ReportService.ReportType.SLABS_INVENTORY.getExportFilenameStem())).isEqualTo("plaka_stogu");
    }
}
