package com.ozerler.marble.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private Role roleUser;

    @BeforeEach
    void setUp() {
        roleAdmin = Role.builder().id(1L).name("ROLE_ADMIN").description("Admin").build();
        roleUser = Role.builder().id(2L).name("ROLE_USER").description("User").build();
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
}
