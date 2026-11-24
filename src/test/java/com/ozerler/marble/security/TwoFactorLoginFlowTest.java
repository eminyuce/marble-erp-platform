package com.ozerler.marble.security;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.result.ModelResultMatchers;
import org.springframework.test.web.servlet.result.StatusResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import com.ozerler.marble.model.enums.TwoFactorMode;
import com.ozerler.marble.repository.RoleRepository;
import com.ozerler.marble.repository.TotpBackupCodeRepository;
import com.ozerler.marble.repository.UserRepository;
import com.ozerler.marble.service.TwoFactorService;
import com.ozerler.marble.service.TwoFactorSettingsService;

/**
 * End-to-end login flow with the second factor, lockout, math question and admin reset.
 * Every request uses its own client address so the per-IP limit never leaks into other tests.
 */
@SpringBootTest
@ActiveProfiles("test")
class TwoFactorLoginFlowTest {

    private static final String LOGIN = "/account/adminlogin/";
    private static final String PASSWORD = "Gizli-Parola-42";
    private static final AtomicInteger IP_SEQUENCE = new AtomicInteger(1);

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private TotpBackupCodeRepository backupCodeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TwoFactorSettingsService settingsService;
    @Autowired
    private TwoFactorService twoFactorService;
    @Autowired
    private TotpService totpService;
    @Autowired
    private TotpEncryptionService encryptionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        settingsService.update(TwoFactorMode.AUTHENTICATOR, false, "test");
    }

    @AfterEach
    void tearDown() {
        settingsService.update(TwoFactorMode.DISABLED, false, "test");
    }

    private static MockHttpServletRequestBuilder get(String url) {
        return MockMvcRequestBuilders.get(url);
    }

    private static MockHttpServletRequestBuilder post(String url) {
        return MockMvcRequestBuilders.post(url);
    }

    private static RequestPostProcessor csrf() {
        return SecurityMockMvcRequestPostProcessors.csrf();
    }

    private static RequestPostProcessor user(String name, String... roles) {
        return SecurityMockMvcRequestPostProcessors.user(name).roles(roles);
    }

    private static ResultMatcher redirectedUrl(String url) {
        return MockMvcResultMatchers.redirectedUrl(url);
    }

    private static StatusResultMatchers status() {
        return MockMvcResultMatchers.status();
    }

    private static ResultMatcher view(String name) {
        return MockMvcResultMatchers.view().name(name);
    }

    private static ModelResultMatchers model() {
        return MockMvcResultMatchers.model();
    }

    private static ResultMatcher authenticated() {
        return SecurityMockMvcResultMatchers.authenticated();
    }

    private static ResultMatcher unauthenticated() {
        return SecurityMockMvcResultMatchers.unauthenticated();
    }

    private static String redirectOf(MvcResult result) {
        return result.getResponse().getRedirectedUrl();
    }

    private static RequestPostProcessor freshIp() {
        int n = IP_SEQUENCE.getAndIncrement();
        String ip = "10.77." + (n / 250) + "." + (n % 250 + 1);
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private Role role(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).description(name).build()));
    }

    private User newUser() {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .username("tfa_" + tag)
                .email("tfa_" + tag + "@ozerler.test")
                .password(passwordEncoder.encode(PASSWORD))
                .firstName("İki")
                .lastName("Faktör")
                .enabled(true)
                .roles(new HashSet<>(Set.of(role("ROLE_ADMIN"), role("ROLE_USER"))))
                .build();
        return userRepository.save(user);
    }

    private String secretOf(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return encryptionService.decrypt(user.getTotpSecretEncrypted(), "totp-secret:" + userId);
    }

    /** Enrols with the previous step's code so the current step is still free for the login. */
    private List<String> enrol(User user) {
        twoFactorService.beginSetup(user.getId());
        String secret = secretOf(user.getId());
        return twoFactorService.confirmSetup(user.getId(),
                totpService.codeAt(secret, System.currentTimeMillis() - TotpService.PERIOD_MILLIS));
    }

    private String code(User user, long offsetSteps) {
        return totpService.codeAt(secretOf(user.getId()),
                System.currentTimeMillis() + offsetSteps * TotpService.PERIOD_MILLIS);
    }

    private MockHttpServletRequestBuilder loginRequest(MockHttpSession session, String username, String password) {
        return post(LOGIN).session(session).with(csrf()).with(freshIp())
                .param("username", username)
                .param("password", password);
    }

    private MvcResult verify(MockHttpSession session, String code) throws Exception {
        return mockMvc.perform(post("/auth/2fa/verify").session(session).with(csrf()).with(freshIp())
                .param("code", code)).andReturn();
    }

    @Test
    @DisplayName("With 2FA disabled an existing user signs in with the password only")
    void disabledModeKeepsPasswordLogin() throws Exception {
        User user = newUser();
        enrol(user);
        settingsService.update(TwoFactorMode.DISABLED, false, "test");
        mockMvc.perform(loginRequest(new MockHttpSession(), user.getEmail(), PASSWORD))
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(SecurityMockMvcResultMatchers.authenticated().withUsername(user.getEmail()));
    }

    @Test
    @DisplayName("Without enrolment and without enforcement the password is enough")
    void notEnrolledNotEnforced() throws Exception {
        User user = newUser();
        mockMvc.perform(loginRequest(new MockHttpSession(), user.getEmail(), PASSWORD))
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(authenticated());
    }

    @Test
    @DisplayName("Password step leaves the user unauthenticated until the code is entered")
    void passwordThenCode() throws Exception {
        User user = newUser();
        enrol(user);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(loginRequest(session, user.getEmail(), PASSWORD))
                .andExpect(redirectedUrl("/auth/2fa/verify"))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(LOGIN));
        mockMvc.perform(get("/auth/2fa/verify").session(session))
                .andExpect(status().isOk())
                .andExpect(view("auth/two-factor-verify"));

        MvcResult result = verify(session, code(user, 0));
        Assertions.assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/dashboard");
        mockMvc.perform(get("/account/2fa").session(session)).andExpect(status().isOk());
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertThat(reloaded.getFailedLoginAttempts()).isZero();
    }

    @Test
    @DisplayName("A used time step cannot be replayed")
    void replayRejected() throws Exception {
        User user = newUser();
        enrol(user);
        String current = code(user, 0);

        MockHttpSession first = new MockHttpSession();
        mockMvc.perform(loginRequest(first, user.getEmail(), PASSWORD));
        Assertions.assertThat(verify(first, current).getResponse().getRedirectedUrl()).isEqualTo("/admin/dashboard");

        MockHttpSession second = new MockHttpSession();
        mockMvc.perform(loginRequest(second, user.getEmail(), PASSWORD));
        Assertions.assertThat(verify(second, current).getResponse().getRedirectedUrl()).isEqualTo("/auth/2fa/verify");
        mockMvc.perform(get("/admin/dashboard").session(second)).andExpect(redirectedUrl(LOGIN));
    }

    @Test
    @DisplayName("Verify page without a pending login goes back to the login page")
    void verifyWithoutPendingRedirects() throws Exception {
        mockMvc.perform(get("/auth/2fa/verify")).andExpect(redirectedUrl(LOGIN));
        mockMvc.perform(post("/auth/2fa/verify").with(csrf()).with(freshIp()).param("code", "123456"))
                .andExpect(redirectedUrl(LOGIN));
        mockMvc.perform(get("/auth/2fa/setup")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Five wrong codes lock the account for 15 minutes; the password alone is then refused")
    void wrongCodesLock() throws Exception {
        User user = newUser();
        enrol(user);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(loginRequest(session, user.getEmail(), PASSWORD));
        String wrong = String.format("%06d", (Integer.parseInt(code(user, 0)) + 500_000) % 1_000_000);
        for (int i = 0; i < 4; i++) {
            Assertions.assertThat(redirectOf(verify(session, wrong))).isEqualTo("/auth/2fa/verify");
        }
        Assertions.assertThat(redirectOf(verify(session, wrong))).isEqualTo(LOGIN + "?locked=true");
        User locked = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertThat(locked.getLockedUntil()).isAfter(java.time.LocalDateTime.now().plusMinutes(14));

        mockMvc.perform(loginRequest(new MockHttpSession(), user.getEmail(), PASSWORD))
                .andExpect(redirectedUrl(LOGIN + "?locked=true"))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/auth/2fa/verify").session(session)).andExpect(redirectedUrl(LOGIN));
    }

    @Test
    @DisplayName("Five wrong passwords lock the account")
    void wrongPasswordsLock() throws Exception {
        User user = newUser();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(loginRequest(new MockHttpSession(), user.getEmail(), "wrong-" + i));
        }
        Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().getLockedUntil()).isNotNull();
        mockMvc.perform(loginRequest(new MockHttpSession(), user.getEmail(), PASSWORD))
                .andExpect(redirectedUrl(LOGIN + "?locked=true"));
    }

    @Test
    @DisplayName("Five failures from one address block that address for the window")
    void ipBlocked() throws Exception {
        RequestPostProcessor sameIp = request -> {
            request.setRemoteAddr("10.99.0.1");
            return request;
        };
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(LOGIN).with(csrf()).with(sameIp)
                    .param("username", "nobody-" + i + "@ozerler.test").param("password", "x"));
        }
        User user = newUser();
        mockMvc.perform(post(LOGIN).with(csrf()).with(sameIp)
                        .param("username", user.getEmail()).param("password", PASSWORD))
                .andExpect(redirectedUrl(LOGIN + "?blocked=true"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("A forged X-Forwarded-For header does not dodge the per-address limit")
    void forwardedHeaderIgnored() throws Exception {
        RequestPostProcessor sameIp = request -> {
            request.setRemoteAddr("10.99.0.2");
            return request;
        };
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(LOGIN).with(csrf()).with(sameIp).header("X-Forwarded-For", "203.0.113." + i)
                    .param("username", "nobody-" + i + "@ozerler.test").param("password", "x"));
        }
        mockMvc.perform(post(LOGIN).with(csrf()).with(sameIp).header("X-Forwarded-For", "203.0.113.200")
                        .param("username", "nobody@ozerler.test").param("password", "x"))
                .andExpect(redirectedUrl(LOGIN + "?blocked=true"));
    }

    @Test
    @DisplayName("A backup code works once")
    void backupCodeSingleUse() throws Exception {
        User user = newUser();
        List<String> codes = enrol(user);
        Assertions.assertThat(codes).hasSize(10);

        MockHttpSession first = new MockHttpSession();
        mockMvc.perform(loginRequest(first, user.getEmail(), PASSWORD));
        Assertions.assertThat(redirectOf(verify(first, codes.get(0)))).isEqualTo("/admin/dashboard");
        Assertions.assertThat(backupCodeRepository.countByUserIdAndUsedAtIsNull(user.getId())).isEqualTo(9);

        MockHttpSession second = new MockHttpSession();
        mockMvc.perform(loginRequest(second, user.getEmail(), PASSWORD));
        Assertions.assertThat(redirectOf(verify(second, codes.get(0)))).isEqualTo("/auth/2fa/verify");
    }

    @Test
    @DisplayName("Remember-me cookie is issued only after the second factor")
    void rememberMeWaitsForSecondFactor() throws Exception {
        User user = newUser();
        enrol(user);
        MockHttpSession session = new MockHttpSession();
        MvcResult password = mockMvc.perform(loginRequest(session, user.getEmail(), PASSWORD)
                .param("remember-me", "true")).andReturn();
        Assertions.assertThat(password.getResponse().getCookie("remember-me")).isNull();
        MvcResult second = verify(session, code(user, 0));
        Assertions.assertThat(second.getResponse().getRedirectedUrl()).isEqualTo("/admin/dashboard");
        Assertions.assertThat(second.getResponse().getCookie("remember-me")).isNotNull();
        Assertions.assertThat(second.getResponse().getCookie("remember-me").getValue()).isNotBlank();
    }

    @Test
    @DisplayName("Enforced 2FA sends a new user through setup, backup codes and then to the dashboard")
    void enforcedSetupFlow() throws Exception {
        settingsService.update(TwoFactorMode.AUTHENTICATOR, true, "test");
        User user = newUser();
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(loginRequest(session, user.getEmail(), PASSWORD))
                .andExpect(redirectedUrl("/auth/2fa/setup"))
                .andExpect(unauthenticated());
        mockMvc.perform(get("/auth/2fa/setup").session(session))
                .andExpect(status().isOk())
                .andExpect(view("auth/two-factor-setup"))
                .andExpect(model().attributeExists("setup"));
        mockMvc.perform(get("/admin/dashboard").session(session)).andExpect(redirectedUrl(LOGIN));

        mockMvc.perform(post("/auth/2fa/setup/verify").session(session).with(csrf()).with(freshIp())
                        .param("code", "000000"))
                .andExpect(redirectedUrl("/auth/2fa/setup"));
        mockMvc.perform(post("/auth/2fa/setup/verify").session(session).with(csrf()).with(freshIp())
                        .param("code", code(user, 0)))
                .andExpect(redirectedUrl("/auth/2fa/setup/backup-codes"));
        mockMvc.perform(get("/auth/2fa/setup/backup-codes").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("backupCodes"));
        mockMvc.perform(post("/auth/2fa/setup/backup-codes").session(session).with(csrf())
                        .param("confirmed", "false"))
                .andExpect(redirectedUrl("/auth/2fa/setup/backup-codes"));
        mockMvc.perform(post("/auth/2fa/setup/backup-codes").session(session).with(csrf())
                        .param("confirmed", "true"))
                .andExpect(redirectedUrl("/admin/dashboard"));
        mockMvc.perform(get("/account/2fa").session(session)).andExpect(status().isOk());

        User enrolled = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertThat(enrolled.isTotpEnabled()).isTrue();
        Assertions.assertThat(enrolled.isTotpSetupCompleted()).isTrue();
        Assertions.assertThat(enrolled.getTotpSecretEncrypted())
                .startsWith("v1:")
                .doesNotContain(secretOf(user.getId()));
        Assertions.assertThat(backupCodeRepository.countByUserIdAndUsedAtIsNull(user.getId())).isEqualTo(10);
        mockMvc.perform(get("/auth/2fa/setup/backup-codes").session(session))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Admin reset clears the secret and backup codes; the user signs in with the password again")
    void adminReset() throws Exception {
        User user = newUser();
        enrol(user);
        mockMvc.perform(post("/admin/users/" + user.getId() + "/reset-2fa").with(csrf())
                        .with(user("admin@eimece.test", "ADMIN")))
                .andExpect(redirectedUrl("/admin/users/" + user.getId()));
        User reset = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertThat(reset.isTotpEnabled()).isFalse();
        Assertions.assertThat(reset.isTotpSetupCompleted()).isFalse();
        Assertions.assertThat(reset.getTotpSecretEncrypted()).isNull();
        Assertions.assertThat(backupCodeRepository.countByUserIdAndUsedAtIsNull(user.getId())).isZero();
        mockMvc.perform(loginRequest(new MockHttpSession(), user.getEmail(), PASSWORD))
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("Admin reset is refused for non-admins")
    void adminResetNeedsAdmin() throws Exception {
        User user = newUser();
        enrol(user);
        mockMvc.perform(post("/admin/users/" + user.getId() + "/reset-2fa").with(csrf())
                        .with(user("plain@ozerler.test", "USER")))
                .andExpect(status().is4xxClientError());
        Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().isTotpEnabled()).isTrue();
    }

    @Test
    @DisplayName("Turning 2FA off needs the password and a fresh code")
    void userDisable() throws Exception {
        User user = newUser();
        enrol(user);
        var principal = user(user.getEmail(), "ADMIN", "USER");
        mockMvc.perform(post("/account/2fa/disable").with(csrf()).with(principal)
                        .param("currentPassword", "wrong").param("code", code(user, 0)))
                .andExpect(redirectedUrl("/account/2fa"));
        Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().isTotpEnabled()).isTrue();
        mockMvc.perform(post("/account/2fa/disable").with(csrf()).with(principal)
                        .param("currentPassword", PASSWORD).param("code", code(user, 0)))
                .andExpect(redirectedUrl("/account/2fa"));
        Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().isTotpEnabled()).isFalse();
    }

    @Test
    @DisplayName("Math question appears after two failures; a wrong answer skips the password check")
    void mathCaptcha() throws Exception {
        User user = newUser();
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(loginRequest(session, user.getEmail(), "wrong-1"))
                .andExpect(redirectedUrl(LOGIN + "?error=true"));
        mockMvc.perform(get(LOGIN).session(session)).andExpect(model().attributeDoesNotExist("captchaQuestion"));
        mockMvc.perform(loginRequest(session, user.getEmail(), "wrong-2"));
        mockMvc.perform(get(LOGIN).session(session))
                .andExpect(model().attributeExists("captchaQuestion"));
        Integer answer = (Integer) session.getAttribute(MathCaptchaService.ANSWER);
        Assertions.assertThat(answer).isBetween(0, 100);
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ISSUED_AT)).isInstanceOf(Long.class);

        mockMvc.perform(loginRequest(session, user.getEmail(), PASSWORD).param("captcha", String.valueOf(answer + 1)))
                .andExpect(redirectedUrl(LOGIN + "?captcha=true"))
                .andExpect(unauthenticated());
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ANSWER)).isNull();

        mockMvc.perform(get(LOGIN + "?captcha=true").session(session))
                .andExpect(model().attributeExists("captchaQuestion"));
        Integer fresh = (Integer) session.getAttribute(MathCaptchaService.ANSWER);
        mockMvc.perform(loginRequest(session, user.getEmail(), PASSWORD).param("captcha", String.valueOf(fresh)))
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(authenticated());
        Assertions.assertThat(session.getAttribute(MathCaptchaService.FAILED_COUNT)).isNull();
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ANSWER)).isNull();
    }

    @Test
    @DisplayName("Admin settings form saves mode and enforcement without a restart")
    void adminSettingsForm() throws Exception {
        var admin = user("settings-admin@ozerler.test", "ADMIN");
        mockMvc.perform(post("/admin/settings/two-factor").with(csrf()).with(admin)
                        .param("mode", "AUTHENTICATOR").param("enforced", "true"))
                .andExpect(redirectedUrl("/admin/settings?tab=security"));
        Assertions.assertThat(settingsService.mode()).isEqualTo(TwoFactorMode.AUTHENTICATOR);
        Assertions.assertThat(settingsService.enforced()).isTrue();
        mockMvc.perform(post("/admin/settings/two-factor").with(csrf()).with(admin)
                        .param("mode", "DISABLED"))
                .andExpect(redirectedUrl("/admin/settings?tab=security"));
        Assertions.assertThat(settingsService.mode()).isEqualTo(TwoFactorMode.DISABLED);
        Assertions.assertThat(settingsService.enforced()).isFalse();
        mockMvc.perform(post("/admin/settings/two-factor").with(csrf())
                        .with(user("plain@ozerler.test", "USER")).param("mode", "AUTHENTICATOR"))
                .andExpect(status().is4xxClientError());
    }
}
