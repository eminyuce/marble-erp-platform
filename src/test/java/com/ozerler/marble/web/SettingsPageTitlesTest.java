package com.ozerler.marble.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SettingsPageTitlesTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates/admin/settings");

    @Test
    void everySettingsSectionAndChildPageHasATitle() throws Exception {
        String settings = Files.readString(TEMPLATES.resolve("index.html"), StandardCharsets.UTF_8);
        Assertions.assertThat(settings)
                .contains("pageTitle()")
                .contains("Genel Ayarlar")
                .contains("2FA ve reCAPTCHA")
                .contains("İstek Sınırlandırma")
                .contains("SMTP Posta Sunucusu")
                .contains("E-Posta Şablonları")
                .contains("Araçlar ve Bakım")
                .contains("Yedekleme ve RustFS")
                .contains("class=\"admin-page-title\"");

        Assertions.assertThat(Files.readString(TEMPLATES.resolve("logs.html"), StandardCharsets.UTF_8))
                .contains("class=\"admin-page-title")
                .contains("Uygulama Logları");
        Assertions.assertThat(Files.readString(TEMPLATES.resolve("template-form.html"), StandardCharsets.UTF_8))
                .contains("class=\"admin-page-title\"")
                .contains("E-posta şablonunu düzenle");
        Assertions.assertThat(Files.readString(TEMPLATES.resolve("template-preview.html"), StandardCharsets.UTF_8))
                .contains("class=\"admin-page-title\"")
                .contains("E-posta şablonu canlı önizleme");
    }
}
