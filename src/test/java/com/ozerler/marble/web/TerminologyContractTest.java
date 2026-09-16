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
                .contains("/admin/definitions/machines")
                .contains("/admin/definitions/suppliers")
                .doesNotContain("th:href=\"@{/admin/definitions}\"");
        String megaMenu = Files.readString(root.resolve("templates/layout/mega-menu.html"));
        assertThat(megaMenu).doesNotContain("Sistem Tanımları");
        assertThat(megaMenu).doesNotContain("th:href=\"@{/admin/definitions}\"");
        assertThat(megaMenu).contains("admin-mega-card-title\">Tanımlar<");
        assertThat(megaMenu).contains("admin-mega-card-title\">Sistem<");
        assertThat(megaMenu).doesNotContain("Tanımlar &amp; Yönetim");
        int definitionsIdx = megaMenu.indexOf("data-mega-group=\"definitions\"");
        int systemIdx = megaMenu.indexOf("data-mega-group=\"system\"");
        assertThat(definitionsIdx).isGreaterThanOrEqualTo(0);
        assertThat(systemIdx).isGreaterThan(definitionsIdx);
        String definitionsCard = megaMenu.substring(definitionsIdx, systemIdx);
        String systemCard = megaMenu.substring(systemIdx);
        assertThat(definitionsCard)
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
        assertThat(systemCard)
                .contains("/admin/users")
                .contains("/admin/settings")
                .contains("/admin/dashboard/systemhealth/")
                .doesNotContain("/admin/definitions/machines")
                .doesNotContain("/admin/definitions/suppliers");
        assertThat(megaMenu).doesNotContain("/swagger-ui.html");
        assertThat(megaMenu).doesNotContain("/v3/api-docs");
        assertThat(megaMenu).doesNotContain("/actuator");
        assertThat(megaMenu).doesNotContain("/blocks/create");
        assertThat(megaMenu).doesNotContain("/production/create");
        assertThat(megaMenu).doesNotContain("/projects/create");
        assertThat(megaMenu).doesNotContain("/sales/create");
        assertThat(megaMenu).doesNotContain("/procurement/create");
        assertThat(megaMenu).doesNotContain("/workshop/create");
        assertThat(megaMenu).doesNotContain("/admin/users/create");
        assertThat(megaMenu).doesNotContain("Yeni Blok Kaydı");
        assertThat(megaMenu).doesNotContain("Yeni Üretim İş Emri");
        assertThat(megaMenu).contains("data-search=");
        assertThat(megaMenu).contains("data-lucide=");
        assertThat(megaMenu).contains("admin-mega-item");
        assertThat(megaMenu).contains("/admin/definitions/machines");
        assertThat(megaMenu).contains("/admin/definitions/stock-locations");
        assertThat(megaMenu).contains("/admin/definitions/quarries");
        assertThat(megaMenu).contains("/admin/definitions/customers");
        assertThat(megaMenu).contains("/admin/definitions/cost-centers");
        assertThat(megaMenu).contains("th:href=\"@{/expenses}\"");
        String blocks = Files.readString(root.resolve("templates/erp/blocks/index.html"));
        assertThat(blocks).contains("Stok Sahası").doesNotContain("Sevkiyat Sahası");
        String blockForm = Files.readString(root.resolve("templates/erp/blocks/form.html"));
        assertThat(blockForm).contains("<th:block th:if=\"${isEdit and block != null}\">")
                .doesNotContain("name=\"crackLevel\" class=\"erp-form-select\"");
        String auditFragment = Files.readString(root.resolve("templates/fragments/audit.html"));
        assertThat(auditFragment).contains("th:if=\"${createdDate != null}\"");
        String costs = Files.readString(root.resolve("templates/erp/costs/index.html"));
        assertThat(costs).contains("Ocak Maliyet Analizi")
                .contains("Fabrika Maliyet Analizi")
                .contains("Atölye Maliyet Analizi")
                .contains("Şantiye Maliyet Analizi")
                .contains("Fiyat Simülasyonu")
                .contains("Şantiye gideri bir projeye bağlanmalıdır");
    }
}
