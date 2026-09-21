package com.ozerler.marble.controller.admin;

import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@ActiveProfiles("test")
class EmailTemplatePreviewPageTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private EmailService emailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("settings template cards link to a dedicated preview page")
    void settingsIndexLinksToPreviewPage() throws Exception {
        String source = Files.readString(Path.of("src/main/resources/templates/admin/settings/index.html"),
                StandardCharsets.UTF_8);
        assertThat(source)
                .contains("/admin/settings/templates/{id}/preview")
                .doesNotContain("previewModalOpen")
                .doesNotContain("openPreview(");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("admin can open the email template preview page")
    void adminCanOpenPreviewPage() throws Exception {
        EmailTemplate template = emailService.saveTemplate(EmailTemplate.builder()
                .templateKey("E2E_PREVIEW_" + System.nanoTime())
                .templateName("Önizleme Testi")
                .subject("Merhaba {{fullName}}")
                .bodyHtml("<p>E-posta: {{email}}</p>")
                .placeholders("fullName, email")
                .isActive(true)
                .build());

        mockMvc.perform(get("/admin/settings/templates/" + template.getId() + "/preview"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/template-preview"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("non-admin cannot open the email template preview page")
    void userCannotOpenPreviewPage() throws Exception {
        mockMvc.perform(get("/admin/settings/templates/1/preview"))
                .andExpect(status().isForbidden());
    }
}
