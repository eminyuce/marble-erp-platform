package com.ozerler.marble.service;

import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditPresenterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditPresenter auditPresenter;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("xyz@email.com")
                .firstName("Emin")
                .lastName("YUCE")
                .build();
    }

    @Test
    @DisplayName("formatUser formats known user as email (FullName)")
    void formatUser_knownUser_returnsEmailWithFullName() {
        when(userRepository.findByEmail("xyz@email.com")).thenReturn(Optional.of(testUser));

        String formatted = auditPresenter.formatUser("xyz@email.com");
        assertThat(formatted).isEqualTo("xyz@email.com (Emin YUCE)");
    }

    @Test
    @DisplayName("formatUser formats system email with (Sistem)")
    void formatUser_systemEmail_returnsWithSistemTag() {
        when(userRepository.findByEmail("system@ozerler.com")).thenReturn(Optional.empty());

        String formatted = auditPresenter.formatUser("system@ozerler.com");
        assertThat(formatted).isEqualTo("system@ozerler.com (Sistem)");
    }

    @Test
    @DisplayName("formatUser returns raw email when user not found")
    void formatUser_unknownEmail_returnsRawEmail() {
        when(userRepository.findByEmail("unknown@email.com")).thenReturn(Optional.empty());

        String formatted = auditPresenter.formatUser("unknown@email.com");
        assertThat(formatted).isEqualTo("unknown@email.com");
    }

    @Test
    @DisplayName("formatUser returns dash when email is null or blank")
    void formatUser_nullOrBlank_returnsDash() {
        assertThat(auditPresenter.formatUser(null)).isEqualTo("-");
        assertThat(auditPresenter.formatUser("")).isEqualTo("-");
        assertThat(auditPresenter.formatUser("   ")).isEqualTo("-");
    }

    @Test
    @DisplayName("formatDateTime formats according to Turkish d.MM.yyyy HH:mm format")
    void formatDateTime_formatsCorrectly() {
        LocalDateTime dt1 = LocalDateTime.of(2026, 7, 22, 20, 11);
        LocalDateTime dt2 = LocalDateTime.of(2026, 9, 3, 12, 57);

        assertThat(auditPresenter.formatDateTime(dt1)).isEqualTo("22.07.2026 20:11");
        assertThat(auditPresenter.formatDateTime(dt2)).isEqualTo("3.09.2026 12:57");
    }

    @Test
    @DisplayName("formatDateTime returns dash when dateTime is null")
    void formatDateTime_null_returnsDash() {
        assertThat(auditPresenter.formatDateTime(null)).isEqualTo("-");
    }
}
