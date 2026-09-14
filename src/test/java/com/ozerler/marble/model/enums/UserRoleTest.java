package com.ozerler.marble.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleTest {

    @Test
    @DisplayName("labelFor returns Turkish labels for known role authorities")
    void labelFor_knownRoles_returnsTurkishLabels() {
        assertThat(UserRole.labelFor("ROLE_ADMIN")).isEqualTo("Sistem Yöneticisi");
        assertThat(UserRole.labelFor("ROLE_EXECUTIVE")).isEqualTo("Üst Yönetim");
        assertThat(UserRole.labelFor("ROLE_FACTORY_MANAGER")).isEqualTo("Fabrika Müdürü");
        assertThat(UserRole.labelFor("ROLE_QUARRY_CHIEF")).isEqualTo("Ocak Şefi");
        assertThat(UserRole.labelFor("ROLE_SITE_ENGINEER")).isEqualTo("Şantiye Mühendisi");
        assertThat(UserRole.labelFor("ROLE_WORKSHOP_CHIEF")).isEqualTo("Atölye Şefi");
        assertThat(UserRole.labelFor("ROLE_OPERATOR")).isEqualTo("Operatör");
        assertThat(UserRole.labelFor("ROLE_FINANCE")).isEqualTo("Muhasebe / Finans");
        assertThat(UserRole.labelFor("ROLE_SALES")).isEqualTo("Satış Temsilcisi");
        assertThat(UserRole.labelFor("ROLE_QC")).isEqualTo("Kalite Kontrol Uzmanı");
        assertThat(UserRole.labelFor("ROLE_USER")).isEqualTo("Standart Kullanıcı");
    }

    @Test
    @DisplayName("labelFor accepts enum names and falls back for unknown roles")
    void labelFor_enumNameAndUnknown() {
        assertThat(UserRole.labelFor("ADMIN")).isEqualTo("Sistem Yöneticisi");
        assertThat(UserRole.labelFor("ROLE_CUSTOM")).isEqualTo("CUSTOM");
        assertThat(UserRole.labelFor("")).isEmpty();
        assertThat(UserRole.labelFor(null)).isEmpty();
    }

    @Test
    @DisplayName("labelMap includes every application role")
    void labelMap_containsAllRoles() {
        Map<String, String> labels = UserRole.labelMap();

        assertThat(labels).hasSize(UserRole.values().length);
        assertThat(labels).containsEntry("ROLE_ADMIN", "Sistem Yöneticisi");
        assertThat(labels).containsEntry("ROLE_USER", "Standart Kullanıcı");
    }
}
