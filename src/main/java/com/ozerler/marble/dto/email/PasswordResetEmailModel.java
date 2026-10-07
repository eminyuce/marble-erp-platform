package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

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
        vars.put("fullName", fullName != null ? fullName : "");
        vars.put("resetCode", resetCode != null ? resetCode : "");
        vars.put("expirationMinutes", expirationMinutes != null ? expirationMinutes : "15");
        vars.put("resetUrl", resetUrl != null ? resetUrl : "#");
        vars.put("requestIp", requestIp != null ? requestIp : "127.0.0.1");
        vars.put("requestTime", requestTime != null ? requestTime : "");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
        vars.put("supportEmail", supportEmail != null ? supportEmail : "guvenlik@ozerlermermer.com");
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
