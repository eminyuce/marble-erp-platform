package com.ozerler.marble.security;


import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.RoleRepository;
import com.ozerler.marble.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (userRepository.findByEmail("admin@eimece.test").isEmpty()) {
            Role adminRole =
                    roleRepository
                            .findByName("ROLE_ADMIN")
                            .orElseGet(
                                    () ->
                                            roleRepository.save(
                                                    Role.builder()
                                                            .name("ROLE_ADMIN")
                                                            .description("Yönetici")
                                                            .build()));
            Role userRole =
                    roleRepository
                            .findByName("ROLE_USER")
                            .orElseGet(
                                    () ->
                                            roleRepository.save(
                                                    Role.builder()
                                                            .name("ROLE_USER")
                                                            .description("Kullanıcı")
                                                            .build()));
            Role execRole =
                    roleRepository
                            .findByName("ROLE_EXECUTIVE")
                            .orElseGet(
                                    () ->
                                            roleRepository.save(
                                                    Role.builder()
                                                            .name("ROLE_EXECUTIVE")
                                                            .description("Üst Yönetici")
                                                            .build()));

            User admin = User.builder()
                    .username("admin_eimece")
                    .email("admin@eimece.test")
                    .password(passwordEncoder.encode("B2u5c8JB"))
                    .firstName("Eimece")
                    .lastName("Yönetici")
                    .enabled(true)
                    .roles(new HashSet<>(Set.of(adminRole, userRole, execRole)))
                    .build();
            userRepository.save(admin);
        }
    }

    @Test
    @DisplayName("Admin login page /account/adminlogin/ should be publicly accessible")
    void adminLoginPagePublicAccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/account/adminlogin/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("auth/admin-login"));
    }

    @Test
    @DisplayName("Standard /login should be publicly accessible")
    void loginPagePublicAccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("auth/admin-login"));
    }

    @Test
    @DisplayName("Health endpoint /health/ and /health should be publicly accessible and return status UP")
    void healthCheckPublicAccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/health/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.dependencies").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.response").doesNotExist())
                .andExpect(MockMvcResultMatchers.jsonPath("$.serviceStatus").doesNotExist());

        mockMvc.perform(MockMvcRequestBuilders.get("/health"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.dependencies").doesNotExist());

        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/metrics"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @Test
    @DisplayName("Pre-seeded admin admin@eimece.test should authenticate successfully with password B2u5c8JB")
    void adminUserEimeceAuthentication() throws Exception {
        mockMvc.perform(
                        SecurityMockMvcRequestBuilders.formLogin("/account/adminlogin/")
                                .user("username", "admin@eimece.test")
                                .password("password", "B2u5c8JB"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/admin/dashboard"))
                .andExpect(
                        SecurityMockMvcResultMatchers.authenticated()
                                .withUsername("admin@eimece.test")
                                .withRoles("ADMIN", "USER", "EXECUTIVE"));
    }

    @Test
    @DisplayName("Invalid credentials should fail authentication")
    void invalidLoginFails() throws Exception {
        mockMvc.perform(SecurityMockMvcRequestBuilders.formLogin("/account/adminlogin/")
                        .user("username", "admin@eimece.test")
                        .password("password", "WRONG_PASSWORD"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/account/adminlogin/?error=true"))
                .andExpect(SecurityMockMvcResultMatchers.unauthenticated());
    }

    @Test
    @DisplayName("Admin dashboard should redirect unauthenticated request to /account/adminlogin/")
    void adminDashboardUnauthenticatedRedirects() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/account/adminlogin/"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Admin user with ROLE_ADMIN should access /admin/users and /admin/settings")
    void adminUsersAndSettingsAllowedForAdmin() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/users"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/users/index"))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Roller ve Yetkiler")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("rolesInfoOpen")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Sistem Yöneticisi")));

        mockMvc.perform(MockMvcRequestBuilders.get("/roles"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/settings/index"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/deployment"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/deployment"))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Üretim Yayını")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("deployment-start-btn")));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard/systemhealth/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/system-health"))
                .andExpect(
                        MockMvcResultMatchers.model()
                                .attributeExists(
                                        "overallStatus", "uptime", "components", "rawJson"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard/systemhealth"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/system-health"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard/systemhealth/api"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.body.overallStatus").value("UP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.body.appPort").value(81));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard/oursitefeatures/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/site-features"));

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard/oursitefeatures"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/site-features"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    @DisplayName("Regular user without ROLE_ADMIN should be denied access to /admin/settings")
    void adminSettingsDeniedForRegularUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/settings"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        mockMvc.perform(MockMvcRequestBuilders.get("/admin/deployment"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@eimece.test", roles = {"ADMIN"})
    @DisplayName("Authorized user should access /reports and export reports in CSV/Excel")
    void reportsAllowedAndExportable() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/reports"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("erp/reports/index"));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/reports/export/QUARRY_BLOCKS")
                                .param("format", "csv"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string("Content-Type", Matchers.containsString("text/csv")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        "Content-Disposition",
                                        Matchers.matchesPattern(
                                                ".*filename=\"ocak_bloklari_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"
                                                        + "\\.csv\".*")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        "Content-Disposition",
                                        Matchers.not(Matchers.containsString(":"))));

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/reports/export/QUARRY_BLOCKS")
                                .param("format", "excel"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        "Content-Type",
                                        Matchers.containsString(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        "Content-Disposition",
                                        Matchers.matchesPattern(
                                                ".*filename=\"ocak_bloklari_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"
                                                        + "\\.xlsx\".*")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        "Content-Disposition",
                                        Matchers.not(Matchers.containsString(":"))));
    }

    @Test
    @WithMockUser(username = "admin@eimece.test", roles = {"ADMIN"})
    @DisplayName("A POST with a bad CSRF token is forbidden instead of method not allowed")
    void wrongCsrfReturnsForbidden() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/projects/create")
                                .with(
                                        SecurityMockMvcRequestPostProcessors.csrf()
                                                .useInvalidToken()))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @DisplayName("Public digital stone passport should be accessible without authentication")
    void digitalPassportPublicAccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/passport/NONEXISTENT"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("erp/passport/view"));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = {"OPERATOR"})
    @DisplayName("Any authenticated user should access system health dashboard and see health icon in navbar")
    void systemHealthAccessibleToAnyAuthenticatedUserAndIconPresent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/admin/dashboard/systemhealth/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("admin/system-health"))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(Matchers.containsString("adminTopbarSystemHealth")))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(Matchers.containsString("admin-topbar-health")))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(Matchers.containsString("/admin/dashboard/systemhealth/")));
    }

    @Test
    @DisplayName("favicon, manifest and robots.txt are public and robots.txt has the exact Allow body")
    void anonymousBrandAssetsAndRobotsArePublic() throws Exception {
        String expectedRobots = "User-agent: *\n"
                + "Allow: /\n"
                + "\n"
                + "# Indexing is blocked via noindex and the X-Robots-Tag header.\n";

        mockMvc.perform(MockMvcRequestBuilders.get("/favicon.ico"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/apple-touch-icon.png"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/icons/favicon-32x32.png"))
                .andExpect(MockMvcResultMatchers.status().isOk());
        mockMvc.perform(MockMvcRequestBuilders.get("/site.webmanifest"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("application/manifest+json"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Özerler Mermer ERP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.short_name").value("Özerler ERP"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.theme_color").value("#5A0048"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.icons[2].purpose").value("maskable"));

        String body = mockMvc.perform(MockMvcRequestBuilders.get("/robots.txt"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("text/plain"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(expectedRobots, body);
    }

    @Test
    @DisplayName("X-Robots-Tag is present on the login page and a regular application page")
    void xRobotsTagPresentOnPages() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/account/adminlogin/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("X-Robots-Tag", "noindex, nofollow, noarchive"));

        mockMvc.perform(MockMvcRequestBuilders.get("/access-denied"))
                .andExpect(MockMvcResultMatchers.header().string("X-Robots-Tag", "noindex, nofollow, noarchive"));
    }
}
