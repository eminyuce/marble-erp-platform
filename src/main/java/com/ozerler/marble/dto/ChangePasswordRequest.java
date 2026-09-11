package com.ozerler.marble.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for self-service password change by authenticated users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Mevcut şifre boş bırakılamaz")
    private String currentPassword;

    @NotBlank(message = "Yeni şifre boş bırakılamaz")
    @Size(min = 6, message = "Şifre en az 6 karakterden oluşmalıdır")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Şifre en az bir büyük harf, bir küçük harf ve bir sayı içermelidir"
    )
    private String newPassword;

    @NotBlank(message = "Şifre onayı boş bırakılamaz")
    private String confirmPassword;
}
