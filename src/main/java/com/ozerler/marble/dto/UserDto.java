package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozerler.marble.model.Role;
import com.ozerler.marble.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data Transfer Object representing system user details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @JsonProperty("username")
    @JsonAlias("username")
    private String username;

    @JsonProperty("email")
    @JsonAlias("email")
    private String email;

    @JsonProperty("first_name")
    @JsonAlias("firstName")
    private String firstName;

    @JsonProperty("last_name")
    @JsonAlias("lastName")
    private String lastName;

    @JsonProperty("full_name")
    @JsonAlias("fullName")
    private String fullName;

    @JsonProperty("enabled")
    @JsonAlias("enabled")
    private boolean enabled;

    @JsonProperty("roles")
    @JsonAlias("roles")
    private Set<String> roles;

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonAlias("updatedAt")
    private LocalDateTime updatedAt;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
