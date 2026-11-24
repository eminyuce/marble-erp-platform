package com.ozerler.marble.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.ozerler.marble.config.TwoFactorProperties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM encryption for TOTP secrets. Every call uses a fresh random 96-bit IV.
 * The stored form is {@code v1:} followed by Base64(IV || ciphertext || tag).
 * The key and plain text are never logged.
 */
@Service
@Slf4j
public class TotpEncryptionService {

    static final int KEY_BYTES = 32;
    private static final String PREFIX = "v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKey key;

    public TotpEncryptionService(TwoFactorProperties properties) {
        this.key = resolveKey(properties.getEncryptionKey(), properties.isKeyRequired(), random);
    }

    static SecretKey resolveKey(String encoded, boolean required, SecureRandom random) {
        if (encoded == null || encoded.isBlank()) {
            if (required) {
                throw new IllegalStateException(
                        "TOTP_ENCRYPTION_KEY_FILE is required: a base64 encoded 32-byte AES key");
            }
            log.warn("TOTP encryption key is not set. A temporary key is used; enrolled authenticators "
                    + "stop working after a restart. Set TOTP_ENCRYPTION_KEY_FILE outside local development.");
            byte[] temporary = new byte[KEY_BYTES];
            random.nextBytes(temporary);
            return new SecretKeySpec(temporary, "AES");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("TOTP encryption key is not valid base64");
        }
        if (raw.length != KEY_BYTES) {
            throw new IllegalStateException("TOTP encryption key must decode to 32 bytes (AES-256)");
        }
        return new SecretKeySpec(raw, "AES");
    }

    /**
     * Encrypts a value and binds it to a context string (for example the user id).
     *
     * @param plainText value to protect
     * @param context associated data that must match on decrypt
     * @return versioned Base64 ciphertext
     */
    public String encrypt(String plainText, String context) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] sealed = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + sealed.length);
            buffer.put(iv).put(sealed);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOTP secret could not be encrypted", ex);
        }
    }

    /**
     * Decrypts a value produced by {@link #encrypt(String, String)}.
     *
     * @param stored versioned ciphertext
     * @param context the same associated data used to encrypt
     * @return the plain text
     * @throws IllegalStateException when the value was tampered with or the key changed
     */
    public String decrypt(String stored, String context) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw new IllegalStateException("TOTP secret has an unknown format");
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (all.length <= IV_BYTES) {
                throw new IllegalStateException("TOTP secret is truncated");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("TOTP secret could not be decrypted (tampered value or changed key)");
        }
    }
}
