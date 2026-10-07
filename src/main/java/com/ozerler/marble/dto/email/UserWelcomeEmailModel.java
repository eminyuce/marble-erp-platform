package com.ozerler.marble.dto.email;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model for 'USER_WELCOME' email template.
 * Dispatched to new employees/users when account credentials are generated.
 */
@Getter
@Builder
@ToString
public class UserWelcomeEmailModel implements BaseEmailModel {

    public static final String TEMPLATE_KEY = "USER_WELCOME";

    private final String fullName;
    private final String username;
    private final String email;
    private final String roleName;
    private final String temporaryPassword;
    private final String loginUrl;
    private final String companyName;
    private final String supportContact;

    @Override
    public String getTemplateKey() {
        return TEMPLATE_KEY;
    }

    @Override
    public Map<String, String> toVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("fullName", fullName != null ? fullName : "");
        vars.put("username", username != null ? username : "");
        vars.put("email", email != null ? email : "");
        vars.put("roleName", roleName != null ? roleName : "Kullanıcı");
        vars.put("temporaryPassword", temporaryPassword != null ? temporaryPassword : "");
        vars.put("loginUrl", loginUrl != null ? loginUrl : "http://localhost:81/account/adminlogin/");
        vars.put("companyName", companyName != null ? companyName : "Özerler Mermer A.Ş.");
        vars.put("supportContact", supportContact != null ? supportContact : "it-destek@ozerlermermer.com");
        return vars;
    }

    public static UserWelcomeEmailModel sample() {
        return UserWelcomeEmailModel.builder()
                .fullName("Ahmet Yılmaz")
                .username("ahmet.yilmaz")
                .email("ahmet.yilmaz@ozerler.test")
                .roleName("Fabrika Müdürü (ROLE_EXECUTIVE)")
                .temporaryPassword("Ozerler*2026!Pass")
                .loginUrl("http://localhost:81/account/adminlogin/")
                .companyName("Özerler Mermer A.Ş.")
                .supportContact("+90 272 214 00 00 / Dahili: 104")
                .build();
    }
}
