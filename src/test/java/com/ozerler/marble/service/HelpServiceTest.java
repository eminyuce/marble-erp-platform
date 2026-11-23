package com.ozerler.marble.service;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.ozerler.marble.dto.HelpFeedbackResponse;
import com.ozerler.marble.dto.HelpPageDto;

class HelpServiceTest {

    private HelpService helpService;

    @BeforeEach
    void setUp() {
        HelpContentCatalog catalog = new HelpContentCatalog(new DefaultResourceLoader());
        catalog.loadHelpPages();
        helpService = new HelpService(catalog);
    }

    @Test
    @DisplayName("startup cache contains every classpath help HTML file")
    void loadHelpPagesCachesAllClasspathFiles() {
        Assertions.assertThat(helpService.getHelpPage("projects")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("users")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("dashboard")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("production")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("slabs")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("workshop")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("procurement")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("sales")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("blocks")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("genealogy")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("reports")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("costs")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("definitions")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("settings")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("deployment")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("change-password")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("expenses")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("machine-fuel")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("system-health")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("block-move")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("block-move").orElseThrow().getTitle())
                .isEqualTo("Blok Saha Taşıma");
        Assertions.assertThat(helpService.getHelpPage("block-transfer")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("block-transfer").orElseThrow().getTitle())
                .isEqualTo("Fabrikaya Sevk");
        Assertions.assertThat(helpService.getHelpPage("block-photos")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("block-photos").orElseThrow().getTitle())
                .isEqualTo("Ocak — Blok Fotoğrafları & Taş Pasaportu");
        Assertions.assertThat(helpService.getHelpPage("block-sell")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("tablet")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("pallets")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("polish")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("email-preview")).isPresent();
        Assertions.assertThat(helpService.getHelpPage("email-preview").orElseThrow().getTitle())
                .isEqualTo("E-posta Şablon Önizleme");
    }

    @Test
    @DisplayName("getHelpPage returns catalog content for a known key")
    void getHelpPageKnownKey() {
        Optional<HelpPageDto> page = helpService.getHelpPage("change-password");

        Assertions.assertThat(page).isPresent();
        Assertions.assertThat(page.get().getTitle()).isEqualTo("Şifre değiştir");
        Assertions.assertThat(page.get().getFormat()).isEqualTo("html");
        Assertions.assertThat(page.get().getBody()).contains("6 karakter");
        Assertions.assertThat(page.get().getBody()).contains("help-panel-list");
    }

    @Test
    @DisplayName("getHelpPage rejects unknown or unsafe keys")
    void getHelpPageUnknownOrUnsafe() {
        Assertions.assertThat(helpService.getHelpPage("missing-page")).isEmpty();
        Assertions.assertThat(helpService.getHelpPage("../etc/passwd")).isEmpty();
        Assertions.assertThat(helpService.getHelpPage("")).isEmpty();
        Assertions.assertThat(helpService.getHelpPage(null)).isEmpty();
    }

    @Test
    @DisplayName("resolvePageKey maps request paths including nested routes")
    void resolvePageKeyMapsNestedRoutes() {
        Assertions.assertThat(helpService.resolvePageKey("/projects")).contains("projects");
        Assertions.assertThat(helpService.resolvePageKey("/projects/2")).contains("projects");
        Assertions.assertThat(helpService.resolvePageKey("/production/slabs")).contains("slabs");
        Assertions.assertThat(helpService.resolvePageKey("/production/orders/1")).contains("production");
        Assertions.assertThat(helpService.resolvePageKey("/production/polish")).contains("polish");
        Assertions.assertThat(helpService.resolvePageKey("/production/tablet")).contains("tablet");
        Assertions.assertThat(helpService.resolvePageKey("/production/pallets")).contains("pallets");
        Assertions.assertThat(helpService.resolvePageKey("/production/accept")).contains("production");
        Assertions.assertThat(helpService.resolvePageKey("/quarry")).contains("blocks");
        Assertions.assertThat(helpService.resolvePageKey("/factory")).contains("production");
        Assertions.assertThat(helpService.resolvePageKey("/sites")).contains("projects");
        Assertions.assertThat(helpService.resolvePageKey("/cost-analysis")).contains("costs");
        Assertions.assertThat(helpService.resolvePageKey("/admin/definitions")).contains("definitions");
        Assertions.assertThat(helpService.resolvePageKey("/admin/deployment")).contains("deployment");
        Assertions.assertThat(helpService.resolvePageKey("/account/change-password?x=1")).contains("change-password");
        Assertions.assertThat(helpService.resolvePageKey("/expenses")).contains("expenses");
        Assertions.assertThat(helpService.resolvePageKey("/machines/fuel")).contains("machine-fuel");
        Assertions.assertThat(helpService.resolvePageKey("/admin/dashboard/systemhealth")).contains("system-health");
        Assertions.assertThat(helpService.resolvePageKey("/blocks")).contains("blocks");
        Assertions.assertThat(helpService.resolvePageKey("/blocks/12")).contains("blocks");
        Assertions.assertThat(helpService.resolvePageKey("/blocks/12/edit")).contains("blocks");
        Assertions.assertThat(helpService.resolvePageKey("/blocks/12/move")).contains("block-move");
        Assertions.assertThat(
                        helpService.resolvePageKey("/blocks/12/move?targetType=DISPATCH_YARD"))
                .contains("block-move");
        Assertions.assertThat(helpService.resolvePageKey("/blocks/12/transfer-to-factory")).contains("block-transfer");
        Assertions.assertThat(helpService.resolvePageKey("/blocks/12/sell")).contains("block-sell");
        Assertions.assertThat(helpService.resolvePageKey("/blocks/12/photos")).contains("block-photos");
        Assertions.assertThat(helpService.resolvePageKey("/blocks/12/photos/8")).contains("block-photos");
        Assertions.assertThat(helpService.resolvePageKey("/admin/settings/templates/3/preview"))
                .contains("email-preview");
        Assertions.assertThat(helpService.resolvePageKey("/invoices")).contains("invoices");
        Assertions.assertThat(helpService.resolvePageKey("/invoices/10")).contains("invoices");
        Assertions.assertThat(helpService.resolvePageKey("/unknown")).isEmpty();
        Assertions.assertThat(helpService.resolvePageKey("/roles")).isEmpty();
    }

    @Test
    @DisplayName("recordFeedback stores a vote for a known page")
    void recordFeedbackKnownPage() {
        HelpFeedbackResponse response = helpService.recordFeedback("users", true);

        Assertions.assertThat(response.isRecorded()).isTrue();
        Assertions.assertThat(response.isHelpful()).isTrue();
        Assertions.assertThat(response.getPageKey()).isEqualTo("users");
    }

    @Test
    @DisplayName("recordFeedback rejects an unknown page")
    void recordFeedbackUnknownPage() {
        Assertions.assertThatThrownBy(() -> helpService.recordFeedback("no-such-page", false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
