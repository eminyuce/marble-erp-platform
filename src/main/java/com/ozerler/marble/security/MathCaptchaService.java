package com.ozerler.marble.security;

import java.security.SecureRandom;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import com.ozerler.marble.config.TwoFactorProperties;

/**
 * Server-side arithmetic question for the login form, shown after repeated failures.
 * The answer lives only in the HTTP session as an {@link Integer}, with its issue time as a {@link Long}.
 * It is single-use and expires. Nothing here calls an external service.
 */
@Service
public class MathCaptchaService {

    /** Expected answer, {@link Integer}. */
    public static final String ANSWER = "MATH_CAPTCHA_ANSWER";
    /** Issue time in epoch millis, {@link Long}. */
    public static final String ISSUED_AT = "MATH_CAPTCHA_ISSUED_AT";
    /** Failed logins in this browser session, {@link Integer}. */
    public static final String FAILED_COUNT = "LOGIN_FAILED_COUNT";

    static final int MIN_OPERAND = 1;
    static final int MAX_OPERAND = 20;
    static final int MAX_RESULT = 100;
    private static final int OPERATOR_COUNT = 3;
    private static final int MAX_ANSWER_DIGITS = 3;

    private final SecureRandom random = new SecureRandom();
    private final TwoFactorProperties properties;

    public MathCaptchaService(TwoFactorProperties properties) {
        this.properties = properties;
    }

    /**
     * Whether the login form must show the question.
     *
     * @param session session or null
     * @return true after the configured number of failures
     */
    public boolean isRequired(HttpSession session) {
        return failedCount(session) >= properties.getCaptchaAfterFailures();
    }

    /**
     * Failed logins counted in this session.
     *
     * @param session session or null
     * @return count
     */
    public int failedCount(HttpSession session) {
        if (session == null) {
            return 0;
        }
        Object value = session.getAttribute(FAILED_COUNT);
        if (value instanceof Integer count) {
            return count;
        }
        return 0;
    }

    /**
     * Counts one failed login (wrong password, locked account, or wrong math answer).
     *
     * @param session session
     */
    public void recordFailure(HttpSession session) {
        session.setAttribute(FAILED_COUNT, Integer.valueOf(failedCount(session) + 1));
    }

    /**
     * Creates a new question, stores its answer in the session and returns the text to display.
     *
     * @param session session or null
     * @return text such as "12 + 7 = ?", or null without a session
     */
    public String newQuestion(HttpSession session) {
        if (session == null) {
            return null;
        }
        Question question = generate();
        session.setAttribute(ANSWER, Integer.valueOf(question.answer()));
        session.setAttribute(ISSUED_AT, Long.valueOf(System.currentTimeMillis()));
        return question.text();
    }

    /**
     * Checks the typed answer. The stored answer is removed whatever the result (single use).
     *
     * @param session session or null
     * @param typed user input
     * @param nowMillis current time
     * @return true only for a fresh, matching answer
     */
    public boolean validate(HttpSession session, String typed, long nowMillis) {
        if (session == null) {
            return false;
        }
        Object answer = session.getAttribute(ANSWER);
        Object issuedAt = session.getAttribute(ISSUED_AT);
        session.removeAttribute(ANSWER);
        session.removeAttribute(ISSUED_AT);
        if (!(answer instanceof Integer expected) || !(issuedAt instanceof Long issued)) {
            return false;
        }
        if (nowMillis - issued > properties.getCaptchaTtl().toMillis()) {
            return false;
        }
        if (typed == null) {
            return false;
        }
        String trimmed = typed.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_ANSWER_DIGITS
                || !trimmed.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return Integer.parseInt(trimmed) == expected;
    }

    /**
     * Clears the answer and the failure count after a full login.
     *
     * @param session session or null
     */
    public void reset(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(ANSWER);
        session.removeAttribute(ISSUED_AT);
        session.removeAttribute(FAILED_COUNT);
    }

    /**
     * Builds one question: operands 1 to 20, +, - or ×, result 0 to 100.
     *
     * @return question text and answer
     */
    Question generate() {
        while (true) {
            int left = operand();
            int right = operand();
            switch (random.nextInt(OPERATOR_COUNT)) {
                case 0:
                    return new Question(left + " + " + right + " = ?", left + right);
                case 1:
                    int high = Math.max(left, right);
                    int low = Math.min(left, right);
                    return new Question(high + " - " + low + " = ?", high - low);
                default:
                    if (left * right <= MAX_RESULT) {
                        return new Question(left + " × " + right + " = ?", left * right);
                    }
                    break;
            }
        }
    }

    private int operand() {
        return MIN_OPERAND + random.nextInt(MAX_OPERAND - MIN_OPERAND + 1);
    }

    /**
     * One arithmetic question.
     *
     * @param text display text
     * @param answer expected answer
     */
    record Question(String text, int answer) {
    }
}
