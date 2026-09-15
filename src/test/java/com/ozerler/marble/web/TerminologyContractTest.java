package com.ozerler.marble.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TerminologyContractTest {

    private static final List<String> FORBIDDEN = List.of(
            "1. Ocak & Blok",
            "1. Ocak &amp; Blok",
            "Fabrika &amp; Katrak",
            "Fabrika & Katrak",
            "Plaka Ambarı",
            "Kesim Emri",
            "Ebatlama Emri",
            "Maliyet Muhasebesi",
            "Sistem Sağlığı (Health)",
            "Dikey Yarma",
            "Maliyet &amp; Fiyatlama",
            "Maliyet & Fiyatlama",
            "Proje &amp; Şantiye",
            "Atölye &amp; Ebatlama",
            "Katrak Kesim Emri",
            "Akıllı Fiyatlama",
            "Moloz / Düşük",
            "Sevkiyat Sahası"
    );

    @Test
    @DisplayName("UI templates, help and grids do not use retired module names")
    void screensUseCanonicalTurkishTerms() throws Exception {
        Path root = Path.of("src/main/resources");
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> sources = files
                    .filter(path -> {
                        String name = path.toString();
                        return name.endsWith(".html") || name.endsWith(".js") || name.endsWith(".properties");
                    })
                    .filter(Files::isRegularFile)
                    .toList();
            assertThat(sources).isNotEmpty();
            for (Path file : sources) {
                String text = Files.readString(file);
                for (String forbidden : FORBIDDEN) {
                    assertThat(text)
                            .as("%s must not contain '%s'", root.relativize(file), forbidden)
                            .doesNotContain(forbidden);
                }
            }
        }

        String sidebar = Files.readString(root.resolve("templates/layout/sidebar.html"));
        assertThat(sidebar).contains(">Ocak<").contains(">Fabrika<").contains(">Atölye<")
                .contains(">Şantiyeler<").contains(">Maliyet Analizi<").contains(">Plaka Stok Sahası<")
                .contains("Sistem Tanımları");
        String megaMenu = Files.readString(root.resolve("templates/layout/mega-menu.html"));
        assertThat(megaMenu).contains("Sistem Tanımları");
        String blocks = Files.readString(root.resolve("templates/erp/blocks/index.html"));
        assertThat(blocks).contains("Stok Sahası").doesNotContain("Sevkiyat Sahası");
        String costs = Files.readString(root.resolve("templates/erp/costs/index.html"));
        assertThat(costs).contains("Ocak Maliyet Analizi")
                .contains("Fabrika Maliyet Analizi")
                .contains("Atölye Maliyet Analizi")
                .contains("Şantiye Maliyet Analizi")
                .contains("Fiyat Simülasyonu")
                .contains("Şantiye gideri bir projeye bağlanmalıdır");
    }
}
