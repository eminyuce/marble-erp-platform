package com.ozerler.marble.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
            Assertions.assertThat(sources).isNotEmpty();
            for (Path file : sources) {
                String text = Files.readString(file);
                for (String forbidden : FORBIDDEN) {
                    Assertions.assertThat(text)
                            .as("%s must not contain '%s'", root.relativize(file), forbidden)
                            .doesNotContain(forbidden);
                }
            }
        }

        String sidebar = Files.readString(root.resolve("templates/layout/sidebar.html"));
        Assertions.assertThat(sidebar).contains(">Ocaklar<").contains(">Fabrika<").contains(">Atölye<")
                .contains(">Şantiyeler<").contains(">Maliyet Analizi<")
                .contains("/admin/definitions/machines")
                .contains("/admin/definitions/suppliers")
                .doesNotContain("th:href=\"@{/admin/definitions}\"");
        String megaMenu = Files.readString(root.resolve("templates/layout/mega-menu.html"));
        Assertions.assertThat(megaMenu).contains(">Ocaklar — Blok Takibi<");
        Assertions.assertThat(megaMenu).doesNotContain("Sistem Tanımları");
        Assertions.assertThat(megaMenu).doesNotContain("th:href=\"@{/admin/definitions}\"");
        Assertions.assertThat(megaMenu).contains("admin-mega-card-title\">Tanımlar<");
        Assertions.assertThat(megaMenu).contains("admin-mega-card-title\">Sistem<");
        Assertions.assertThat(megaMenu).doesNotContain("Tanımlar &amp; Yönetim");
        int definitionsIdx = megaMenu.indexOf("data-mega-group=\"definitions\"");
        int systemIdx = megaMenu.indexOf("data-mega-group=\"system\"");
        Assertions.assertThat(definitionsIdx).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(systemIdx).isGreaterThan(definitionsIdx);
        String definitionsCard = megaMenu.substring(definitionsIdx, systemIdx);
        String systemCard = megaMenu.substring(systemIdx);
        Assertions.assertThat(definitionsCard)
                .doesNotContain("th:href=\"@{/admin/definitions}\"")
                .contains("/admin/definitions/machines")
                .contains("/admin/definitions/stock-locations")
                .contains("/admin/definitions/quarries")
                .contains("/admin/definitions/customers")
                .contains("/admin/definitions/suppliers")
                .contains("/admin/definitions/cost-centers")
                .doesNotContain("/admin/users")
                .doesNotContain("/admin/settings")
                .doesNotContain("systemhealth");
        Assertions.assertThat(systemCard)
                .contains("/admin/users")
                .contains("/admin/settings")
                .contains("/admin/dashboard/systemhealth/")
                .doesNotContain("/admin/definitions/machines")
                .doesNotContain("/admin/definitions/suppliers");
        Assertions.assertThat(megaMenu).doesNotContain("/swagger-ui.html");
        Assertions.assertThat(megaMenu).doesNotContain("/v3/api-docs");
        Assertions.assertThat(megaMenu).doesNotContain("/actuator");
        Assertions.assertThat(megaMenu).doesNotContain("/blocks/create");
        Assertions.assertThat(megaMenu).doesNotContain("/production/create");
        Assertions.assertThat(megaMenu).doesNotContain("/projects/create");
        Assertions.assertThat(megaMenu).doesNotContain("/sales/create");
        Assertions.assertThat(megaMenu).doesNotContain("/procurement/create");
        Assertions.assertThat(megaMenu).doesNotContain("/workshop/create");
        Assertions.assertThat(megaMenu).doesNotContain("/admin/users/create");
        Assertions.assertThat(megaMenu).doesNotContain("Yeni Blok Kaydı");
        Assertions.assertThat(megaMenu).doesNotContain("Yeni Üretim İş Emri");
        Assertions.assertThat(megaMenu).contains("data-search=");
        Assertions.assertThat(megaMenu).contains("data-lucide=");
        Assertions.assertThat(megaMenu).contains("admin-mega-item");
        Assertions.assertThat(megaMenu).contains("/admin/definitions/machines");
        Assertions.assertThat(megaMenu).contains("/admin/definitions/stock-locations");
        Assertions.assertThat(megaMenu).contains("/admin/definitions/quarries");
        Assertions.assertThat(megaMenu).contains("/admin/definitions/customers");
        Assertions.assertThat(megaMenu).contains("/admin/definitions/cost-centers");
        Assertions.assertThat(megaMenu).contains("th:href=\"@{/expenses}\"");
        String blocks = Files.readString(root.resolve("templates/erp/blocks/index.html"));
        Assertions.assertThat(blocks).contains("Stok Sahası").doesNotContain("Sevkiyat Sahası");
        String blockForm = Files.readString(root.resolve("templates/erp/blocks/form.html"));
        Assertions.assertThat(blockForm).contains("<th:block th:if=\"${isEdit and block != null}\">")
                .doesNotContain("name=\"crackLevel\" class=\"erp-form-select\"");
        String auditFragment = Files.readString(root.resolve("templates/fragments/audit.html"));
        Assertions.assertThat(auditFragment).contains("th:if=\"${createdDate != null}\"");
        String costs = Files.readString(root.resolve("templates/erp/costs/index.html"));
        Assertions.assertThat(costs).contains("Ocak Maliyet Analizi")
                .contains("Fabrika Maliyet Analizi")
                .contains("Atölye Maliyet Analizi")
                .contains("Şantiye Maliyet Analizi")
                .contains("Fiyat Simülasyonu")
                .contains("Şantiye gideri bir projeye bağlanmalıdır");

        String settings = Files.readString(root.resolve("templates/admin/settings/index.html"));
        Assertions.assertThat(settings).doesNotContain(">Deployment<").contains(">Yeni Gelişmeleri Sahaya Taşı<");
    }
}
