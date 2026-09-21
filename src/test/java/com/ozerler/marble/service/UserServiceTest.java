package com.ozerler.marble.service;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.UserCreateRequest;
import com.ozerler.marble.dto.UserDto;
import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.RoleRepository;
import com.ozerler.marble.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        roleAdmin = Role.builder().id(1L).name("ROLE_ADMIN").description("Admin").build();
    }

    @Test
    @DisplayName("Should successfully create a user with hashed password and assigned roles")
    void createUser_success() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("new_operator")
                .email("operator@ozerler.com")
                .password("password123")
                .firstName("Mehmet")
                .lastName("Kaya")
                .enabled(true)
                .roles(Set.of("ROLE_ADMIN"))
                .build();

        when(userRepository.existsByUsername("new_operator")).thenReturn(false);
        when(userRepository.existsByEmail("operator@ozerler.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");

        User savedUser = User.builder()
                .id(10L)
                .username("new_operator")
                .email("operator@ozerler.com")
                .password("$2a$10$hashedPassword")
                .firstName("Mehmet")
                .lastName("Kaya")
                .enabled(true)
                .deleted(false)
                .roles(Set.of(roleAdmin))
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.createUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUsername()).isEqualTo("new_operator");
        assertThat(result.getEmail()).isEqualTo("operator@ozerler.com");
        assertThat(result.getRoles()).contains("ROLE_ADMIN");

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void createUser_duplicateUsername() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("existing_user")
                .email("user@ozerler.com")
                .password("password123")
                .firstName("Ali")
                .lastName("Can")
                .build();

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bu kullanıcı adı zaten kullanımda");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should soft delete user and disable account")
    void softDeleteUser_success() {
        User user = User.builder()
                .id(5L)
                .username("test_user")
                .enabled(true)
                .deleted(false)
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        userService.softDeleteUser(5L);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.isEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Empty role selection lists all users")
    void getUsersPaged_emptyRoles_doesNotFilterByRole() {
        when(userRepository.searchActiveUsers(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        TabulatorResponse<UserDto> response = userService.getUsersPaged(
                1, 10, null, null, null, List.of(), null);

        assertThat(response.getData()).isEmpty();
        verify(userRepository).searchActiveUsers(isNull(), isNull(), isNull(), any(Pageable.class));
        verify(userRepository, never()).searchActiveUsersByAnyRole(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Blank role tokens are treated as no role filter")
    void getUsersPaged_blankRoles_doesNotFilterByRole() {
        when(userRepository.searchActiveUsers(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        userService.getUsersPaged(1, 10, null, null, null, List.of("  ", ""), null);

        verify(userRepository).searchActiveUsers(isNull(), isNull(), isNull(), any(Pageable.class));
        verify(userRepository, never()).searchActiveUsersByAnyRole(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Selected roles filter users who have any of those roles")
    void getUsersPaged_multipleRoles_matchesAnySelectedRole() {
        User adminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .firstName("Sistem")
                .lastName("Yöneticisi")
                .roles(Set.of(roleAdmin))
                .build();
        when(userRepository.searchActiveUsersByAnyRole(
                isNull(), eq(List.of("ROLE_ADMIN", "ROLE_SALES")), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(adminUser)));

        TabulatorResponse<UserDto> response = userService.getUsersPaged(
                1, 10, null, null, null, List.of("ROLE_ADMIN", "ROLE_SALES"), null);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().getUsername()).isEqualTo("admin");
        verify(userRepository).searchActiveUsersByAnyRole(
                isNull(), eq(List.of("ROLE_ADMIN", "ROLE_SALES")), isNull(), any(Pageable.class));
        verify(userRepository, never()).searchActiveUsers(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Comma-separated role tokens are split before querying")
    void getUsersPaged_commaSeparatedRoles_areNormalized() {
        when(userRepository.searchActiveUsersByAnyRole(
                isNull(), eq(List.of("ROLE_ADMIN", "ROLE_USER")), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        userService.getUsersPaged(1, 10, null, null, null, List.of("ROLE_ADMIN, ROLE_USER"), true);

        verify(userRepository).searchActiveUsersByAnyRole(
                isNull(), eq(List.of("ROLE_ADMIN", "ROLE_USER")), eq(true), any(Pageable.class));
    }
}
