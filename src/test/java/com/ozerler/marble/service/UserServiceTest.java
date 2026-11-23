package com.ozerler.marble.service;


import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.UserCreateRequest;
import com.ozerler.marble.dto.UserDto;
import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.RoleRepository;
import com.ozerler.marble.repository.UserRepository;

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
    void createUserSuccess() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("new_operator")
                .email("operator@ozerler.com")
                .password("password123")
                .firstName("Mehmet")
                .lastName("Kaya")
                .enabled(true)
                .roles(Set.of("ROLE_ADMIN"))
                .build();

        Mockito.when(userRepository.existsByUsername("new_operator")).thenReturn(false);
        Mockito.when(userRepository.existsByEmail("operator@ozerler.com")).thenReturn(false);
        Mockito.when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPassword");

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

        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(savedUser);

        UserDto result = userService.createUser(request);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getId()).isEqualTo(10L);
        Assertions.assertThat(result.getUsername()).isEqualTo("new_operator");
        Assertions.assertThat(result.getEmail()).isEqualTo("operator@ozerler.com");
        Assertions.assertThat(result.getRoles()).contains("ROLE_ADMIN");

        Mockito.verify(passwordEncoder).encode("password123");
        Mockito.verify(userRepository).save(Mockito.any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void createUserDuplicateUsername() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("existing_user")
                .email("user@ozerler.com")
                .password("password123")
                .firstName("Ali")
                .lastName("Can")
                .build();

        Mockito.when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        Assertions.assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bu kullanıcı adı zaten kullanımda");

        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    @DisplayName("Should soft delete user and disable account")
    void softDeleteUserSuccess() {
        User user = User.builder()
                .id(5L)
                .username("test_user")
                .enabled(true)
                .deleted(false)
                .build();

        Mockito.when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        userService.softDeleteUser(5L);

        Assertions.assertThat(user.isDeleted()).isTrue();
        Assertions.assertThat(user.isEnabled()).isFalse();
        Mockito.verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Empty role selection lists all users")
    void getUsersPagedEmptyRolesDoesNotFilterByRole() {
        Mockito.when(
                        userRepository.searchActiveUsers(
                                Mockito.isNull(),
                                Mockito.isNull(),
                                Mockito.isNull(),
                                Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        TabulatorResponse<UserDto> response = userService.getUsersPaged(
                1, 10, null, null, null, List.of(), null);

        Assertions.assertThat(response.getData()).isEmpty();
        Mockito.verify(userRepository)
                .searchActiveUsers(
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.any(Pageable.class));
        Mockito.verify(userRepository, Mockito.never())
                .searchActiveUsersByAnyRole(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Blank role tokens are treated as no role filter")
    void getUsersPagedBlankRolesDoesNotFilterByRole() {
        Mockito.when(
                        userRepository.searchActiveUsers(
                                Mockito.isNull(),
                                Mockito.isNull(),
                                Mockito.isNull(),
                                Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        userService.getUsersPaged(1, 10, null, null, null, List.of("  ", ""), null);

        Mockito.verify(userRepository)
                .searchActiveUsers(
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.any(Pageable.class));
        Mockito.verify(userRepository, Mockito.never())
                .searchActiveUsersByAnyRole(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Selected roles filter users who have any of those roles")
    void getUsersPagedMultipleRolesMatchesAnySelectedRole() {
        User adminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .firstName("Sistem")
                .lastName("Yöneticisi")
                .roles(Set.of(roleAdmin))
                .build();
        Mockito.when(
                        userRepository.searchActiveUsersByAnyRole(
                                Mockito.isNull(),
                                Mockito.eq(List.of("ROLE_ADMIN", "ROLE_SALES")),
                                Mockito.isNull(),
                                Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(adminUser)));

        TabulatorResponse<UserDto> response = userService.getUsersPaged(
                1, 10, null, null, null, List.of("ROLE_ADMIN", "ROLE_SALES"), null);

        Assertions.assertThat(response.getData()).hasSize(1);
        Assertions.assertThat(response.getData().getFirst().getUsername()).isEqualTo("admin");
        Mockito.verify(userRepository)
                .searchActiveUsersByAnyRole(
                        Mockito.isNull(),
                        Mockito.eq(List.of("ROLE_ADMIN", "ROLE_SALES")),
                        Mockito.isNull(),
                        Mockito.any(Pageable.class));
        Mockito.verify(userRepository, Mockito.never())
                .searchActiveUsers(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Comma-separated role tokens are split before querying")
    void getUsersPagedCommaSeparatedRolesAreNormalized() {
        Mockito.when(
                        userRepository.searchActiveUsersByAnyRole(
                                Mockito.isNull(),
                                Mockito.eq(List.of("ROLE_ADMIN", "ROLE_USER")),
                                Mockito.eq(true),
                                Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        userService.getUsersPaged(1, 10, null, null, null, List.of("ROLE_ADMIN, ROLE_USER"), true);

        Mockito.verify(userRepository)
                .searchActiveUsersByAnyRole(
                        Mockito.isNull(),
                        Mockito.eq(List.of("ROLE_ADMIN", "ROLE_USER")),
                        Mockito.eq(true),
                        Mockito.any(Pageable.class));
    }
}
