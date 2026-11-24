package com.ozerler.marble.service;

import org.springframework.stereotype.Service;

import com.ozerler.marble.model.enums.TwoFactorMode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * System-wide 2FA mode and enforcement. Stored in {@code system_settings}; changes apply on the next request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorSettingsService {

    public static final String KEY_MODE = "security.2fa.mode";
    public static final String KEY_ENFORCED = "security.2fa.enforced";
    private static final String CATEGORY = "SECURITY";

    private final SettingService settingService;

    /**
     * Current mode. Missing rows mean {@link TwoFactorMode#DISABLED}.
     *
     * @return the mode
     */
    public TwoFactorMode mode() {
        return TwoFactorMode.fromValue(settingService.getSetting(KEY_MODE, TwoFactorMode.DISABLED.name()));
    }

    /**
     * Whether every user must enrol. Only meaningful when the mode is AUTHENTICATOR.
     *
     * @return true when enforced and the mode is on
     */
    public boolean enforced() {
        return mode() == TwoFactorMode.AUTHENTICATOR && settingService.getBooleanSetting(KEY_ENFORCED, false);
    }

    /**
     * Saves the mode and enforcement flag and writes an audit line.
     *
     * @param mode new mode
     * @param enforce new enforcement flag
     * @param actor e-mail of the admin
     * @throws TwoFactorException when enforcement is asked for while the mode is DISABLED
     */
    public void update(TwoFactorMode mode, boolean enforce, String actor) {
        if (mode == null) {
            throw new TwoFactorException("twofactor.settings.mode.invalid");
        }
        if (enforce && mode == TwoFactorMode.DISABLED) {
            throw new TwoFactorException("twofactor.settings.enforce.requires.mode");
        }
        TwoFactorMode oldMode = mode();
        boolean oldEnforced = settingService.getBooleanSetting(KEY_ENFORCED, false);
        settingService.saveSetting(KEY_MODE, mode.name(), CATEGORY,
                "İki faktörlü doğrulama modu: DISABLED veya AUTHENTICATOR");
        settingService.saveSetting(KEY_ENFORCED, Boolean.toString(enforce), CATEGORY,
                "Tüm kullanıcılar için Authenticator kurulumu zorunlu mu");
        log.info("SECURITY_AUDIT 2FA settings changed by {}: mode {} -> {}, enforced {} -> {}",
                actor, oldMode, mode, oldEnforced, enforce);
    }
}
