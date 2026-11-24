package com.ozerler.marble.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.ozerler.marble.config.TwoFactorProperties;

@ExtendWith(OutputCaptureExtension.class)
class TotpEncryptionServiceTest {

    private static final String SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

    private static String newKey() {
        byte[] raw = new byte[TotpEncryptionService.KEY_BYTES];
        new SecureRandom().nextBytes(raw);
        return Base64.getEncoder().encodeToString(raw);
    }

    private static TotpEncryptionService service(String key, boolean required) {
        TwoFactorProperties properties = new TwoFactorProperties();
        properties.setEncryptionKey(key);
        properties.setKeyRequired(required);
        return new TotpEncryptionService(properties);
    }

    @Test
    @DisplayName("Encrypt then decrypt returns the secret; ciphertext never contains it")
    void roundTrip() {
        TotpEncryptionService service = service(newKey(), true);
        String stored = service.encrypt(SECRET, "totp-secret:1");
        assertThat(stored).startsWith("v1:").doesNotContain(SECRET);
        assertThat(service.decrypt(stored, "totp-secret:1")).isEqualTo(SECRET);
    }

    @Test
    @DisplayName("A random IV makes two encryptions of the same secret differ")
    void randomIv() {
        TotpEncryptionService service = service(newKey(), true);
        assertThat(service.encrypt(SECRET, "c")).isNotEqualTo(service.encrypt(SECRET, "c"));
    }

    @Test
    @DisplayName("A changed byte fails GCM authentication")
    void tamperDetected() {
        TotpEncryptionService service = service(newKey(), true);
        String stored = service.encrypt(SECRET, "totp-secret:1");
        byte[] raw = Base64.getDecoder().decode(stored.substring(3));
        raw[raw.length - 1] ^= 0x01;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> service.decrypt(tampered, "totp-secret:1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(SECRET);
    }

    @Test
    @DisplayName("Ciphertext bound to one user cannot be moved to another user")
    void contextBound() {
        TotpEncryptionService service = service(newKey(), true);
        String stored = service.encrypt(SECRET, "totp-secret:1");
        assertThatThrownBy(() -> service.decrypt(stored, "totp-secret:2"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A different key cannot decrypt")
    void wrongKey() {
        String stored = service(newKey(), true).encrypt(SECRET, "c");
        TotpEncryptionService other = service(newKey(), true);
        assertThatThrownBy(() -> other.decrypt(stored, "c")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Garbage, truncated and unknown-format values are refused")
    void malformedRefused() {
        TotpEncryptionService service = service(newKey(), true);
        assertThatThrownBy(() -> service.decrypt(null, "c")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.decrypt(SECRET, "c")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.decrypt("v1:AAAA", "c")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.decrypt("v1:%%%", "c")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Production fails fast without a key; bad keys are refused")
    void keyValidation() {
        assertThatThrownBy(() -> service("", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TOTP_ENCRYPTION_KEY_FILE");
        assertThatThrownBy(() -> service("not base64!!", true)).isInstanceOf(IllegalStateException.class);
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> service(shortKey, true)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Development without a key uses a temporary key and still round-trips")
    void temporaryKeyOutsideProduction() {
        TotpEncryptionService service = service("", false);
        assertThat(service.decrypt(service.encrypt(SECRET, "c"), "c")).isEqualTo(SECRET);
    }

    @Test
    @DisplayName("Neither the secret nor the key ever reaches the log")
    void nothingSensitiveLogged(CapturedOutput output) {
        String key = newKey();
        TotpEncryptionService service = service(key, true);
        String stored = service.encrypt(SECRET, "c");
        service.decrypt(stored, "c");
        try {
            service.decrypt(stored, "other");
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage()).doesNotContain(SECRET).doesNotContain(key);
        }
        service("", false);
        assertThat(output.getAll()).doesNotContain(SECRET).doesNotContain(key);
    }
}
