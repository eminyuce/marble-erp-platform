package com.ozerler.marble.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ozerler.marble.config.TwoFactorProperties;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.HmacHashFunction;
import com.warrenstrange.googleauth.KeyRepresentation;

/**
 * Offline RFC 6238 TOTP: 6 digits, 30 second period, HMAC-SHA1, one step of clock drift either way.
 * Nothing here calls a network service. Secrets and codes are never logged.
 */
@Service
public class TotpService {

    public static final int CODE_DIGITS = 6;
    public static final long PERIOD_MILLIS = 30_000L;
    public static final int BACKUP_CODE_COUNT = 10;
    public static final int BACKUP_CODE_LENGTH = 8;

    private static final int SECRET_BITS = 160;
    /** Window size 3 = the current step plus one before and one after. */
    private static final int WINDOW_SIZE = 3;
    private static final int QR_SIZE = 320;
    private static final String BACKUP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern TOTP_PATTERN = Pattern.compile("\\d{6}");
    private static final Pattern BACKUP_PATTERN = Pattern.compile("[A-Z0-9]{8}");

    private final GoogleAuthenticator authenticator;
    private final PasswordEncoder passwordEncoder;
    private final String issuer;
    private final SecureRandom random = new SecureRandom();

    public TotpService(PasswordEncoder passwordEncoder, TwoFactorProperties properties) {
        this.passwordEncoder = passwordEncoder;
        this.issuer = properties.getIssuer();
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setCodeDigits(CODE_DIGITS)
                .setTimeStepSizeInMillis(PERIOD_MILLIS)
                .setWindowSize(WINDOW_SIZE)
                .setHmacHashFunction(HmacHashFunction.HmacSHA1)
                .setKeyRepresentation(KeyRepresentation.BASE32)
                .setSecretBits(SECRET_BITS)
                .setNumberOfScratchCodes(0)
                .build();
        this.authenticator = new GoogleAuthenticator(config);
    }

    /**
     * Creates a new random 160-bit secret.
     *
     * @return Base32 secret
     */
    public String generateSecret() {
        return authenticator.createCredentials().getKey();
    }

    /**
     * Builds the {@code otpauth://} URI an authenticator app reads from the QR code.
     *
     * @param username account label shown in the app
     * @param secret Base32 secret
     * @return the URI
     */
    public String getQrCodeUri(String username, String secret) {
        String encodedIssuer = encode(issuer);
        return "otpauth://totp/" + encodedIssuer + ":" + encode(username)
                + "?secret=" + secret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1&digits=" + CODE_DIGITS
                + "&period=" + PERIOD_MILLIS / 1000;
    }

    /**
     * Renders a QR code locally.
     *
     * @param qrUri the otpauth URI
     * @return Base64 PNG
     */
    public String generateQrCodeImage(String qrUri) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(qrUri, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 1,
                            EncodeHintType.CHARACTER_SET, "UTF-8"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException ex) {
            // The URI holds the secret, so it is not part of the message.
            throw new IllegalStateException("QR code could not be rendered", ex);
        }
    }

    /**
     * Checks a code against the current time.
     *
     * @param secret Base32 secret
     * @param code 6-digit code
     * @return true when the code matches the current step or one step either side
     */
    public boolean verifyCode(String secret, String code) {
        return matchStep(secret, code, System.currentTimeMillis()).isPresent();
    }

    /**
     * Finds the time step a code belongs to, so callers can reject a replay of the same step.
     *
     * @param secret Base32 secret
     * @param code 6-digit code
     * @param nowMillis current time
     * @return the matching step, or empty when the code is wrong
     */
    public OptionalLong matchStep(String secret, String code, long nowMillis) {
        String normalized = normalizeTotp(code);
        if (!TOTP_PATTERN.matcher(normalized).matches()) {
            return OptionalLong.empty();
        }
        int value = Integer.parseInt(normalized);
        if (!authenticator.authorize(secret, value, nowMillis)) {
            return OptionalLong.empty();
        }
        long current = nowMillis / PERIOD_MILLIS;
        for (long step = current - 1; step <= current + 1; step++) {
            if (authenticator.getTotpPassword(secret, step * PERIOD_MILLIS) == value) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    /**
     * Code for a given time. Used by tests and by nothing on the request path.
     *
     * @param secret Base32 secret
     * @param timeMillis instant
     * @return the 6-digit code with leading zeros
     */
    public String codeAt(String secret, long timeMillis) {
        return String.format(Locale.ROOT, "%06d", authenticator.getTotpPassword(secret, timeMillis));
    }

    /**
     * Creates 10 single-use 8-character backup codes.
     *
     * @return plain codes (show once) and their BCrypt hashes (store)
     */
    public BackupCodes generateBackupCodes() {
        List<String> plain = new ArrayList<>(BACKUP_CODE_COUNT);
        List<String> hashes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder(BACKUP_CODE_LENGTH);
            for (int c = 0; c < BACKUP_CODE_LENGTH; c++) {
                code.append(BACKUP_ALPHABET.charAt(random.nextInt(BACKUP_ALPHABET.length())));
            }
            plain.add(code.toString());
            hashes.add(passwordEncoder.encode(code.toString()));
        }
        return new BackupCodes(List.copyOf(plain), List.copyOf(hashes));
    }

    /**
     * Compares a typed backup code with a stored hash.
     *
     * @param typed code as typed (dashes and spaces ignored)
     * @param hash BCrypt hash
     * @return true on match
     */
    public boolean matchesBackupCode(String typed, String hash) {
        String normalized = normalizeBackup(typed);
        return BACKUP_PATTERN.matcher(normalized).matches() && passwordEncoder.matches(normalized, hash);
    }

    /**
     * Whether the input looks like a backup code rather than a TOTP code.
     *
     * @param typed user input
     * @return true for 8 letters/digits
     */
    public boolean looksLikeBackupCode(String typed) {
        return BACKUP_PATTERN.matcher(normalizeBackup(typed)).matches();
    }

    private static String normalizeTotp(String code) {
        if (code == null) {
            return "";
        }
        return code.replaceAll("\\s", "");
    }

    private static String normalizeBackup(String code) {
        if (code == null) {
            return "";
        }
        return code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Freshly generated backup codes.
     *
     * @param plain codes to show once
     * @param hashes BCrypt hashes to store
     */
    public record BackupCodes(List<String> plain, List<String> hashes) {
    }
}
