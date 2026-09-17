package com.ozerler.marble.service;

import com.ozerler.marble.dto.HelpFeedbackResponse;
import com.ozerler.marble.dto.HelpPageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void loadHelpPages_CachesAllClasspathFiles() {
        assertThat(helpService.getHelpPage("projects")).isPresent();
        assertThat(helpService.getHelpPage("users")).isPresent();
        assertThat(helpService.getHelpPage("dashboard")).isPresent();
        assertThat(helpService.getHelpPage("production")).isPresent();
        assertThat(helpService.getHelpPage("slabs")).isPresent();
        assertThat(helpService.getHelpPage("workshop")).isPresent();
        assertThat(helpService.getHelpPage("procurement")).isPresent();
        assertThat(helpService.getHelpPage("sales")).isPresent();
        assertThat(helpService.getHelpPage("blocks")).isPresent();
        assertThat(helpService.getHelpPage("genealogy")).isPresent();
        assertThat(helpService.getHelpPage("reports")).isPresent();
        assertThat(helpService.getHelpPage("costs")).isPresent();
        assertThat(helpService.getHelpPage("definitions")).isPresent();
        assertThat(helpService.getHelpPage("settings")).isPresent();
        assertThat(helpService.getHelpPage("deployment")).isPresent();
        assertThat(helpService.getHelpPage("change-password")).isPresent();
    }

    @Test
    @DisplayName("getHelpPage returns catalog content for a known key")
    void getHelpPage_KnownKey() {
        Optional<HelpPageDto> page = helpService.getHelpPage("change-password");

        assertThat(page).isPresent();
        assertThat(page.get().getTitle()).isEqualTo("Şifre değiştir");
        assertThat(page.get().getFormat()).isEqualTo("html");
        assertThat(page.get().getBody()).contains("6 karakter");
        assertThat(page.get().getBody()).contains("help-panel-list");
    }

    @Test
    @DisplayName("getHelpPage rejects unknown or unsafe keys")
    void getHelpPage_UnknownOrUnsafe() {
        assertThat(helpService.getHelpPage("missing-page")).isEmpty();
        assertThat(helpService.getHelpPage("../etc/passwd")).isEmpty();
        assertThat(helpService.getHelpPage("")).isEmpty();
        assertThat(helpService.getHelpPage(null)).isEmpty();
    }

    @Test
    @DisplayName("resolvePageKey maps request paths including nested routes")
    void resolvePageKey_MapsNestedRoutes() {
        assertThat(helpService.resolvePageKey("/projects")).contains("projects");
        assertThat(helpService.resolvePageKey("/projects/2")).contains("projects");
        assertThat(helpService.resolvePageKey("/production/slabs")).contains("slabs");
        assertThat(helpService.resolvePageKey("/production/orders/1")).contains("production");
        assertThat(helpService.resolvePageKey("/production/polish")).contains("production");
        assertThat(helpService.resolvePageKey("/production/accept")).contains("production");
        assertThat(helpService.resolvePageKey("/quarry")).contains("blocks");
        assertThat(helpService.resolvePageKey("/factory")).contains("production");
        assertThat(helpService.resolvePageKey("/sites")).contains("projects");
        assertThat(helpService.resolvePageKey("/cost-analysis")).contains("costs");
        assertThat(helpService.resolvePageKey("/admin/definitions")).contains("definitions");
        assertThat(helpService.resolvePageKey("/admin/deployment")).contains("deployment");
        assertThat(helpService.resolvePageKey("/account/change-password?x=1")).contains("change-password");
        assertThat(helpService.resolvePageKey("/unknown")).isEmpty();
        assertThat(helpService.resolvePageKey("/roles")).isEmpty();
    }

    @Test
    @DisplayName("recordFeedback stores a vote for a known page")
    void recordFeedback_KnownPage() {
        HelpFeedbackResponse response = helpService.recordFeedback("users", true);

        assertThat(response.isRecorded()).isTrue();
        assertThat(response.isHelpful()).isTrue();
        assertThat(response.getPageKey()).isEqualTo("users");
    }

    @Test
    @DisplayName("recordFeedback rejects an unknown page")
    void recordFeedback_UnknownPage() {
        assertThatThrownBy(() -> helpService.recordFeedback("no-such-page", false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
