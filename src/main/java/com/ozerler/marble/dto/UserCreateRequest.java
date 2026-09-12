package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Request DTO for creating a new user account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "{validation.user.username.required}")
    @Size(min = 3, max = 50, message = "{validation.user.username.size}")
    @JsonProperty("username")
    @JsonAlias("username")
    private String username;

    @NotBlank(message = "{validation.user.email.required}")
    @Email(message = "{validation.user.email.invalid}")
    @JsonProperty("email")
    @JsonAlias("email")
    private String email;

    @NotBlank(message = "{validation.user.password.required}")
    @Size(min = 6, message = "{validation.user.password.size}")
    @JsonProperty("password")
    @JsonAlias("password")
    private String password;

    @NotBlank(message = "{validation.user.firstname.required}")
    @JsonProperty("first_name")
    @JsonAlias("firstName")
    private String firstName;

    @NotBlank(message = "{validation.user.lastname.required}")
    @JsonProperty("last_name")
    @JsonAlias("lastName")
    private String lastName;

    @Builder.Default
    @JsonProperty("enabled")
    @JsonAlias("enabled")
    private boolean enabled = true;

    @Builder.Default
    @JsonProperty("roles")
    @JsonAlias("roles")
    private Set<String> roles = new HashSet<>();
}
