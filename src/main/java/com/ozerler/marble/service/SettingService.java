package com.ozerler.marble.service;

import com.ozerler.marble.model.SystemSetting;
import com.ozerler.marble.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingService {

    private final SystemSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public Map<String, String> getAllSettingsAsMap() {
        List<SystemSetting> all = settingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SystemSetting s : all) {
            map.put(s.getKey(), s.getValue());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<SystemSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String getSetting(String key, String defaultValue) {
        return settingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String val = getSetting(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    @Transactional
    public void saveSetting(String key, String value, String category, String description) {
        Optional<SystemSetting> existing = settingRepository.findByKey(key);
        SystemSetting setting = existing.orElseGet(() -> SystemSetting.builder()
                .key(key)
                .category(category != null ? category : "GENERAL")
                .description(description)
                .build());

        setting.setValue(value);
        if (category != null) {
            setting.setCategory(category);
        }
        if (description != null) {
            setting.setDescription(description);
        }
        settingRepository.save(setting);
        log.info("System setting saved: {} = {}", key, key.toLowerCase().contains("password") ? "******" : value);
    }

    @Transactional
    public void updateSettings(Map<String, String> settings) {
        settings.forEach((k, v) -> {
            Optional<SystemSetting> opt = settingRepository.findByKey(k);
            if (opt.isPresent()) {
                SystemSetting s = opt.get();
                s.setValue(v);
                settingRepository.save(s);
            } else {
                saveSetting(k, v, "GENERAL", null);
            }
        });
    }
}
