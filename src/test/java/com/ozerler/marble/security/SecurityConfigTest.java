package com.ozerler.marble.security;

import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.RoleRepository;
import com.ozerler.marble.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .apply(springSecurity())
                .build();

        if (userRepository.findByEmail("admin@eimece.test").isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").description("Yönetici").build()));
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").description("Kullanıcı").build()));
            Role execRole = roleRepository.findByName("ROLE_EXECUTIVE")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_EXECUTIVE").description("Üst Yönetici").build()));

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
    void adminLoginPage_publicAccess() throws Exception {
        mockMvc.perform(get("/account/adminlogin/"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/admin-login"));
    }

    @Test
    @DisplayName("Standard /login should be publicly accessible")
    void loginPage_publicAccess() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/admin-login"));
    }

    @Test
    @DisplayName("Health endpoint /health/ and /health should be publicly accessible and return status UP")
    void healthCheck_publicAccess() throws Exception {
        mockMvc.perform(get("/health/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.dependencies.database.status").value("UP"))
                .andExpect(jsonPath("$.response").doesNotExist())
                .andExpect(jsonPath("$.serviceStatus").doesNotExist());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.dependencies.diskSpace.status").value("UP"));
    }

    @Test
    @DisplayName("Pre-seeded admin admin@eimece.test should authenticate successfully with password B2u5c8JB")
    void adminUser_eimeceAuthentication() throws Exception {
        mockMvc.perform(formLogin("/account/adminlogin/")
                        .user("username", "admin@eimece.test")
                        .password("password", "B2u5c8JB"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(authenticated().withUsername("admin@eimece.test").withRoles("ADMIN", "USER", "EXECUTIVE"));
    }

    @Test
    @DisplayName("Invalid credentials should fail authentication")
    void invalidLogin_fails() throws Exception {
        mockMvc.perform(formLogin("/account/adminlogin/")
                        .user("username", "admin@eimece.test")
                        .password("password", "WRONG_PASSWORD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/adminlogin/?error=true"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("Admin dashboard should redirect unauthenticated request to /account/adminlogin/")
    void adminDashboard_unauthenticatedRedirects() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account/adminlogin/"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("Admin user with ROLE_ADMIN should access /admin/users and /admin/settings")
    void adminUsersAndSettings_allowedForAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/index"))
                .andExpect(content().string(containsString("Roller ve Yetkiler")))
                .andExpect(content().string(containsString("rolesInfoOpen")))
                .andExpect(content().string(containsString("Sistem Yöneticisi")));

        mockMvc.perform(get("/roles"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/index"));

        mockMvc.perform(get("/admin/dashboard/systemhealth/"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system-health"))
                .andExpect(model().attributeExists("overallStatus", "uptime", "components", "rawJson"));

        mockMvc.perform(get("/admin/dashboard/systemhealth"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system-health"));

        mockMvc.perform(get("/admin/dashboard/systemhealth/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.body.overallStatus").value("UP"))
                .andExpect(jsonPath("$.response.body.appPort").value(81));

        mockMvc.perform(get("/admin/dashboard/oursitefeatures/"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/site-features"));

        mockMvc.perform(get("/admin/dashboard/oursitefeatures"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/site-features"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"USER"})
    @DisplayName("Regular user without ROLE_ADMIN should be denied access to /admin/settings")
    void adminSettings_deniedForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@eimece.test", roles = {"ADMIN"})
    @DisplayName("Authorized user should access /reports and export reports in CSV/Excel")
    void reports_allowedAndExportable() throws Exception {
        mockMvc.perform(get("/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("erp/reports/index"));

        mockMvc.perform(get("/reports/export/QUARRY_BLOCKS").param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition",
                        matchesPattern(".*filename=\"ocak_bloklari_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.csv\".*")))
                .andExpect(header().string("Content-Disposition", not(containsString(":"))));

        mockMvc.perform(get("/reports/export/QUARRY_BLOCKS").param("format", "excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition",
                        matchesPattern(".*filename=\"ocak_bloklari_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.xlsx\".*")))
                .andExpect(header().string("Content-Disposition", not(containsString(":"))));
    }

    @Test
    @DisplayName("Public digital stone passport should be accessible without authentication")
    void digitalPassport_publicAccess() throws Exception {
        mockMvc.perform(get("/passport/NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("erp/passport/view"));
    }

    @Test
    @WithMockUser(username = "operator@example.com", roles = {"OPERATOR"})
    @DisplayName("Any authenticated user should access system health dashboard and see health icon in navbar")
    void systemHealth_accessibleToAnyAuthenticatedUserAndIconPresent() throws Exception {
        mockMvc.perform(get("/admin/dashboard/systemhealth/"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system-health"))
                .andExpect(content().string(containsString("adminTopbarSystemHealth")))
                .andExpect(content().string(containsString("admin-topbar-health")))
                .andExpect(content().string(containsString("/admin/dashboard/systemhealth/")));
    }
}
