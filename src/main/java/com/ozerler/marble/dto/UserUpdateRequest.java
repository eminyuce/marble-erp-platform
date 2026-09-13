package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    @NotBlank(message = "{validation.user.email.required}")
    @Email(message = "{validation.user.email.invalid}")
    @JsonProperty("email")
    @JsonAlias("email")
    private String email;

    @NotBlank(message = "{validation.user.firstname.required}")
    @JsonProperty("first_name")
    @JsonAlias("firstName")
    private String firstName;

    @NotBlank(message = "{validation.user.lastname.required}")
    @JsonProperty("last_name")
    @JsonAlias("lastName")
    private String lastName;

    @JsonProperty("enabled")
    @JsonAlias("enabled")
    private boolean enabled;

    @Builder.Default
    @JsonProperty("roles")
    @JsonAlias("roles")
    private Set<String> roles = new HashSet<>();

    @JsonProperty("created_date")
    @JsonAlias("createdDate")
    private LocalDateTime createdDate;

    @JsonProperty("updated_date")
    @JsonAlias("updatedDate")
    private LocalDateTime updatedDate;

    @JsonProperty("add_user_id")
    @JsonAlias("addUserId")
    private String addUserId;

    @JsonProperty("update_user_id")
    @JsonAlias("updateUserId")
    private String updateUserId;
}
