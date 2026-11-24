package com.ozerler.marble.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;

import org.assertj.core.api.Assertions;
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

import javax.imageio.ImageIO;

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
        Assertions.assertThat(secret).matches("[A-Z2-7]{32}");
        Assertions.assertThat(service.generateSecret()).isNotEqualTo(secret);
    }

    @Test
    @DisplayName("Code for the current step is accepted and maps to that step")
    void currentCodeAccepted() {
        String code = service.codeAt(secret, NOW);
        OptionalLong step = service.matchStep(secret, code, NOW);
        Assertions.assertThat(step).hasValue(NOW / TotpService.PERIOD_MILLIS);
    }

    @Test
    @DisplayName("One step of clock drift either way is accepted")
    void oneStepDriftAccepted() {
        long period = TotpService.PERIOD_MILLIS;
        Assertions.assertThat(service.matchStep(secret, service.codeAt(secret, NOW - period), NOW)).isPresent();
        Assertions.assertThat(service.matchStep(secret, service.codeAt(secret, NOW + period), NOW)).isPresent();
    }

    @Test
    @DisplayName("Codes two or more steps away are rejected")
    void outsideWindowRejected() {
        long period = TotpService.PERIOD_MILLIS;
        String old = service.codeAt(secret, NOW - 3 * period);
        String future = service.codeAt(secret, NOW + 3 * period);
        String current = service.codeAt(secret, NOW);
        if (!old.equals(current)) {
            Assertions.assertThat(service.matchStep(secret, old, NOW)).isEmpty();
        }
        if (!future.equals(current)) {
            Assertions.assertThat(service.matchStep(secret, future, NOW)).isEmpty();
        }
    }

    @Test
    @DisplayName("Wrong, empty and malformed codes are rejected")
    void wrongCodesRejected() {
        String current = service.codeAt(secret, NOW);
        String wrong = String.format("%06d", (Integer.parseInt(current) + 1) % 1_000_000);
        Assertions.assertThat(service.matchStep(secret, wrong, NOW)).isEmpty();
        Assertions.assertThat(service.matchStep(secret, null, NOW)).isEmpty();
        Assertions.assertThat(service.matchStep(secret, "", NOW)).isEmpty();
        Assertions.assertThat(service.matchStep(secret, "12345", NOW)).isEmpty();
        Assertions.assertThat(service.matchStep(secret, "1234567", NOW)).isEmpty();
        Assertions.assertThat(service.matchStep(secret, "abcdef", NOW)).isEmpty();
    }

    @Test
    @DisplayName("Spaces inside the code are ignored")
    void spacesIgnored() {
        String code = service.codeAt(secret, NOW);
        String spaced = code.substring(0, 3) + " " + code.substring(3);
        Assertions.assertThat(service.matchStep(secret, spaced, NOW)).isPresent();
    }

    @Test
    @DisplayName("otpauth URI carries issuer, label, secret and RFC 6238 parameters")
    void qrUri() {
        String uri = service.getQrCodeUri("ali@ozerler.com", secret);
        Assertions.assertThat(uri)
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
        Assertions.assertThat(png[1]).isEqualTo((byte) 'P');
        var image = ImageIO.read(new ByteArrayInputStream(png));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Assertions.assertThat(new QRCodeReader().decode(bitmap).getText()).isEqualTo(uri);
    }

    @Test
    @DisplayName("Ten unique backup codes, stored only as BCrypt hashes")
    void backupCodes() {
        TotpService.BackupCodes codes = service.generateBackupCodes();
        List<String> plain = codes.plain();
        Assertions.assertThat(plain).hasSize(TotpService.BACKUP_CODE_COUNT);
        Assertions.assertThat(new HashSet<>(plain)).hasSize(TotpService.BACKUP_CODE_COUNT);
        Assertions.assertThat(plain).allMatch(c -> c.matches("[A-Z2-9]{8}"));
        Assertions.assertThat(codes.hashes()).allMatch(h -> h.startsWith("$2"));
        for (int i = 0; i < plain.size(); i++) {
            Assertions.assertThat(codes.hashes().get(i)).isNotEqualTo(plain.get(i));
            Assertions.assertThat(service.matchesBackupCode(plain.get(i), codes.hashes().get(i))).isTrue();
        }
        String first = plain.get(0);
        String dashed = first.substring(0, 4).toLowerCase() + "-" + first.substring(4).toLowerCase();
        Assertions.assertThat(service.matchesBackupCode(dashed, codes.hashes().get(0))).isTrue();
        Assertions.assertThat(service.matchesBackupCode(plain.get(1), codes.hashes().get(0))).isFalse();
        Assertions.assertThat(service.matchesBackupCode("123456", codes.hashes().get(0))).isFalse();
    }

    @Test
    @DisplayName("Backup code and TOTP inputs are told apart")
    void looksLikeBackupCode() {
        Assertions.assertThat(service.looksLikeBackupCode("ABCD-EF23")).isTrue();
        Assertions.assertThat(service.looksLikeBackupCode("123456")).isFalse();
        Assertions.assertThat(service.looksLikeBackupCode(null)).isFalse();
    }
}
