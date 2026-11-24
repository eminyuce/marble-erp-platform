package com.ozerler.marble.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.TwoFactorSetupView;
import com.ozerler.marble.dto.TwoFactorStatusView;
import com.ozerler.marble.model.TotpBackupCode;
import com.ozerler.marble.model.User;
import com.ozerler.marble.model.enums.TwoFactorMode;
import com.ozerler.marble.repository.TotpBackupCodeRepository;
import com.ozerler.marble.repository.UserRepository;
import com.ozerler.marble.security.TotpEncryptionService;
import com.ozerler.marble.security.TotpService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Per-user TOTP enrolment, login verification, backup codes and admin reset.
 * Secrets are decrypted only in memory and never logged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorService {

    private static final String SECRET_CONTEXT = "totp-secret:";
    private static final String BACKUP_CONTEXT = "backup-codes:";
    private static final int KEY_GROUP = 4;
    private static final int STAMP_LENGTH = 16;

    private final UserRepository userRepository;
    private final TotpBackupCodeRepository backupCodeRepository;
    private final TotpService totpService;
    private final TotpEncryptionService encryptionService;
    private final TwoFactorSettingsService settingsService;
    private final LoginAttemptService loginAttemptService;
    private final PasswordEncoder passwordEncoder;
    private Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * What a password-verified login still needs.
     *
     * @param email authenticated user name (e-mail)
     * @return the decision with the user id
     */
    @Transactional(readOnly = true)
    public LoginDecision decideAfterPassword(String email) {
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || settingsService.mode() == TwoFactorMode.DISABLED) {
            return new LoginDecision(found.map(User::getId).orElse(null), Challenge.NONE);
        }
        User user = found.get();
        if (user.isTotpEnabled() && user.isTotpSetupCompleted()) {
            return new LoginDecision(user.getId(), Challenge.VERIFY);
        }
        if (settingsService.enforced()) {
            return new LoginDecision(user.getId(), Challenge.SETUP);
        }
        return new LoginDecision(user.getId(), Challenge.NONE);
    }

    /**
     * Whether an already authenticated user must enrol before using the app (enforced, not set up).
     *
     * @param email user name
     * @return true when setup is required
     */
    @Transactional(readOnly = true)
    public boolean setupRequired(String email) {
        if (!settingsService.enforced()) {
            return false;
        }
        return userRepository.findByEmail(email).map(u -> !u.isTotpSetupCompleted()).orElse(false);
    }

    /**
     * A value mixed into the remember-me signature. It changes whenever the user's TOTP secret changes,
     * so a cookie issued before 2FA was enabled or reset stops working. Empty for users without 2FA,
     * so their existing cookies stay valid.
     *
     * @param email user name
     * @return the stamp
     */
    @Transactional(readOnly = true)
    public String rememberMeStamp(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u.isTotpEnabled() && u.getTotpSecretEncrypted() != null)
                .map(u -> "|2fa:" + sha256(u.getTotpSecretEncrypted()).substring(0, STAMP_LENGTH))
                .orElse("");
    }

    /**
     * Status for the account page.
     *
     * @param email user name
     * @return status view
     */
    @Transactional(readOnly = true)
    public TwoFactorStatusView status(String email) {
        User user = requireByEmail(email);
        long remaining = 0;
        if (user.isTotpEnabled()) {
            remaining = backupCodeRepository.countByUserIdAndUsedAtIsNull(user.getId());
        }
        return new TwoFactorStatusView(settingsService.mode(), settingsService.enforced(),
                user.isTotpEnabled(), remaining);
    }

    /**
     * User id for an e-mail.
     *
     * @param email user name
     * @return id
     */
    @Transactional(readOnly = true)
    public Long userIdForEmail(String email) {
        return requireByEmail(email).getId();
    }

    /**
     * E-mail for a user id (used to build the full session after the second factor).
     *
     * @param userId id
     * @return e-mail
     */
    @Transactional(readOnly = true)
    public String emailForUserId(Long userId) {
        return requireById(userId).getEmail();
    }

    /**
     * Starts or resumes enrolment. A pending secret is reused so reloading the page keeps the same QR.
     *
     * @param userId user id
     * @return QR and manual key
     */
    @Transactional
    public TwoFactorSetupView beginSetup(Long userId) {
        if (settingsService.mode() != TwoFactorMode.AUTHENTICATOR) {
            throw new TwoFactorException("twofactor.disabled");
        }
        User user = requireById(userId);
        if (user.isTotpEnabled()) {
            throw new TwoFactorException("twofactor.setup.already");
        }
        String secret;
        if (user.getTotpSecretEncrypted() == null) {
            secret = totpService.generateSecret();
            user.setTotpSecretEncrypted(encryptionService.encrypt(secret, SECRET_CONTEXT + userId));
            user.setTotpSetupCompleted(false);
            user.setTotpLastUsedStep(null);
            userRepository.save(user);
        } else {
            secret = decryptSecret(user);
        }
        String uri = totpService.getQrCodeUri(user.getEmail(), secret);
        return new TwoFactorSetupView(user.getEmail(), groupKey(secret), totpService.generateQrCodeImage(uri));
    }

    /**
     * Confirms enrolment with the first code and creates the backup codes.
     *
     * @param userId user id
     * @param code 6-digit code from the app
     * @return the plain backup codes to show once
     * @throws TwoFactorException when the code is wrong
     */
    @Transactional
    public List<String> confirmSetup(Long userId, String code) {
        User user = requireById(userId);
        if (user.isTotpEnabled() || user.getTotpSecretEncrypted() == null) {
            throw new TwoFactorException("twofactor.setup.restart");
        }
        OptionalLong step = totpService.matchStep(decryptSecret(user), code, clock.millis());
        if (step.isEmpty()) {
            throw new TwoFactorException("twofactor.code.invalid");
        }
        user.setTotpEnabled(true);
        user.setTotpSetupCompleted(true);
        user.setTotpLastUsedStep(step.getAsLong());
        userRepository.save(user);
        List<String> codes = replaceBackupCodes(userId);
        log.info("SECURITY_AUDIT 2FA enabled: userId={} time={}", userId, LocalDateTime.now(clock));
        return codes;
    }

    /**
     * Checks the second factor at login. Accepts a TOTP code (no replay of a used step) or an unused backup code.
     *
     * @param userId pending user
     * @param code typed code
     * @param clientIp resolved client address for the failure log
     * @return the outcome
     */
    @Transactional
    public CodeCheck verifyLoginCode(Long userId, String code, String clientIp) {
        User user = requireById(userId);
        if (user.isLockedAt(LocalDateTime.now(clock))) {
            return new CodeCheck(CodeOutcome.LOCKED, 0);
        }
        if (user.isTotpEnabled() && acceptCode(user, code)) {
            loginAttemptService.resetAccount(userId);
            return new CodeCheck(CodeOutcome.SUCCESS, 0);
        }
        LoginAttemptService.Outcome outcome = loginAttemptService.recordSecondFactorFailure(userId, clientIp);
        if (outcome == LoginAttemptService.Outcome.LOCKED) {
            return new CodeCheck(CodeOutcome.LOCKED, 0);
        }
        return new CodeCheck(CodeOutcome.INVALID, loginAttemptService.remainingAttempts(userId));
    }

    /**
     * Turns 2FA off for the signed-in user. Needs the current password and a fresh TOTP code.
     * Refused while the admin enforces 2FA.
     *
     * @param email user name
     * @param currentPassword password
     * @param code TOTP code
     */
    @Transactional
    public void disable(String email, String currentPassword, String code) {
        if (settingsService.enforced()) {
            throw new TwoFactorException("twofactor.disable.enforced");
        }
        User user = requireByEmail(email);
        if (!user.isTotpEnabled()) {
            throw new TwoFactorException("twofactor.not.enabled");
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new TwoFactorException("twofactor.password.invalid");
        }
        if (!acceptTotp(user, code)) {
            throw new TwoFactorException("twofactor.code.invalid");
        }
        clear(user);
        log.info("SECURITY_AUDIT 2FA disabled by the user: userId={} time={}", user.getId(), LocalDateTime.now(clock));
    }

    /**
     * Replaces all backup codes. Needs a fresh TOTP code.
     *
     * @param email user name
     * @param code TOTP code
     * @return new plain codes to show once
     */
    @Transactional
    public List<String> regenerateBackupCodes(String email, String code) {
        User user = requireByEmail(email);
        if (!user.isTotpEnabled()) {
            throw new TwoFactorException("twofactor.not.enabled");
        }
        if (!acceptTotp(user, code)) {
            throw new TwoFactorException("twofactor.code.invalid");
        }
        List<String> codes = replaceBackupCodes(user.getId());
        log.info("SECURITY_AUDIT 2FA backup codes regenerated: userId={} time={}",
                user.getId(), LocalDateTime.now(clock));
        return codes;
    }

    /**
     * Admin reset: removes the secret and backup codes and clears any lock. The user enrols again if required.
     *
     * @param userId target user
     * @param actor admin e-mail
     */
    @Transactional
    public void adminReset(Long userId, String actor) {
        User user = requireById(userId);
        clear(user);
        userRepository.resetLoginFailures(userId);
        log.warn("SECURITY_AUDIT 2FA reset by admin: actor={} targetUserId={} targetEmail={} time={}",
                actor, userId, user.getEmail(), LocalDateTime.now(clock));
    }

    /**
     * Encrypts freshly generated backup codes for a short stay in the session until the user confirms.
     *
     * @param userId owner
     * @param codes plain codes
     * @return ciphertext
     */
    public String sealBackupCodes(Long userId, List<String> codes) {
        return encryptionService.encrypt(String.join(",", codes), BACKUP_CONTEXT + userId);
    }

    /**
     * Opposite of {@link #sealBackupCodes(Long, List)}.
     *
     * @param userId owner
     * @param sealed ciphertext
     * @return plain codes
     */
    public List<String> openBackupCodes(Long userId, String sealed) {
        return List.of(encryptionService.decrypt(sealed, BACKUP_CONTEXT + userId).split(","));
    }

    private boolean acceptCode(User user, String code) {
        if (totpService.looksLikeBackupCode(code)) {
            return consumeBackupCode(user, code);
        }
        return acceptTotp(user, code);
    }

    private boolean acceptTotp(User user, String code) {
        OptionalLong step = totpService.matchStep(decryptSecret(user), code, clock.millis());
        if (step.isEmpty()) {
            return false;
        }
        Long last = user.getTotpLastUsedStep();
        if (last != null && step.getAsLong() <= last) {
            log.warn("Rejected a reused TOTP code: userId={}", user.getId());
            return false;
        }
        user.setTotpLastUsedStep(step.getAsLong());
        userRepository.save(user);
        return true;
    }

    private boolean consumeBackupCode(User user, String code) {
        for (TotpBackupCode stored : backupCodeRepository.findByUserIdAndUsedAtIsNull(user.getId())) {
            if (totpService.matchesBackupCode(code, stored.getCodeHash())) {
                stored.setUsedAt(LocalDateTime.now(clock));
                backupCodeRepository.save(stored);
                log.info("SECURITY_AUDIT 2FA backup code used: userId={} remaining={}", user.getId(),
                        backupCodeRepository.countByUserIdAndUsedAtIsNull(user.getId()));
                return true;
            }
        }
        return false;
    }

    private List<String> replaceBackupCodes(Long userId) {
        backupCodeRepository.deleteAllForUser(userId);
        TotpService.BackupCodes codes = totpService.generateBackupCodes();
        LocalDateTime now = LocalDateTime.now(clock);
        for (String hash : codes.hashes()) {
            backupCodeRepository.save(TotpBackupCode.builder()
                    .userId(userId)
                    .codeHash(hash)
                    .createdAt(now)
                    .build());
        }
        return codes.plain();
    }

    private void clear(User user) {
        user.setTotpSecretEncrypted(null);
        user.setTotpEnabled(false);
        user.setTotpSetupCompleted(false);
        user.setTotpLastUsedStep(null);
        userRepository.save(user);
        backupCodeRepository.deleteAllForUser(user.getId());
    }

    private String decryptSecret(User user) {
        return encryptionService.decrypt(user.getTotpSecretEncrypted(), SECRET_CONTEXT + user.getId());
    }

    private User requireById(Long userId) {
        if (userId == null) {
            throw new TwoFactorException("twofactor.user.missing");
        }
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new TwoFactorException("twofactor.user.missing"));
    }

    private User requireByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new TwoFactorException("twofactor.user.missing"));
    }

    private static String groupKey(String secret) {
        StringBuilder grouped = new StringBuilder(secret.length() + secret.length() / KEY_GROUP);
        for (int i = 0; i < secret.length(); i++) {
            if (i > 0 && i % KEY_GROUP == 0) {
                grouped.append(' ');
            }
            grouped.append(secret.charAt(i));
        }
        return grouped.toString();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    /** What a login still needs after the password. */
    public enum Challenge {
        NONE, VERIFY, SETUP
    }

    /** Second-factor check result. */
    public enum CodeOutcome {
        SUCCESS, INVALID, LOCKED
    }

    /**
     * Decision after the password step.
     *
     * @param userId user id, null for an unknown user
     * @param challenge what is still needed
     */
    public record LoginDecision(Long userId, Challenge challenge) {
    }

    /**
     * Second-factor result.
     *
     * @param outcome result
     * @param remainingAttempts attempts left before the lock
     */
    public record CodeCheck(CodeOutcome outcome, int remainingAttempts) {
    }
}
