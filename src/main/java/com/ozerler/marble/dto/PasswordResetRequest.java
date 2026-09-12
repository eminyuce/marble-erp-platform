package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for administrative password reset operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @JsonProperty("user_id")
    @JsonAlias("userId")
    private Long userId;

    @NotBlank(message = "{validation.user.new_password.required}")
    @Size(min = 6, message = "{validation.user.password.size}")
    @JsonProperty("new_password")
    @JsonAlias("newPassword")
    private String newPassword;
}
