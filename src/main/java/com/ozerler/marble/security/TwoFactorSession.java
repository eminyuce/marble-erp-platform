package com.ozerler.marble.security;

import java.time.Duration;

import jakarta.servlet.http.HttpSession;

/**
 * Session attributes for the half-finished login. Values are JDK types only (Long, Boolean, String)
 * because Spring Session JDBC serializes them.
 */
public final class TwoFactorSession {

    /** User id whose password was verified but who still owes the second factor. */
    public static final String PENDING_USER_ID = "PENDING_2FA_USER_ID";
    /** TRUE when the pending user must enrol first instead of entering a code. */
    public static final String PENDING_SETUP = "PENDING_2FA_SETUP";
    /** Epoch millis when the pending state started. */
    public static final String PENDING_STARTED_AT = "PENDING_2FA_STARTED_AT";
    /** TRUE when the user ticked "Beni hatırla"; the cookie is issued only after the second factor. */
    public static final String PENDING_REMEMBER = "PENDING_2FA_REMEMBER";
    /** AES-GCM sealed backup codes waiting for the "Yedek kodları kaydettim" confirmation. */
    public static final String BACKUP_CODES_SEALED = "TWO_FACTOR_BACKUP_CODES_SEALED";
    /** Owner of {@link #BACKUP_CODES_SEALED}. */
    public static final String BACKUP_CODES_USER_ID = "TWO_FACTOR_BACKUP_CODES_USER_ID";

    private TwoFactorSession() {
    }

    /**
     * Starts the pending state.
     *
     * @param session HTTP session
     * @param userId password-verified user
     * @param setup whether enrolment is required
     * @param nowMillis current time
     */
    public static void startPending(HttpSession session, Long userId, boolean setup, long nowMillis) {
        session.setAttribute(PENDING_USER_ID, userId);
        session.setAttribute(PENDING_SETUP, Boolean.valueOf(setup));
        session.setAttribute(PENDING_STARTED_AT, Long.valueOf(nowMillis));
    }

    /**
     * The pending user id when the state exists, has the expected kind and has not expired.
     *
     * @param session HTTP session or null
     * @param setup expected kind
     * @param timeout lifetime of the pending state
     * @param nowMillis current time
     * @return user id or null
     */
    public static Long pendingUserId(HttpSession session, boolean setup, Duration timeout, long nowMillis) {
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute(PENDING_USER_ID);
        Object kind = session.getAttribute(PENDING_SETUP);
        Object started = session.getAttribute(PENDING_STARTED_AT);
        if (!(userId instanceof Long id) || !(kind instanceof Boolean isSetup) || !(started instanceof Long at)) {
            return null;
        }
        if (isSetup != setup) {
            return null;
        }
        if (nowMillis - at > timeout.toMillis()) {
            clearPending(session);
            return null;
        }
        return id;
    }

    /**
     * Removes the pending state.
     *
     * @param session HTTP session or null
     */
    public static void clearPending(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(PENDING_USER_ID);
        session.removeAttribute(PENDING_SETUP);
        session.removeAttribute(PENDING_STARTED_AT);
        session.removeAttribute(PENDING_REMEMBER);
    }

    /**
     * Removes sealed backup codes.
     *
     * @param session HTTP session or null
     */
    public static void clearBackupCodes(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(BACKUP_CODES_SEALED);
        session.removeAttribute(BACKUP_CODES_USER_ID);
    }
}
