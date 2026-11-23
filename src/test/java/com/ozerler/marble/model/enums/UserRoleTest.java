package com.ozerler.marble.model.enums;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    @DisplayName("labelFor returns Turkish labels for known role authorities")
    void labelFor_knownRoles_returnsTurkishLabels() {
        Assertions.assertThat(UserRole.labelFor("ROLE_ADMIN")).isEqualTo("Sistem Yöneticisi");
        Assertions.assertThat(UserRole.labelFor("ROLE_EXECUTIVE")).isEqualTo("Üst Yönetim");
        Assertions.assertThat(UserRole.labelFor("ROLE_FACTORY_MANAGER")).isEqualTo("Fabrika Müdürü");
        Assertions.assertThat(UserRole.labelFor("ROLE_QUARRY_CHIEF")).isEqualTo("Ocak Şefi");
        Assertions.assertThat(UserRole.labelFor("ROLE_SITE_ENGINEER")).isEqualTo("Şantiye Mühendisi");
        Assertions.assertThat(UserRole.labelFor("ROLE_WORKSHOP_CHIEF")).isEqualTo("Atölye Şefi");
        Assertions.assertThat(UserRole.labelFor("ROLE_OPERATOR")).isEqualTo("Operatör");
        Assertions.assertThat(UserRole.labelFor("ROLE_FINANCE")).isEqualTo("Muhasebe / Finans");
        Assertions.assertThat(UserRole.labelFor("ROLE_SALES")).isEqualTo("Satış Temsilcisi");
        Assertions.assertThat(UserRole.labelFor("ROLE_QC")).isEqualTo("Kalite Kontrol Uzmanı");
        Assertions.assertThat(UserRole.labelFor("ROLE_USER")).isEqualTo("Standart Kullanıcı");
    }

    @Test
    @DisplayName("labelFor accepts enum names and falls back for unknown roles")
    void labelFor_enumNameAndUnknown() {
        Assertions.assertThat(UserRole.labelFor("ADMIN")).isEqualTo("Sistem Yöneticisi");
        Assertions.assertThat(UserRole.labelFor("ROLE_CUSTOM")).isEqualTo("CUSTOM");
        Assertions.assertThat(UserRole.labelFor("")).isEmpty();
        Assertions.assertThat(UserRole.labelFor(null)).isEmpty();
    }

    @Test
    @DisplayName("labelMap includes every application role")
    void labelMap_containsAllRoles() {
        Map<String, String> labels = UserRole.labelMap();

        Assertions.assertThat(labels).hasSize(UserRole.values().length);
        Assertions.assertThat(labels).containsEntry("ROLE_ADMIN", "Sistem Yöneticisi");
        Assertions.assertThat(labels).containsEntry("ROLE_USER", "Standart Kullanıcı");
    }
}
