package com.ozerler.marble.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SettingValueMaskTest {

    @Test
    @DisplayName("password, secret, token and key values are not shown")
    void masksCredentialNames() {
        Assertions.assertThat(SettingValueMask.display("mail.smtp.password", "smtppassword123")).isEqualTo("******");
        Assertions.assertThat(SettingValueMask.display("api.secret", "abc")).isEqualTo("******");
        Assertions.assertThat(SettingValueMask.display("session.token", "abc")).isEqualTo("******");
        Assertions.assertThat(SettingValueMask.display("app.security.key", "abc")).isEqualTo("******");
        Assertions.assertThat(SettingValueMask.display("mail.smtp.host", "smtp.example.com"))
                .isEqualTo("smtp.example.com");
    }
}
