package com.ozerler.marble.controller.admin;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.ozerler.marble.dto.RateLimitSettings;
import com.ozerler.marble.service.RateLimitSettingsService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SettingControllerRateLimitTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RateLimitSettingsService rateLimitSettingsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        rateLimitSettingsService.resetToDefaults();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("the settings page renders the rate-limit form with Turkish labels and YAML fallbacks")
    void rendersRateLimitForm() throws Exception {
        String html = mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Assertions.assertThat(html).contains("İstek sınırlandırmayı aç");
        Assertions.assertThat(html).contains("En fazla istek");
        Assertions.assertThat(html).contains("Pencere (saniye)");
        Assertions.assertThat(html).contains("Varsayılana dön");
        Assertions.assertThat(html).contains("İstek Sınırlarını Kaydet");
        Assertions.assertThat(html).contains("application.yml");
        Assertions.assertThat(html).contains("varsayılan değerler kullanılır");
        Assertions.assertThat(html).doesNotContain("yeniden başlatın");
        Assertions.assertThat(html).doesNotContain("Yapılandırma Dosyası");
        Assertions.assertThat(html).contains("name=\"rate-limiting.login.max-requests\"");
        Assertions.assertThat(html).contains("name=\"rate-limiting.form.max-requests\"");
        Assertions.assertThat(html).contains("name=\"rate-limiting.general.max-requests\"");
        Assertions.assertThat(html).contains("value=\"10\"");
        Assertions.assertThat(html).contains("value=\"30\"");
        Assertions.assertThat(html).contains("value=\"120\"");
        Assertions.assertThat(html).contains("/admin/settings/rate-limiting");
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    @DisplayName("a non-admin cannot view or change rate-limit settings")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(saveRequest(true, "4", "90", "15", "45", "80", "30")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/settings/rate-limiting/reset")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(username = "exec@example.com", roles = {"EXECUTIVE"})
    @DisplayName("ROLE_EXECUTIVE cannot change rate-limit settings")
    void executiveIsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mockMvc.perform(saveRequest(true, "4", "90", "15", "45", "80", "30")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @DisplayName("anonymous users are redirected to login")
    void anonymousIsRedirected() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
        mockMvc.perform(saveRequest(true, "4", "90", "15", "45", "80", "30")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("a save without a CSRF token is rejected")
    void saveWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(saveRequest(true, "4", "90", "15", "45", "80", "30"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        Assertions.assertThat(rateLimitSettingsService.effective().getLoginMaxRequests()).isEqualTo(10);
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("invalid values are rejected and the YAML defaults stay in effect")
    void validationRejectedOnController() throws Exception {
        mockMvc.perform(saveRequest(true, "0", "60", "30", "60", "120", "60")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/settings?tab=rate-limiting"))
                .andExpect(MockMvcResultMatchers.flash().attributeExists("errorMessage"));
        Assertions.assertThat(rateLimitSettingsService.effective())
                .isEqualTo(rateLimitSettingsService.yamlDefaults());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("an admin can save overrides and reset them back to YAML defaults")
    void adminCanSaveAndReset() throws Exception {
        mockMvc.perform(saveRequest(false, "4", "90", "15", "45", "80", "30")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/settings?tab=rate-limiting"))
                .andExpect(MockMvcResultMatchers.flash().attributeExists("successMessage"));

        RateLimitSettings saved = rateLimitSettingsService.effective();
        Assertions.assertThat(saved.isEnabled()).isFalse();
        Assertions.assertThat(saved.getLoginMaxRequests()).isEqualTo(4);
        Assertions.assertThat(saved.getLoginWindowSeconds()).isEqualTo(90);
        Assertions.assertThat(saved.getFormMaxRequests()).isEqualTo(15);
        Assertions.assertThat(saved.getGeneralMaxRequests()).isEqualTo(80);

        mockMvc.perform(MockMvcRequestBuilders.post("/admin/settings/rate-limiting/reset")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.flash().attributeExists("successMessage"));

        Assertions.assertThat(rateLimitSettingsService.effective())
                .isEqualTo(rateLimitSettingsService.yamlDefaults());
        Assertions.assertThat(rateLimitSettingsService.hasOverride()).isFalse();
    }

    private static MockHttpServletRequestBuilder saveRequest(
            boolean enabled,
            String loginMax,
            String loginWindow,
            String formMax,
            String formWindow,
            String generalMax,
            String generalWindow) {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/admin/settings/rate-limiting")
                .param("rate-limiting.login.max-requests", loginMax)
                .param("rate-limiting.login.window-seconds", loginWindow)
                .param("rate-limiting.form.max-requests", formMax)
                .param("rate-limiting.form.window-seconds", formWindow)
                .param("rate-limiting.general.max-requests", generalMax)
                .param("rate-limiting.general.window-seconds", generalWindow);
        if (enabled) {
            request.param("security.rate_limiting.enabled", "true");
        }
        return request;
    }
}
