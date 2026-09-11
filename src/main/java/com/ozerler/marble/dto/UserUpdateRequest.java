package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for updating an existing user account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @JsonProperty("id")
    @JsonAlias("id")
    private Long id;

    @NotBlank(message = "Missing required field: email")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @JsonProperty("email")
    @JsonAlias("email")
    private String email;

    @NotBlank(message = "Missing required field: first_name")
    @JsonProperty("first_name")
    @JsonAlias("firstName")
    private String firstName;

    @NotBlank(message = "Missing required field: last_name")
    @JsonProperty("last_name")
    @JsonAlias("lastName")
    private String lastName;

    @JsonProperty("enabled")
    @JsonAlias("enabled")
    private boolean enabled;

    @JsonProperty("roles")
    @JsonAlias("roles")
    private Set<String> roles;
}
