package com.ozerler.marble.session;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two application contexts share one Postgres database. The second context can
 * read the session only if it was stored in SPRING_SESSION, not in Tomcat memory.
 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcSessionStoreTest {

    private static final String PRINCIPAL = "admin@eimece.test";
    private static final String SECURITY_CONTEXT = HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16-alpine"));

    private static ConfigurableApplicationContext writer;
    private static ConfigurableApplicationContext reader;

    @BeforeAll
    static void startContexts() {
        writer = startContext();
        reader = startContext();
    }

    @AfterAll
    static void stopContexts() {
        if (reader != null) {
            reader.close();
        }
        if (writer != null) {
            writer.close();
        }
    }

    @Test
    @DisplayName("A session saved in one context, including SecurityContext, is visible in another")
    void sessionSurvivesASecondApplicationContext() {
        SessionRepository<?> writerRepository = writer.getBean(SessionRepository.class);
        String sessionId = saveLogin(writerRepository);

        Session loaded = find(reader.getBean(SessionRepository.class), sessionId);
        assertThat(loaded).isNotNull();
        SecurityContext restored = loaded.getAttribute(SECURITY_CONTEXT);
        assertThat(restored).isNotNull();
        assertThat(restored.getAuthentication().getName()).isEqualTo(PRINCIPAL);
        assertThat(restored.getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EXECUTIVE");
        assertThat(restored.getAuthentication().getDetails()).isInstanceOf(WebAuthenticationDetails.class);

        @SuppressWarnings("unchecked")
        Map<String, List<String>> form = loaded.getAttribute("preservedForm");
        assertThat(form.get("customerName")).containsExactly("Özerler");
        assertThat((String) loaded.getAttribute("erp.note")).isEqualTo("second-replica");

        JdbcTemplate jdbc = reader.getBean(JdbcTemplate.class);
        String principal = jdbc.queryForObject(
                "SELECT PRINCIPAL_NAME FROM SPRING_SESSION WHERE SESSION_ID = ?",
                String.class,
                sessionId);
        assertThat(principal).isEqualTo(PRINCIPAL);

        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'spring_session'",
                String.class);
        assertThat(indexes).contains("spring_session_ix1", "spring_session_ix2", "spring_session_ix3");

        Map<String, ? extends Session> byPrincipal = findByPrincipal(
                reader.getBean(FindByIndexNameSessionRepository.class),
                PRINCIPAL);
        assertThat(byPrincipal).containsKey(sessionId);

        Environment environment = writer.getEnvironment();
        assertThat(environment.getProperty("spring.session.jdbc.initialize-schema")).isEqualTo("never");
        assertThat(environment.getProperty("spring.session.jdbc.cleanup-cron")).isEqualTo("0 * * * * *");
        assertThat(environment.getProperty("spring.session.jdbc.save-mode")).isEqualTo("on-set-attribute");
        assertThat(environment.getProperty("spring.session.jdbc.flush-mode")).isEqualTo("on-save");

        DefaultCookieSerializer serializer = writer.getBean(DefaultCookieSerializer.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, "abc"));
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).startsWith("SESSION=");
        assertThat(setCookie).contains("HttpOnly").contains("SameSite=Lax");
        assertThat(setCookie).doesNotContain("Secure");
    }

    @Test
    @DisplayName("cleanUpExpiredSessions removes only rows whose expiry is in the past")
    void expiredSessionsAreDeleted() {
        SessionRepository<?> repository = writer.getBean(SessionRepository.class);
        String liveId = saveLogin(repository);
        String expiredId = saveLogin(repository);

        JdbcTemplate jdbc = writer.getBean(JdbcTemplate.class);
        jdbc.update("UPDATE SPRING_SESSION SET EXPIRY_TIME = ? WHERE SESSION_ID = ?", 0L, expiredId);
        assertThat(countRows(jdbc, expiredId)).isEqualTo(1);

        writer.getBean(JdbcIndexedSessionRepository.class).cleanUpExpiredSessions();

        assertThat(countRows(jdbc, expiredId)).isZero();
        assertThat(countAttributes(jdbc, expiredId)).isZero();
        assertThat(countRows(jdbc, liveId)).isEqualTo(1);
        assertThat(find(reader.getBean(SessionRepository.class), liveId)).isNotNull();
        assertThat(find(reader.getBean(SessionRepository.class), expiredId)).isNull();
    }

    private static String saveLogin(SessionRepository<?> repository) {
        return saveLoginCaptured(repository);
    }

    private static <S extends Session> String saveLoginCaptured(SessionRepository<S> repository) {
        S session = repository.createSession();
        session.setAttribute(SECURITY_CONTEXT, securityContext());
        Map<String, List<String>> form = new LinkedHashMap<>();
        form.put("customerName", new ArrayList<>(List.of("Özerler")));
        session.setAttribute("preservedForm", form);
        session.setAttribute("erp.note", "first-replica");
        repository.save(session);
        session.setAttribute("erp.note", "second-replica");
        repository.save(session);
        return session.getId();
    }

    private static Session find(SessionRepository<?> repository, String sessionId) {
        return findCaptured(repository, sessionId);
    }

    private static <S extends Session> S findCaptured(SessionRepository<S> repository, String sessionId) {
        return repository.findById(sessionId);
    }

    private static Map<String, ? extends Session> findByPrincipal(
            FindByIndexNameSessionRepository<?> repository,
            String principal) {
        return findByPrincipalCaptured(repository, principal);
    }

    private static <S extends Session> Map<String, S> findByPrincipalCaptured(
            FindByIndexNameSessionRepository<S> repository,
            String principal) {
        return repository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                principal);
    }

    private static int countRows(JdbcTemplate jdbc, String sessionId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION WHERE SESSION_ID = ?",
                Integer.class,
                sessionId);
        return count == null ? 0 : count;
    }

    private static int countAttributes(JdbcTemplate jdbc, String sessionId) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM SPRING_SESSION_ATTRIBUTES a
                JOIN SPRING_SESSION s ON s.PRIMARY_ID = a.SESSION_PRIMARY_ID
                WHERE s.SESSION_ID = ?
                """,
                Integer.class,
                sessionId);
        return count == null ? 0 : count;
    }

    private static SecurityContext securityContext() {
        UserDetails user = User.withUsername(PRINCIPAL)
                .password("{noop}secret")
                .roles("ADMIN", "EXECUTIVE")
                .build();
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                user,
                user.getPassword(),
                user.getAuthorities());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.8");
        authentication.setDetails(new WebAuthenticationDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }

    private static ConfigurableApplicationContext startContext() {
        SpringApplication application = new SpringApplication(SessionStoreProbe.class);
        application.setAdditionalProfiles("session-it");
        return application.run(
                "--spring.profiles.active=session-it",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword(),
                "--spring.datasource.driver-class-name=org.postgresql.Driver",
                "--spring.flyway.enabled=true",
                "--spring.jpa.hibernate.ddl-auto=none",
                "--spring.jpa.open-in-view=false",
                "--spring.session.jdbc.initialize-schema=never",
                "--spring.session.timeout=30m",
                "--server.port=0",
                "--spring.main.banner-mode=off",
                "--spring.main.web-application-type=servlet",
                "--logging.level.root=WARN");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class SessionStoreProbe {
    }
}
