package com.ozerler.marble.dto;

import com.ozerler.marble.model.enums.TwoFactorMode;

/**
 * A user's own 2FA status for the account page.
 *
 * @param mode system mode
 * @param enforced whether the admin made 2FA mandatory
 * @param enabled whether this user has an active authenticator
 * @param remainingBackupCodes unused backup codes
 */
public record TwoFactorStatusView(TwoFactorMode mode, boolean enforced, boolean enabled, long remainingBackupCodes) {

    public boolean isAvailable() {
        return mode == TwoFactorMode.AUTHENTICATOR;
    }
}
