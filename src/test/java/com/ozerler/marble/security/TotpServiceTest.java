package com.ozerler.marble.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.ozerler.marble.config.TwoFactorProperties;

class TotpServiceTest {

    private static final long NOW = 1_764_000_000_000L;

    private TotpService service;
    private String secret;

    @BeforeEach
    void setUp() {
        service = new TotpService(new BCryptPasswordEncoder(4), new TwoFactorProperties());
        secret = service.generateSecret();
    }

    @Test
    @DisplayName("Secret is a 160-bit Base32 value and differs each time")
    void secretIsBase32() {
        assertThat(secret).matches("[A-Z2-7]{32}");
        assertThat(service.generateSecret()).isNotEqualTo(secret);
    }

    @Test
    @DisplayName("Code for the current step is accepted and maps to that step")
    void currentCodeAccepted() {
        String code = service.codeAt(secret, NOW);
        OptionalLong step = service.matchStep(secret, code, NOW);
        assertThat(step).hasValue(NOW / TotpService.PERIOD_MILLIS);
    }

    @Test
    @DisplayName("One step of clock drift either way is accepted")
    void oneStepDriftAccepted() {
        long period = TotpService.PERIOD_MILLIS;
        assertThat(service.matchStep(secret, service.codeAt(secret, NOW - period), NOW)).isPresent();
        assertThat(service.matchStep(secret, service.codeAt(secret, NOW + period), NOW)).isPresent();
    }

    @Test
    @DisplayName("Codes two or more steps away are rejected")
    void outsideWindowRejected() {
        long period = TotpService.PERIOD_MILLIS;
        String old = service.codeAt(secret, NOW - 3 * period);
        String future = service.codeAt(secret, NOW + 3 * period);
        String current = service.codeAt(secret, NOW);
        if (!old.equals(current)) {
            assertThat(service.matchStep(secret, old, NOW)).isEmpty();
        }
        if (!future.equals(current)) {
            assertThat(service.matchStep(secret, future, NOW)).isEmpty();
        }
    }

    @Test
    @DisplayName("Wrong, empty and malformed codes are rejected")
    void wrongCodesRejected() {
        String current = service.codeAt(secret, NOW);
        String wrong = String.format("%06d", (Integer.parseInt(current) + 1) % 1_000_000);
        assertThat(service.matchStep(secret, wrong, NOW)).isEmpty();
        assertThat(service.matchStep(secret, null, NOW)).isEmpty();
        assertThat(service.matchStep(secret, "", NOW)).isEmpty();
        assertThat(service.matchStep(secret, "12345", NOW)).isEmpty();
        assertThat(service.matchStep(secret, "1234567", NOW)).isEmpty();
        assertThat(service.matchStep(secret, "abcdef", NOW)).isEmpty();
    }

    @Test
    @DisplayName("Spaces inside the code are ignored")
    void spacesIgnored() {
        String code = service.codeAt(secret, NOW);
        assertThat(service.matchStep(secret, code.substring(0, 3) + " " + code.substring(3), NOW)).isPresent();
    }

    @Test
    @DisplayName("otpauth URI carries issuer, label, secret and RFC 6238 parameters")
    void qrUri() {
        String uri = service.getQrCodeUri("ali@ozerler.com", secret);
        assertThat(uri)
                .startsWith("otpauth://totp/Ozerler%20Mermer%20ERP:ali%40ozerler.com?")
                .contains("secret=" + secret)
                .contains("issuer=Ozerler%20Mermer%20ERP")
                .contains("algorithm=SHA1")
                .contains("digits=6")
                .contains("period=30");
    }

    @Test
    @DisplayName("QR image is a locally rendered PNG that decodes back to the URI")
    void qrImageDecodes() throws IOException, NotFoundException, ChecksumException, FormatException {
        String uri = service.getQrCodeUri("ali@ozerler.com", secret);
        byte[] png = Base64.getDecoder().decode(service.generateQrCodeImage(uri));
        assertThat(png[1]).isEqualTo((byte) 'P');
        var image = ImageIO.read(new ByteArrayInputStream(png));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        assertThat(new QRCodeReader().decode(bitmap).getText()).isEqualTo(uri);
    }

    @Test
    @DisplayName("Ten unique backup codes, stored only as BCrypt hashes")
    void backupCodes() {
        TotpService.BackupCodes codes = service.generateBackupCodes();
        List<String> plain = codes.plain();
        assertThat(plain).hasSize(TotpService.BACKUP_CODE_COUNT);
        assertThat(new HashSet<>(plain)).hasSize(TotpService.BACKUP_CODE_COUNT);
        assertThat(plain).allMatch(c -> c.matches("[A-Z2-9]{8}"));
        assertThat(codes.hashes()).allMatch(h -> h.startsWith("$2"));
        for (int i = 0; i < plain.size(); i++) {
            assertThat(codes.hashes().get(i)).isNotEqualTo(plain.get(i));
            assertThat(service.matchesBackupCode(plain.get(i), codes.hashes().get(i))).isTrue();
        }
        String first = plain.get(0);
        String dashed = first.substring(0, 4).toLowerCase() + "-" + first.substring(4).toLowerCase();
        assertThat(service.matchesBackupCode(dashed, codes.hashes().get(0))).isTrue();
        assertThat(service.matchesBackupCode(plain.get(1), codes.hashes().get(0))).isFalse();
        assertThat(service.matchesBackupCode("123456", codes.hashes().get(0))).isFalse();
    }

    @Test
    @DisplayName("Backup code and TOTP inputs are told apart")
    void looksLikeBackupCode() {
        assertThat(service.looksLikeBackupCode("ABCD-EF23")).isTrue();
        assertThat(service.looksLikeBackupCode("123456")).isFalse();
        assertThat(service.looksLikeBackupCode(null)).isFalse();
    }
}
