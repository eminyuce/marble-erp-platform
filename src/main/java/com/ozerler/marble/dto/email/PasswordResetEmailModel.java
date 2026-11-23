package com.ozerler.marble.dto.email;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Model for 'PASSWORD_RESET' email template.
 * Dispatched with one-time verification security PIN or reset link.
 */
@Getter
@Builder
@ToString
public class PasswordResetEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "PASSWORD_RESET";

    private final String fullName;
    private final String resetCode;
    private final String expirationMinutes;
    private final String resetUrl;
    private final String requestIp;
    private final String requestTime;
    private final String companyName;
    private final String supportEmail;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("fullName", Objects.requireNonNullElse(fullName, ""));
        vars.put("resetCode", Objects.requireNonNullElse(resetCode, ""));
        vars.put("expirationMinutes", Objects.requireNonNullElse(expirationMinutes, "15"));
        vars.put("resetUrl", Objects.requireNonNullElse(resetUrl, "#"));
        vars.put("requestIp", Objects.requireNonNullElse(requestIp, "127.0.0.1"));
        vars.put("requestTime", Objects.requireNonNullElse(requestTime, ""));
        vars.put("companyName", Objects.requireNonNullElse(companyName, "Özerler Mermer A.Ş."));
        vars.put("supportEmail", Objects.requireNonNullElse(supportEmail, "guvenlik@ozerlermermer.com"));
        return vars;
    }

    public static PasswordResetEmailModel sample() {
        return PasswordResetEmailModel.builder()
                .fullName("Emin Yüce")
                .resetCode("942851")
                .expirationMinutes("15")
                .resetUrl("http://localhost:81/account/password-reset?token=942851")
                .requestIp("192.168.1.105 (Afyon Merkez Kampüs)")
                .requestTime("07.10.2026 20:15")
                .companyName("Özerler Mermer A.Ş.")
                .supportEmail("bilgi-guvenligi@ozerlermermer.com")
                .build();
    }
}
