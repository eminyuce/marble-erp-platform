package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;

import java.util.Locale;

/**
 * Builds download filenames from Turkish words using ASCII-only letters.
 */
public final class TurkishAsciiFilename {

    private static final String FALLBACK_NAME = Constants.FALLBACK_DOWNLOAD_FILENAME;

    private TurkishAsciiFilename() {
    }

    public static String toAsciiTurkishFilename(String value) {
        if (value == null || value.isBlank()) {
            return FALLBACK_NAME;
        }

        String ascii = value
                .replace("ç", "c").replace("Ç", "C")
                .replace("ğ", "g").replace("Ğ", "G")
                .replace("ı", "i").replace("İ", "I")
                .replace("ö", "o").replace("Ö", "O")
                .replace("ş", "s").replace("Ş", "S")
                .replace("ü", "u").replace("Ü", "U")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "_")
                .replaceAll("^_+|_+$", "");

        return ascii.isEmpty() ? FALLBACK_NAME : ascii;
    }
}
