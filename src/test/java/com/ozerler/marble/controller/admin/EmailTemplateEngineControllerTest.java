package com.ozerler.marble.controller.admin;


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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.service.EmailService;

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
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can access email templates index page")
    void adminCanAccessIndexPage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/email-templates"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/email-templates/index"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("templates", "kpi", "categories"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Non-admin user is forbidden from accessing email templates")
    void nonAdminIsForbidden() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/email-templates"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Data endpoint returns JSON response for Tabulator")
    void dataEndpointReturnsJson() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/email-templates/data"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can open create form page")
    void adminCanOpenCreateForm() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/email-templates/create"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/email-templates/form"))
                .andExpect(MockMvcResultMatchers.model().attribute("isEdit", false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can open guide page")
    void adminCanOpenGuidePage() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/email-templates/guide"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/email-templates/guide"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("catalogList"));
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

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/email-templates/" + template.getId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/email-templates/detail"))
                .andExpect(MockMvcResultMatchers.model().attributeExists("template", "preview", "placeholderSamples"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can create and edit custom template")
    void adminCanCreateAndEditTemplate() throws Exception {
        String uniqueKey = "TEST_KEY_" + System.currentTimeMillis();

        // Create
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/email-templates/create")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("templateKey", uniqueKey)
                        .param("templateName", "Otomasyon Şablonu")
                        .param("category", "Sipariş & Satış")
                        .param("description", "Açıklama metni")
                        .param("subject", "Konu {{orderNumber}}")
                        .param("bodyHtml", "<p>Sipariş {{orderNumber}}</p>")
                        .param("placeholders", "orderNumber")
                        .param("isActive", "true"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());

        EmailTemplate created = emailService.getTemplateByKey(uniqueKey).orElseThrow();

        // Edit
        mockMvc.perform(MockMvcRequestBuilders.post("/admin/email-templates/" + created.getId() + "/edit")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .param("templateName", "Güncellenmiş Otomasyon")
                        .param("category", "Sipariş & Satış")
                        .param("description", "Yeni Açıklama")
                        .param("subject", "Yeni Konu {{orderNumber}}")
                        .param("bodyHtml", "<p>Yeni Gövde {{orderNumber}}</p>")
                        .param("placeholders", "orderNumber")
                        .param("isActive", "true"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/email-templates/" + created.getId()));
    }
}
