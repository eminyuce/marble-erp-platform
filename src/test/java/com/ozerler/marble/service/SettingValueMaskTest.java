package com.ozerler.marble.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SettingValueMaskTest {

    @Test
    @DisplayName("password, secret, token and key values are not shown")
    void masksCredentialNames() {
        assertThat(SettingValueMask.display("mail.smtp.password", "smtppassword123")).isEqualTo("******");
        assertThat(SettingValueMask.display("api.secret", "abc")).isEqualTo("******");
        assertThat(SettingValueMask.display("session.token", "abc")).isEqualTo("******");
        assertThat(SettingValueMask.display("app.security.key", "abc")).isEqualTo("******");
        assertThat(SettingValueMask.display("mail.smtp.host", "smtp.example.com")).isEqualTo("smtp.example.com");
    }
}
