package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Named application roles and their localized display labels.
 */
public enum UserRole {
    ADMIN("ROLE_ADMIN", "enum.user_role.admin"),
    EXECUTIVE("ROLE_EXECUTIVE", "enum.user_role.executive"),
    FACTORY_MANAGER("ROLE_FACTORY_MANAGER", "enum.user_role.factory_manager"),
    QUARRY_CHIEF("ROLE_QUARRY_CHIEF", "enum.user_role.quarry_chief"),
    SITE_ENGINEER("ROLE_SITE_ENGINEER", "enum.user_role.site_engineer"),
    WORKSHOP_CHIEF("ROLE_WORKSHOP_CHIEF", "enum.user_role.workshop_chief"),
    OPERATOR("ROLE_OPERATOR", "enum.user_role.operator"),
    FINANCE("ROLE_FINANCE", "enum.user_role.finance"),
    SALES("ROLE_SALES", "enum.user_role.sales"),
    QC("ROLE_QC", "enum.user_role.qc"),
    USER("ROLE_USER", "enum.user_role.user");

    private final String authority;
    private final String messageKey;

    UserRole(String authority, String messageKey) {
        this.authority = authority;
        this.messageKey = messageKey;
    }

    public String getAuthority() {
        return authority;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }

    public static String labelFor(String roleName) {
        UserRole role = fromAuthority(roleName);
        if (role != null) {
            return role.getLabel();
        }
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        return roleName.startsWith("ROLE_") ? roleName.substring("ROLE_".length()) : roleName;
    }

    public static Map<String, String> labelMap() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (UserRole role : values()) {
            labels.put(role.getAuthority(), role.getLabel());
        }
        return Map.copyOf(labels);
    }

    private static UserRole fromAuthority(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        for (UserRole role : values()) {
            if (role.authority.equalsIgnoreCase(roleName) || role.name().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        return null;
    }
}
