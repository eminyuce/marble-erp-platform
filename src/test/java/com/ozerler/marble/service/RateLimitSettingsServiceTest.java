package com.ozerler.marble.service;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.config.RateLimitProperties;
import com.ozerler.marble.dto.RateLimitSettings;

@ExtendWith(MockitoExtension.class)
class RateLimitSettingsServiceTest {

    @Mock
    private SettingService settingService;

    private final Map<String, String> store = new HashMap<>();

    private RateLimitSettingsService service;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getLogin().setMaxRequests(10);
        properties.getLogin().setWindowSeconds(60);
        properties.getForm().setMaxRequests(30);
        properties.getForm().setWindowSeconds(60);
        properties.getGeneral().setMaxRequests(120);
        properties.getGeneral().setWindowSeconds(60);

        Mockito.when(settingService.getSettingValue(Mockito.anyString()))
                .thenAnswer(invocation -> store.get(invocation.getArgument(0)));
        Mockito.lenient().doAnswer(invocation -> {
            store.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(settingService).saveSetting(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any());
        Mockito.lenient().doAnswer(invocation -> {
            store.remove((String) invocation.getArgument(0));
            return null;
        }).when(settingService).deleteSetting(Mockito.anyString());

        service = new RateLimitSettingsService(settingService, properties);
    }

    @Test
    @DisplayName("YAML defaults are used when no database override is stored")
    void defaultsApplyWhenNoOverride() {
        RateLimitSettings defaults = service.yamlDefaults();
        RateLimitSettings effective = service.effective();

        Assertions.assertThat(defaults.isEnabled()).isTrue();
        Assertions.assertThat(defaults.getLoginMaxRequests()).isEqualTo(10);
        Assertions.assertThat(defaults.getFormMaxRequests()).isEqualTo(30);
        Assertions.assertThat(defaults.getGeneralMaxRequests()).isEqualTo(120);
        Assertions.assertThat(effective).isEqualTo(defaults);
        Assertions.assertThat(service.hasOverride()).isFalse();
    }

    @Test
    @DisplayName("Database overrides replace YAML defaults and are cached until evicted")
    void overridesReplaceDefaultsAndCacheEvictsOnSave() {
        store.put(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_MAX, "3");
        RateLimitSettings first = service.effective();
        Assertions.assertThat(first.getLoginMaxRequests()).isEqualTo(3);

        store.put(Constants.SETTING_KEY_RATE_LIMIT_LOGIN_MAX, "8");
        Assertions.assertThat(service.effective().getLoginMaxRequests())
                .as("cached snapshot should not reload until evicted")
                .isEqualTo(3);

        service.evict();
        Assertions.assertThat(service.effective().getLoginMaxRequests()).isEqualTo(8);
        Assertions.assertThat(service.hasOverride()).isTrue();
    }

    @Test
    @DisplayName("save persists validated values, evicts the cache and keeps enabled=false")
    void savePersistsAndAppliesLive() {
        service.save(false, "4", "90", "15", "45", "80", "30");
        RateLimitSettings effective = service.effective();

        Assertions.assertThat(effective.isEnabled()).isFalse();
        Assertions.assertThat(effective.getLoginMaxRequests()).isEqualTo(4);
        Assertions.assertThat(effective.getLoginWindowSeconds()).isEqualTo(90);
        Assertions.assertThat(effective.getFormMaxRequests()).isEqualTo(15);
        Assertions.assertThat(effective.getFormWindowSeconds()).isEqualTo(45);
        Assertions.assertThat(effective.getGeneralMaxRequests()).isEqualTo(80);
        Assertions.assertThat(effective.getGeneralWindowSeconds()).isEqualTo(30);
        Assertions.assertThat(store.get(Constants.SETTING_KEY_RATE_LIMITING)).isEqualTo("false");
    }

    @Test
    @DisplayName("resetToDefaults deletes overrides so YAML values apply again")
    void resetRestoresYamlDefaults() {
        service.save(false, "4", "90", "15", "45", "80", "30");
        service.resetToDefaults();

        Assertions.assertThat(store).isEmpty();
        Assertions.assertThat(service.hasOverride()).isFalse();
        Assertions.assertThat(service.effective()).isEqualTo(service.yamlDefaults());
        Assertions.assertThat(service.effective().isEnabled()).isTrue();
        Assertions.assertThat(service.effective().getLoginMaxRequests()).isEqualTo(10);
    }

    @Test
    @DisplayName("validation rejects non-positive, non-numeric and out-of-range values")
    void validationRejectsInvalidValues() {
        Assertions.assertThatThrownBy(() -> service.save(true, "0", "60", "30", "60", "120", "60"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 ile 10000");
        Assertions.assertThatThrownBy(() -> service.save(true, "-1", "60", "30", "60", "120", "60"))
                .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> service.save(true, "10001", "60", "30", "60", "120", "60"))
                .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> service.save(true, "10", "3601", "30", "60", "120", "60"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 ile 3600");
        Assertions.assertThatThrownBy(() -> service.save(true, "abc", "60", "30", "60", "120", "60"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tam sayı");
        Assertions.assertThatThrownBy(() -> service.save(true, " ", "60", "30", "60", "120", "60"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş");
        Assertions.assertThat(service.effective()).isEqualTo(service.yamlDefaults());
    }
}
