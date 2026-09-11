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

    @NotBlank(message = "Missing required field: username")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3 ile 50 karakter arasında olmalıdır")
    @JsonProperty("username")
    @JsonAlias("username")
    private String username;

    @NotBlank(message = "Missing required field: email")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @JsonProperty("email")
    @JsonAlias("email")
    private String email;

    @NotBlank(message = "Missing required field: password")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalıdır")
    @JsonProperty("password")
    @JsonAlias("password")
    private String password;

    @NotBlank(message = "Missing required field: first_name")
    @JsonProperty("first_name")
    @JsonAlias("firstName")
    private String firstName;

    @NotBlank(message = "Missing required field: last_name")
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
