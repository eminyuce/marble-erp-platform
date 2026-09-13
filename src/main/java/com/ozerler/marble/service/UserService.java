package com.ozerler.marble.service;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.*;
import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import com.ozerler.marble.repository.RoleRepository;
import com.ozerler.marble.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service managing user lifecycle, authentication credentials, and role assignments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    @Transactional(readOnly = true)
    public TabulatorResponse<UserDto> getUsersPaged(int page, int size, String search, String sortField, String sortDir,
                                                    String roleName, Boolean enabled) {
        String sortProperty = (sortField == null || sortField.isBlank() || "createdAt".equalsIgnoreCase(sortField))
                ? "createdDate" : sortField;
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(dir, sortProperty);

        // Tabulator pages are 1-indexed, Spring Data is 0-indexed
        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size > 0 ? size : 10, sort);

        String normalizedSearch = StringUtils.isBlank(search) ? null : search.trim();
        String normalizedRole = StringUtils.isBlank(roleName) ? null : roleName.trim();
        Page<User> userPage = userRepository.searchActiveUsers(normalizedSearch, normalizedRole, enabled, pageable);
        List<UserDto> dtos = userPage.getContent().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());

        return TabulatorResponse.of(dtos, userPage.getTotalPages(), userPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.user.not_found", id)));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(getMessage("error.user.username.exists", request.getUsername()));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(getMessage("error.user.email.exists", request.getEmail()));
        }

        Set<Role> roles = new HashSet<>();
        if (CollectionUtils.isNotEmpty(request.getRoles())) {
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
        }
        if (roles.isEmpty()) {
            roleRepository.findByName(Constants.ROLE_USER).ifPresent(roles::add);
        }

        User user = User.builder()
                .username(request.getUsername().trim().toLowerCase())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .enabled(request.isEnabled())
                .deleted(false)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);

        try {
            Map<String, String> welcomeVars = Map.of(
                    "fullName", saved.getFirstName() + " " + saved.getLastName(),
                    "email", saved.getEmail(),
                    "username", saved.getUsername(),
                    "loginUrl", "http://localhost:81/account/adminlogin/",
                    "companyName", getMessage("common.company_name")
            );
            emailService.sendTemplatedEmail(saved.getEmail(), "USER_WELCOME", welcomeVars);
        } catch (Exception ex) {
            log.warn("Could not dispatch welcome email to {}: {}", saved.getEmail(), ex.getMessage());
        }

        return UserDto.fromEntity(saved);
    }

    @Transactional
    public UserDto updateUser(UserUpdateRequest request) {
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.user.not_found", request.getId())));

        userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException(getMessage("error.user.email.belongs_to_other", request.getEmail()));
            }
        });

        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEnabled(request.isEnabled());

        if (CollectionUtils.isNotEmpty(request.getRoles())) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        User updated = userRepository.save(user);
        return UserDto.fromEntity(updated);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.user.not_found", id)));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.user.not_found", request.getUserId())));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        try {
            Map<String, String> resetVars = Map.of(
                    "fullName", user.getFirstName() + " " + user.getLastName(),
                    "temporaryPassword", request.getNewPassword(),
                    "email", user.getEmail(),
                    "companyName", getMessage("common.company_name")
            );
            emailService.sendTemplatedEmail(user.getEmail(), "PASSWORD_RESET", resetVars);
        } catch (Exception ex) {
            log.warn("Could not dispatch password reset email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    @Transactional
    public void softDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.user.not_found", id)));
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public void changeOwnPassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(getMessage("error.user.not_found", email)));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException(getMessage("error.user.password.incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
