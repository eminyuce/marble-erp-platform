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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class EmailTemplateEngineControllerTest {

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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can access email templates index page")
    void adminCanAccessIndexPage() throws Exception {
        mockMvc.perform(get("/admin/email-templates"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email-templates/index"))
                .andExpect(model().attributeExists("templates", "kpi", "categories"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Non-admin user is forbidden from accessing email templates")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/email-templates"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Data endpoint returns JSON response for Tabulator")
    void dataEndpointReturnsJson() throws Exception {
        mockMvc.perform(get("/admin/email-templates/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can open create form page")
    void adminCanOpenCreateForm() throws Exception {
        mockMvc.perform(get("/admin/email-templates/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email-templates/form"))
                .andExpect(model().attribute("isEdit", false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can open guide page")
    void adminCanOpenGuidePage() throws Exception {
        mockMvc.perform(get("/admin/email-templates/guide"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email-templates/guide"))
                .andExpect(model().attributeExists("catalogList"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can view template detail page")
    void adminCanViewDetailPage() throws Exception {
        EmailTemplate template = emailService.saveTemplate(EmailTemplate.builder()
                .templateKey("E2E_DETAIL_" + System.nanoTime())
                .templateName("Detay Testi")
                .category("Sistem & Güvenlik")
                .subject("Test Başlığı {{fullName}}")
                .bodyHtml("<p>Merhaba {{fullName}}</p>")
                .placeholders("fullName")
                .isActive(true)
                .build());

        mockMvc.perform(get("/admin/email-templates/" + template.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/email-templates/detail"))
                .andExpect(model().attributeExists("template", "preview", "placeholderSamples"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can create and edit custom template")
    void adminCanCreateAndEditTemplate() throws Exception {
        String uniqueKey = "TEST_KEY_" + System.currentTimeMillis();

        // Create
        mockMvc.perform(post("/admin/email-templates/create")
                        .with(csrf())
                        .param("templateKey", uniqueKey)
                        .param("templateName", "Otomasyon Şablonu")
                        .param("category", "Sipariş & Satış")
                        .param("description", "Açıklama metni")
                        .param("subject", "Konu {{orderNumber}}")
                        .param("bodyHtml", "<p>Sipariş {{orderNumber}}</p>")
                        .param("placeholders", "orderNumber")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection());

        EmailTemplate created = emailService.getTemplateByKey(uniqueKey).orElseThrow();

        // Edit
        mockMvc.perform(post("/admin/email-templates/" + created.getId() + "/edit")
                        .with(csrf())
                        .param("templateName", "Güncellenmiş Otomasyon")
                        .param("category", "Sipariş & Satış")
                        .param("description", "Yeni Açıklama")
                        .param("subject", "Yeni Konu {{orderNumber}}")
                        .param("bodyHtml", "<p>Yeni Gövde {{orderNumber}}</p>")
                        .param("placeholders", "orderNumber")
                        .param("isActive", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/email-templates/" + created.getId()));
    }
}
