package com.ozerler.marble.security;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.ozerler.marble.config.TwoFactorProperties;

class MathCaptchaServiceTest {

    private MathCaptchaService service;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        service = new MathCaptchaService(new TwoFactorProperties());
        session = new MockHttpSession();
    }

    private int answer() {
        return (Integer) session.getAttribute(MathCaptchaService.ANSWER);
    }

    private long issuedAt() {
        return (Long) session.getAttribute(MathCaptchaService.ISSUED_AT);
    }

    @Test
    @DisplayName("Question appears only after two failed logins")
    void requiredAfterTwoFailures() {
        Assertions.assertThat(service.isRequired(null)).isFalse();
        Assertions.assertThat(service.isRequired(session)).isFalse();
        service.recordFailure(session);
        Assertions.assertThat(service.isRequired(session)).isFalse();
        service.recordFailure(session);
        Assertions.assertThat(service.isRequired(session)).isTrue();
        Assertions.assertThat(service.newQuestion(null)).isNull();
    }

    @Test
    @DisplayName("Generated questions use 1-20 operands and give 0-100 results")
    void questionRange() {
        for (int i = 0; i < 2_000; i++) {
            MathCaptchaService.Question q = service.generate();
            Assertions.assertThat(q.answer()).isBetween(0, 100);
            Assertions.assertThat(q.text()).matches("([1-9]|1\\d|20) [+\\-×] ([1-9]|1\\d|20) = \\?");
        }
    }

    @Test
    @DisplayName("Answer and issue time are stored as Integer and Long in the session")
    void storedInSession() {
        String text = service.newQuestion(session);
        Assertions.assertThat(text).isNotBlank();
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ANSWER)).isInstanceOf(Integer.class);
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ISSUED_AT)).isInstanceOf(Long.class);
    }

    @Test
    @DisplayName("Correct answer passes once; the same answer cannot be reused")
    void singleUse() {
        service.newQuestion(session);
        int expected = answer();
        long now = issuedAt();
        Assertions.assertThat(service.validate(session, " " + expected + " ", now + 1_000)).isTrue();
        Assertions.assertThat(service.validate(session, String.valueOf(expected), now + 2_000)).isFalse();
    }

    @Test
    @DisplayName("Wrong, empty and non-numeric answers fail and consume the question")
    void wrongAnswers() {
        service.newQuestion(session);
        int expected = answer();
        Assertions.assertThat(service.validate(session, String.valueOf(expected + 1), issuedAt())).isFalse();
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ANSWER)).isNull();
        service.newQuestion(session);
        Assertions.assertThat(service.validate(session, "", issuedAt())).isFalse();
        service.newQuestion(session);
        Assertions.assertThat(service.validate(session, "abc", issuedAt())).isFalse();
        service.newQuestion(session);
        Assertions.assertThat(service.validate(session, null, issuedAt())).isFalse();
        Assertions.assertThat(service.validate(session, "5", System.currentTimeMillis())).isFalse();
    }

    @Test
    @DisplayName("Questions expire after three minutes")
    void expires() {
        service.newQuestion(session);
        int expected = answer();
        long issued = issuedAt();
        Assertions.assertThat(service.validate(session, String.valueOf(expected), issued + 3 * 60_000 + 1)).isFalse();
    }

    @Test
    @DisplayName("Reset clears the answer and the failure count")
    void reset() {
        service.recordFailure(session);
        service.recordFailure(session);
        service.newQuestion(session);
        service.reset(session);
        Assertions.assertThat(service.isRequired(session)).isFalse();
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ANSWER)).isNull();
        Assertions.assertThat(session.getAttribute(MathCaptchaService.ISSUED_AT)).isNull();
    }
}
